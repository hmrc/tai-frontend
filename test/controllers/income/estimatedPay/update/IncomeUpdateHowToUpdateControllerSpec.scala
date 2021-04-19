/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.income.estimatedPay.update

import builders.RequestBuilder
import controllers.FakeAuthAction
import controllers.actions.FakeValidatePerson
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.{Employment, _}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants._
import utils.BaseSpec
import views.html.incomes.howToUpdate

import scala.concurrent.Future

class IncomeUpdateHowToUpdateControllerSpec extends BaseSpec with JourneyCacheConstants with ScalaFutures {

  val employer = IncomeSource(id = 1, name = "sample employer")
  val defaultEmployment =
    Employment("company", Live, Some("123"), new LocalDate("2016-05-26"), None, Nil, "", "", 1, None, false, false)

  val incomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]

  class TestIncomeUpdateHowToUpdateController
      extends IncomeUpdateHowToUpdateController(
        FakeAuthAction,
        FakeValidatePerson,
        employmentService,
        incomeService,
        taxAccountService,
        mcc,
        inject[howToUpdate],
        journeyCacheService,
        error_template_noauth,
        error_no_primary,
        MockPartialRetriever,
        MockTemplateRenderer
      ) {
    when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
      .thenReturn(Future.successful(Right(employer.id)))
    when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncome_NameKey))(any()))
      .thenReturn(Future.successful(Right(employer.name)))
  }

  def BuildEmploymentAmount(isLive: Boolean = false, isOccupationPension: Boolean = true) =
    EmploymentAmount(
      name = "name",
      description = "description",
      employmentId = employer.id,
      newAmount = 200,
      oldAmount = 200,
      isLive = isLive,
      isOccupationalPension = isOccupationPension)

  def BuildTaxCodeIncomes(incomeCount: Int) = {

    val taxCodeIncome1 =
      TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)
    val taxCodeIncome2 =
      TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employer", "S1150L", "employer", OtherBasisOfOperation, Live)

    incomeCount match {
      case 2 => Seq(taxCodeIncome1, taxCodeIncome2)
      case 1 => Seq(taxCodeIncome1)
      case 0 => Nil
    }
  }

  "howToUpdatePage" must {
    object HowToUpdatePageHarness {

      sealed class HowToUpdatePageHarness(cacheMap: Map[String, String], employment: Option[Employment]) {

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome])))

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(employment))

        when(incomeService.employmentAmount(any(), any())(any(), any()))
          .thenReturn(Future.successful(BuildEmploymentAmount()))

        Mockito.reset(journeyCacheService)

        when(journeyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(cacheMap))

        def howToUpdatePage(): Future[Result] =
          new TestIncomeUpdateHowToUpdateController()
            .howToUpdatePage(1)(RequestBuilder.buildFakeGetRequestWithAuth)
      }

      def setup(
        cacheMap: Map[String, String],
        employment: Option[Employment] = Some(defaultEmployment)): HowToUpdatePageHarness =
        new HowToUpdatePageHarness(cacheMap, employment)
    }

    "render the right response to the user" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypePension)

      val result = HowToUpdatePageHarness
        .setup(cacheMap)
        .howToUpdatePage()

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
    }

    "cache the employer details" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypeEmployment)

      val result = HowToUpdatePageHarness
        .setup(cacheMap)
        .howToUpdatePage()

      status(result) mustBe SEE_OTHER

      verify(journeyCacheService, times(1)).cache(Matchers.eq(cacheMap))(any())
    }

    "employments return empty income is none" in {

      val cacheMap = Map(
        UpdateIncome_NameKey       -> "company",
        UpdateIncome_IdKey         -> "1",
        UpdateIncome_IncomeTypeKey -> TaiConstants.IncomeTypePension)

      val result = HowToUpdatePageHarness
        .setup(cacheMap, None)
        .howToUpdatePage()

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "processHowToUpdatePage" must {
    object ProcessHowToUpdatePageHarness {

      sealed class ProcessHowToUpdatePageHarness(incomeCount: Int, currentValue: Option[String]) {

        if (incomeCount >= 0) {
          when(incomeService.editableIncomes(any()))
            .thenReturn(BuildTaxCodeIncomes(incomeCount))
        }

        if (incomeCount == 1) {
          when(incomeService.singularIncomeId(any())).thenReturn(Some(1))
        }

        if (incomeCount == 0) {
          when(incomeService.singularIncomeId(any())).thenReturn(None)
        }

        currentValue match {
          case Some(x) =>
            when(journeyCacheService.currentValue(eqTo(UpdateIncome_HowToUpdateKey))(any()))
              .thenReturn(Future.successful(Some(x)))
          case None =>
        }

        def processHowToUpdatePage(employmentAmount: EmploymentAmount): Future[Result] =
          new TestIncomeUpdateHowToUpdateController()
            .processHowToUpdatePage(
              1,
              "name",
              employmentAmount,
              TaiSuccessResponseWithPayload(Seq.empty[TaxCodeIncome]))(
              RequestBuilder.buildFakeGetRequestWithAuth(),
              FakeAuthAction.user)
      }

      def setup(incomeCount: Int = -1, currentValue: Option[String] = None): ProcessHowToUpdatePageHarness =
        new ProcessHowToUpdatePageHarness(incomeCount, currentValue)
    }

    "redirect user for non live employment " when {
      "employment amount is occupation income" in {
        val employmentAmount = BuildEmploymentAmount()

        val result = ProcessHowToUpdatePageHarness
          .setup()
          .processHowToUpdatePage(employmentAmount)

        whenReady(result) { r =>
          r.header.status mustBe SEE_OTHER
          r.header.headers.get(LOCATION) mustBe Some(controllers.routes.IncomeController.pensionIncome().url)
        }
      }

      "employment amount is not occupation income" in {
        val employmentAmount = BuildEmploymentAmount(false, false)
        val result = ProcessHowToUpdatePageHarness
          .setup()
          .processHowToUpdatePage(employmentAmount)

        whenReady(result) { r =>
          r.header.status mustBe SEE_OTHER
          r.header.headers.get(LOCATION) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
        }
      }
    }

    "redirect user for is live employment " when {
      "editable incomes are greater than one and UpdateIncome_HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(2, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable incomes are greater than one and no cached UpdateIncome_HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(2)
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and UpdateIncome_HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(1, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and no cached UpdateIncome_HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(1)
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is none and UpdateIncome_HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(0, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }

      "editable income is none and no cached UpdateIncome_HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(0, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(true, false))

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }
    }
  }

  "handleChooseHowToUpdate" must {
    object HandleChooseHowToUpdateHarness {

      sealed class HandleChooseHowToUpdateHarness() {

        when(journeyCacheService.cache(Matchers.eq(UpdateIncome_HowToUpdateKey), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        def handleChooseHowToUpdate(request: FakeRequest[AnyContentAsFormUrlEncoded]): Future[Result] =
          new TestIncomeUpdateHowToUpdateController()
            .handleChooseHowToUpdate()(request)
      }

      def setup(): HandleChooseHowToUpdateHarness =
        new HandleChooseHowToUpdateHarness()
    }

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val result = HandleChooseHowToUpdateHarness
          .setup()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakePostRequestWithAuth("howToUpdate" -> "incomeCalculator"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateWorkingHoursController.workingHoursPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val result = HandleChooseHowToUpdateHarness
          .setup()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakePostRequestWithAuth("howToUpdate" -> "income"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val result = HandleChooseHowToUpdateHarness
          .setup()
          .handleChooseHowToUpdate(RequestBuilder
            .buildFakePostRequestWithAuth("howToUpdate" -> ""))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", ""))
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {
        val controller = new TestIncomeUpdateHowToUpdateController

        when(journeyCacheService.mandatoryJourneyValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any()))
          .thenReturn(Future.successful(Left("")))
        when(journeyCacheService.mandatoryJourneyValue(Matchers.eq(UpdateIncome_NameKey))(any()))
          .thenReturn(Future.successful(Left("")))

        val result = controller.handleChooseHowToUpdate(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
