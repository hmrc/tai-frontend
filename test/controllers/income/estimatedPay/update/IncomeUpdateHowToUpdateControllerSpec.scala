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
import controllers.ErrorPagesHandler
import controllers.auth.AuthedUser
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import pages.income.{UpdateIncomeIdPage, UpdateIncomeNamePage, UpdateIncomeTypePage, UpdateIncomeUpdateKeyPage}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live, OtherBasisOfOperation, TaxCodeIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants._
import utils.BaseSpec
import views.html.incomes.HowToUpdateView

import java.time.LocalDate
import scala.concurrent.Future
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

  val mockIncomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  class SUT
      extends IncomeUpdateHowToUpdateController(
        mockAuthJourney,
        employmentService,
        mockIncomeService,
        taxAccountService,
        mcc,
        inject[HowToUpdateView],
        mockJourneyCacheNewRepository,
        inject[ErrorPagesHandler]
      ) {
    when(mockJourneyCacheNewRepository.get(any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId))))
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
    setup(UserAnswers(sessionId))
    reset(mockJourneyCacheNewRepository)

    when(taxAccountService.taxCodeIncomes(any(), any())(any()))
      .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))

    when(employmentService.employment(any(), any())(any()))
      .thenReturn(Future.successful(Some(defaultEmployment)))

    when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(BuildEmploymentAmount()))
  }

  "howToUpdatePage" must {
    "render the right response to the user" in {
      val mockUserAnswers = UserAnswers(sessionId)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypePension)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
      when(mockJourneyCacheNewRepository.get(any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result =
        SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome(1).url)
    }

    "cache the employer details" in {
      val mockUserAnswers = UserAnswers(sessionId)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
      when(mockJourneyCacheNewRepository.get(any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result =
        SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      verify(mockJourneyCacheNewRepository).set(any())
    }

    "employments return empty income is none" in {
      reset(mockJourneyCacheNewRepository)

      val mockUserAnswers = UserAnswers(sessionId)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypePension)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result =
        SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "processHowToUpdatePage" must {

    "redirect user for non live employment " when {
      "employment amount is occupation income" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)

        val employmentAmount = BuildEmploymentAmount()
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(2))

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome(1).url)
      }

      "employment amount is not occupation income" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val employmentAmount = BuildEmploymentAmount(isOccupationPension = false)
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(0))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }

    "redirect user for is live employment " when {
      "editable incomes are greater than one and UpdateIncomeConstants.HowToUpdateKey has a cached value" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "incomeCalculator")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.editableIncomes(any())).thenReturn(BuildTaxCodeIncomes(2))

        val employmentAmount = BuildEmploymentAmount(isLive = true, isOccupationPension = false)
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(2))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable incomes are greater than one and no cached UpdateIncomeConstants.HowToUpdateKey" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.editableIncomes(any())).thenReturn(BuildTaxCodeIncomes(2))

        val employmentAmount = BuildEmploymentAmount(isLive = true, isOccupationPension = false)
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(2))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable income is singular and UpdateIncomeConstants.HowToUpdateKey has a cached value" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "incomeCalculator")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.singularIncomeId(any())).thenReturn(Some(1))

        val employmentAmount = BuildEmploymentAmount(isLive = true, isOccupationPension = false)
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(1))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable income is singular and no cached UpdateIncomeConstants.HowToUpdateKey" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.singularIncomeId(any())).thenReturn(Some(1))

        val employmentAmount = BuildEmploymentAmount(isLive = true, isOccupationPension = false)
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(1))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", "name"))
      }

      "editable income is none and UpdateIncomeConstants.HowToUpdateKey has a cached value" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "incomeCalculator")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockIncomeService.editableIncomes(any())).thenReturn(BuildTaxCodeIncomes(0))
        when(mockIncomeService.singularIncomeId(any())).thenReturn(None)

        val employmentAmount = BuildEmploymentAmount(isLive = true, isOccupationPension = false)
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(0))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }

      "editable income is none and no cached UpdateIncomeConstants.HowToUpdateKey" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val employmentAmount = BuildEmploymentAmount(isLive = true, isOccupationPension = false)
        val maybeTaxCodeIncomeDetails = Right(BuildTaxCodeIncomes(0))

        implicit val request: Request[AnyContent] = FakeRequest("GET", "/")
        implicit val user: AuthedUser = AuthedUser(
          Nino(nino.toString()),
          Some("saUtr"),
          None
        )

        val result =
          SUT.processHowToUpdatePage(
            empId,
            "name",
            employmentAmount,
            maybeTaxCodeIncomeDetails,
            userAnswers
          )(request, user)

        val ex = the[RuntimeException] thrownBy whenReady(result) { r =>
          r
        }

        assert(ex.getMessage.contains("Employment id not present"))
      }
    }
  }

  "handleChooseHowToUpdate" must {

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "incomeCalculator")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.handleChooseHowToUpdate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateWorkingHoursController.workingHoursPage().url
        )
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "income")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.handleChooseHowToUpdate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("howToUpdate" -> "income")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handleChooseHowToUpdate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody("howToUpdate" -> "")
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messages("tai.howToUpdate.title", ""))
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {
        val mockUserAnswers = UserAnswers(sessionId)
          .setOrException(UpdateIncomeUpdateKeyPage, "income")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(mockJourneyCacheNewRepository.get(any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.handleChooseHowToUpdate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
