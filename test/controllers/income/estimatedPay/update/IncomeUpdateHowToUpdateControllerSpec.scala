/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.auth.{AuthedUser, DataRequest}
import controllers.{ErrorPagesHandler, FakeAuthRetrievals}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatest.concurrent.ScalaFutures
import pages.income.{UpdateIncomeIdPage, UpdateIncomeNamePage, UpdateIncomeTypePage}
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsFormUrlEncoded, BodyParser, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.service._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.util.constants._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec
import views.html.incomes.HowToUpdateView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class IncomeUpdateHowToUpdateControllerSpec extends BaseSpec with ScalaFutures {

  def randomNino(): Nino = new Generator(new Random()).nextNino
  def createSUT = new SUT

  val empId: Int = 1
  val sessionId: String = "testSessionId"
  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")

  val defaultEmployment: Employment =
    Employment(
      "company",
      Live,
      Some("123"),
      Some(LocalDate.parse("2016-05-26")),
      None,
      Nil,
      "",
      "",
      1,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )

  val incomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  class SUT
      extends IncomeUpdateHowToUpdateController(
        mockAuthJourney,
        employmentService,
        incomeService,
        taxAccountService,
        mcc,
        inject[HowToUpdateView],
        mockJourneyCacheNewRepository,
        inject[ErrorPagesHandler]
      ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  private def setup(ua: UserAnswers): ScalaOngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
    when(mockAuthJourney.authWithDataRetrieval) thenReturn new ActionBuilder[DataRequest, AnyContent] {
      override def invokeBlock[A](
        request: Request[A],
        block: DataRequest[A] => Future[Result]
      ): Future[Result] =
        block(
          DataRequest(
            request,
            taiUser = AuthedUser(
              Nino(nino.toString()),
              Some("saUtr"),
              None
            ),
            fullName = "",
            userAnswers = ua
          )
        )

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    }

  def BuildEmploymentAmount(isLive: Boolean = false, isOccupationPension: Boolean = true): EmploymentAmount =
    EmploymentAmount(
      name = "name",
      description = "description",
      employmentId = employer.id,
      newAmount = 200,
      oldAmount = 200,
      isLive = isLive,
      isOccupationalPension = isOccupationPension
    )

  def BuildTaxCodeIncomes(incomeCount: Int): Seq[TaxCodeIncome] = {

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

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheNewRepository)

    when(taxAccountService.taxCodeIncomes(any(), any())(any()))
      .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))

    when(employmentService.employment(any(), any())(any()))
      .thenReturn(Future.successful(Some(defaultEmployment)))

    when(incomeService.employmentAmount(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(BuildEmploymentAmount()))
  }

  "howToUpdatePage" must {
    "render the right response to the user" in {
      reset(mockJourneyCacheNewRepository)

      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypePension)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result =
        SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome(1).url)
    }

    "cache the employer details" in {
      reset(mockJourneyCacheNewRepository)

      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result =
        SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      verify(mockJourneyCacheNewRepository).set(any())
    }

    "employments return empty income is none" in {
      reset(mockJourneyCacheNewRepository)

      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypePension)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result =
        SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "processHowToUpdatePage" must {

//        if (incomeCount >= 0) {
//          when(incomeService.editableIncomes(any()))
//            .thenReturn(BuildTaxCodeIncomes(incomeCount))
//        }
//
//        if (incomeCount == 1) {
//          when(incomeService.singularIncomeId(any())).thenReturn(Some(1))
//        }
//
//        if (incomeCount == 0) {
//          when(incomeService.singularIncomeId(any())).thenReturn(None)
//        }


//        def processHowToUpdatePage(employmentAmount: EmploymentAmount): Future[Result] =
//          new TestIncomeUpdateHowToUpdateController()
//            .processHowToUpdatePage(1, "name", employmentAmount, Right(Seq.empty[TaxCodeIncome]))(
//              RequestBuilder.buildFakeGetRequestWithAuth(),
//              FakeAuthRetrievals.user
//            )
//      }


    "redirect user for non live employment " when {
      "employment amount is occupation income" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeHowToUpdatePage, 1)

        val employmentAmount = BuildEmploymentAmount()

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))


        val result = ProcessHowToUpdatePageHarness
          .setup()
          .processHowToUpdatePage(employmentAmount)

        whenReady(result) { r =>
          r.header.status mustBe SEE_OTHER
          r.header.headers.get(LOCATION) mustBe Some(controllers.routes.IncomeController.pensionIncome(1).url)
        }
      }

      "employment amount is not occupation income" in {
        val employmentAmount = BuildEmploymentAmount(isOccupationPension = false)
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
      "editable incomes are greater than one and UpdateIncomeConstants.HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(2, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(isLive = true, isOccupationPension = false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable incomes are greater than one and no cached UpdateIncomeConstants.HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(2)
          .processHowToUpdatePage(BuildEmploymentAmount(isLive = true, isOccupationPension = false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and UpdateIncomeConstants.HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(1, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(isLive = true, isOccupationPension = false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is singular and no cached UpdateIncomeConstants.HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(1)
          .processHowToUpdatePage(BuildEmploymentAmount(isLive = true, isOccupationPension = false))

        whenReady(result) { r =>
          r.header.status mustBe OK
          val doc = Jsoup.parse(contentAsString(Future.successful(r)))
          doc.title() must include(messages("tai.howToUpdate.title", "name"))
        }
      }

      "editable income is none and UpdateIncomeConstants.HowToUpdateKey has a cached value" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(0, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(isLive = true, isOccupationPension = false))

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }

      "editable income is none and no cached UpdateIncomeConstants.HowToUpdateKey" in {

        val result = ProcessHowToUpdatePageHarness
          .setup(0, Some("incomeCalculator"))
          .processHowToUpdatePage(BuildEmploymentAmount(isLive = true, isOccupationPension = false))

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

        when(journeyCacheService.cache(meq(UpdateIncomeConstants.HowToUpdateKey), any())(any()))
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
          .handleChooseHowToUpdate(
            RequestBuilder
              .buildFakePostRequestWithAuth("howToUpdate" -> "incomeCalculator")
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateWorkingHoursController.workingHoursPage().url
        )
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val result = HandleChooseHowToUpdateHarness
          .setup()
          .handleChooseHowToUpdate(
            RequestBuilder
              .buildFakePostRequestWithAuth("howToUpdate" -> "income")
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val result = HandleChooseHowToUpdateHarness
          .setup()
          .handleChooseHowToUpdate(
            RequestBuilder
              .buildFakePostRequestWithAuth("howToUpdate" -> "")
          )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", ""))
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {
        val controller = new TestIncomeUpdateHowToUpdateController

        when(journeyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Left("")))

        val result = controller.handleChooseHowToUpdate(RequestBuilder.buildFakePostRequestWithAuth())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
