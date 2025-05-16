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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import pages.income.{UpdateIncomeIdPage, UpdateIncomeNamePage, UpdateIncomeTypePage, UpdateIncomeUpdateKeyPage}
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{IncomeSource, Live}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.TaiConstants
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
      receivingOccupationalPension = false,
      EmploymentIncome
    )

  val mockIncomeService: IncomeService = mock[IncomeService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  class SUT
      extends IncomeUpdateHowToUpdateController(
        mockAuthJourney,
        employmentService,
        mockIncomeService,
        mcc,
        inject[HowToUpdateView],
        mockJourneyCacheRepository,
        inject[ErrorPagesHandler]
      )

  def BuildEmploymentAmount(isLive: Boolean = false, isOccupationPension: Boolean = true): EmploymentAmount =
    EmploymentAmount(
      name = "name",
      description = "description",
      employmentId = employer.id,
      oldAmount = Some(200),
      isLive = isLive,
      isOccupationalPension = isOccupationPension
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheRepository)

    when(employmentService.employment(any(), any())(any()))
      .thenReturn(Future.successful(Some(defaultEmployment)))

    when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(BuildEmploymentAmount()))
  }

  "howToUpdatePage" must {
    "redirect to pension income page if employment is pension-related" in {
      val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
      when(mockJourneyCacheRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(BuildEmploymentAmount(isLive = true)))

      val result = SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeController.pensionIncome(empId).url)
    }

    "render the how to update form if live and not a pension" in {
      val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
      when(mockJourneyCacheRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(BuildEmploymentAmount(isLive = true, isOccupationPension = false)))

      val result = SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(messages("tai.howToUpdate.title", "company"))
    }

    "redirect to summary page if not live and not a pension" in {
      val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypeEmployment)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
      when(mockJourneyCacheRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      when(mockIncomeService.employmentAmount(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(BuildEmploymentAmount(isOccupationPension = false)))

      val result = SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
    }

    "return INTERNAL_SERVER_ERROR when employment not found" in {
      reset(mockJourneyCacheRepository)

      val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
        .setOrException(UpdateIncomeNamePage, "company")
        .setOrException(UpdateIncomeIdPage, 1)
        .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypePension)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result = SUT.howToUpdatePage(empId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "handleChooseHowToUpdate" must {

    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "incomeCalculator")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

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
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "income")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

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
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeNamePage, "company")
          .setOrException(UpdateIncomeIdPage, 1)
          .setOrException(UpdateIncomeUpdateKeyPage, "")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

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
        val mockUserAnswers = UserAnswers(sessionId, randomNino().nino)
          .setOrException(UpdateIncomeUpdateKeyPage, "income")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)
        when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

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
