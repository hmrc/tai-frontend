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

package controllers.income

import builders.RequestBuilder
import controllers.{ControllerViewTestHelper, ErrorPagesHandler}
import org.apache.pekko.Done
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, when}
import pages.income.UpdateNextYearsIncomeNewAmountPage
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.forms.pensions.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.admin.CyPlusOneToggle
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.UpdateNextYearsIncomeService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.DuplicateSubmissionCYPlus1EmploymentViewModel
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, NextYearPay}
import utils.BaseSpec
import views.html.incomes.SameEstimatedPayView
import views.html.incomes.nextYear._

import scala.concurrent.Future
import scala.util.Random

class UpdateIncomeNextYearControllerSpec extends BaseSpec with ControllerViewTestHelper {

  private class TestUpdateIncomeNextYearController
      extends UpdateIncomeNextYearController(
        updateNextYearsIncomeService,
        mock[AuditConnector],
        mockAuthJourney,
        mcc,
        updateIncomeCYPlus1SuccessView,
        updateIncomeCYPlus1ConfirmView,
        updateIncomeCYPlus1WarningView,
        updateIncomeCYPlus1StartView,
        updateIncomeCYPlus1EditView,
        updateIncomeCYPlus1SameView,
        inject[SameEstimatedPayView],
        mockFeatureFlagService,
        inject[ErrorPagesHandler]
      )

  private val updateIncomeCYPlus1ConfirmView = inject[UpdateIncomeCYPlus1ConfirmView]
  private val updateIncomeCYPlus1SuccessView = inject[UpdateIncomeCYPlus1SuccessView]
  private val updateIncomeCYPlus1SameView    = inject[UpdateIncomeCYPlus1SameView]
  private val updateIncomeCYPlus1EditView    = inject[UpdateIncomeCYPlus1EditView]
  private val updateIncomeCYPlus1StartView   = inject[UpdateIncomeCYPlus1StartView]
  private val updateIncomeCYPlus1WarningView = inject[UpdateIncomeCYPlus1WarningView]

  val employmentID       = 1
  val currentEstPay      = 1234
  val newEstPay          = 9999
  val isPension          = false
  val sessionId          = "testSessionId"
  def randomNino(): Nino = new Generator(new Random()).nextNino

  val updateNextYearsIncomeService: UpdateNextYearsIncomeService = mock[UpdateNextYearsIncomeService]
  val mockJourneyCacheRepository: JourneyCacheRepository         = mock[JourneyCacheRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheRepository, updateNextYearsIncomeService, mockFeatureFlagService)
  }

  "onPageLoad" must {
    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {

        val testController = createTestIncomeController()

        when(updateNextYearsIncomeService.isEstimatedPayJourneyCompleteForEmployer(any(), any()))
          .thenReturn(Future.successful(true))

        val result = testController.onPageLoad(employmentID)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.duplicateWarning(employmentID).url)
      }
    }

    "redirect to the estimatedPayLanding url" in {
      val testController = createTestIncomeController()

      when(updateNextYearsIncomeService.isEstimatedPayJourneyCompleteForEmployer(any(), any()))
        .thenReturn(Future.successful(false))

      val result = testController.onPageLoad(employmentID)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.start(employmentID).url)
    }
  }

  "duplicateWarning" must {
    "show employment duplicateSubmissionWarning view" in {
      val testController = createTestIncomeController()

      val vm      = DuplicateSubmissionCYPlus1EmploymentViewModel(employerName, newEstPay)
      val request = RequestBuilder.buildFakeRequestWithOnlySession("GET")

      val result = testController.duplicateWarning(employmentID)(request)

      status(result) mustBe OK

      result rendersTheSameViewAs updateIncomeCYPlus1WarningView(
        DuplicateSubmissionWarningForm.createForm,
        vm,
        employmentID
      )(request, authedUser, messages)
    }

    "redirect to the landing page if there is no new amount entered" in {
      val testController = createTestIncomeController()

      when(
        updateNextYearsIncomeService.getNewAmount(any(), any())
      ).thenReturn(
        Future.successful(Left("error"))
      )

      val result = testController.duplicateWarning(employmentID)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad().url)
    }
  }

  "submitDuplicateWarning" must {
    "redirect to the start url when yes is selected" in {
      val testController = createTestIncomeController()

      val result = testController.submitDuplicateWarning(employmentID)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
      )

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.start(employmentID).url)
    }

    "redirect to the IncomeTaxComparison page url when no is selected" in {
      val testController = createTestIncomeController()

      val result = testController.submitDuplicateWarning(employmentID)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
      )

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad().url)
    }

    "redirect to the landing page if there is no new amount entered" in {
      val testController = createTestIncomeController()

      when(
        updateNextYearsIncomeService.getNewAmount(any(), any())
      ).thenReturn(
        Future.successful(Left("error"))
      )

      val result = testController.submitDuplicateWarning(employmentID)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
      )

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad().url)
    }
  }

  "start" must {
    "return OK with the start view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()

        val request: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.start(employmentID)(request)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1StartView(employerName, employmentID, isPension)(
          request,
          messages,
          authedUser
        )
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.start(employmentID)(request)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "edit" must {
    "return OK with the edit view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()

        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.edit(employmentID)(request)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1EditView(
          employerName,
          employmentID,
          isPension,
          Some(currentEstPay),
          AmountComparatorForm.createForm()
        )
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.edit(employmentID)(request)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "update" must {
    "redirect to the confirm page" when {
      "valid input is passed that is different from the current estimated income" in {
        val testController = createTestIncomeController()
        val newEstPay      = "999"

        val expectedResult = Map(UpdateNextYearsIncomeNewAmountPage(employmentID).toString -> newEstPay)

        when(updateNextYearsIncomeService.setNewAmount(meq(newEstPay), meq(employmentID), any()))
          .thenReturn(Future.successful(expectedResult))

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = testController.update(employmentID)(
          RequestBuilder
            .buildFakeRequestWithOnlySession(POST)
            .withFormUrlEncodedBody("income" -> newEstPay)
        )

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.confirm(employmentID).url)
      }

      "redirect to the no change page" when {
        "valid input is passed that matches the current estimated income" in {
          val testController = createTestIncomeController()
          val newEstPay      = "1234"
          val result         = testController.update(employmentID)(
            RequestBuilder
              .buildFakeRequestWithOnlySession(POST)
              .withFormUrlEncodedBody("income" -> newEstPay)
          )
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.same(employmentID).url)
        }

        "valid input is passed and no current amount has been cached" in {
          val testController = createTestIncomeController()
          val newEstPay      = "1234"

          when(
            updateNextYearsIncomeService.getNewAmount(any(), any())
          ).thenReturn(
            Future.successful(Left("no amount entered"))
          )

          val result = testController.update(employmentID)(
            RequestBuilder
              .buildFakeRequestWithOnlySession(POST)
              .withFormUrlEncodedBody("income" -> newEstPay)
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.same(employmentID).url)
        }

        "valid input is passed and the new amount is the same as the current cached amount" in {
          val testController = createTestIncomeController()
          val newEstPay      = "999"

          when(
            updateNextYearsIncomeService.getNewAmount(any(), any())
          ).thenReturn(
            Future.successful(Right(newEstPay.toInt))
          )

          when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

          val result = testController.update(employmentID)(
            RequestBuilder
              .buildFakeRequestWithOnlySession(POST)
              .withFormUrlEncodedBody("income" -> newEstPay)
          )

          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.updateEmployment.incomeSame.title", ""))
        }
      }
    }

    "respond with a BAD_REQUEST" when {
      "no input is passed" in {
        val testController = createTestIncomeController()
        val newEstPay      = ""

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
          RequestBuilder.buildFakeRequestWithOnlySession(POST).withFormUrlEncodedBody("income" -> newEstPay)

        val result: Future[Result] = testController.update(employmentID)(request)

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs updateIncomeCYPlus1EditView(
          employerName,
          employmentID,
          isPension,
          Some(currentEstPay),
          AmountComparatorForm
            .createForm()
            .bindFromRequest()
        )(request, messages, authedUser)
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        val request: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.update(employmentID)(request)

        status(result) mustBe NOT_FOUND
      }
    }

    "same" must {
      "return OK with the same view" when {
        "the estimated pay is retrieved successfully" in {

          val testController = createTestIncomeController()

          val request: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.same(employmentID)(request)

          status(result) mustBe OK
          result rendersTheSameViewAs updateIncomeCYPlus1SameView(employerName, Some(currentEstPay))(
            request,
            messages,
            authedUser
          )
        }
      }

      "return NOT_FOUND" when {
        "CY Plus 1 is disabled" in {
          val testController = createTestIncomeController(isCyPlusOneEnabled = false)

          val request: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.same(employmentID)(request)

          status(result) mustBe NOT_FOUND
        }
      }
    }

    "success" must {
      "return OK with the success view" when {
        "the estimated pay has been successfully submitted" in {

          val testController = createTestIncomeController()

          val request: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.success(employmentID)(request)

          status(result) mustBe OK
          result rendersTheSameViewAs updateIncomeCYPlus1SuccessView(employerName, isPension)(
            request,
            messages,
            authedUser
          )
        }
      }

      "return NOT_FOUND" when {
        "CY Plus 1 is disabled" in {
          val testController = createTestIncomeController(isCyPlusOneEnabled = false)

          val request: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.success(employmentID)(request)

          status(result) mustBe NOT_FOUND
        }
      }
    }
  }

  "confirm" must {
    "for valid user" must {
      "that has entered an estimated amount" must {
        "respond with and ok and the view" in {
          val request    = RequestBuilder.buildFakeGetRequestWithAuth()
          val controller = createTestIncomeController()

          val newAmount     = newEstPay
          val currentAmount = 1

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentID, isPension = false, Some(1))
          when(
            updateNextYearsIncomeService.get(meq(employmentID), meq(nino), any[UserAnswers])(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val vm           = ConfirmAmountEnteredViewModel(
            employmentID,
            employerName,
            Some(currentAmount),
            newAmount,
            NextYearPay,
            "#"
          )
          val expectedView = updateIncomeCYPlus1ConfirmView(vm)(
            request,
            messages,
            authedUser
          )

          val result = controller.confirm(employmentID)(request)

          status(result) mustBe OK

          result rendersTheSameViewAs expectedView
        }
      }

      "that did not enter an estimated amount" must {
        "redirect to the start of the journey" in {
          val request    = RequestBuilder.buildFakeGetRequestWithAuth()
          val controller = createTestIncomeController()

          when(
            updateNextYearsIncomeService.getNewAmount(any(), any())
          ).thenReturn(
            Future.successful(Left("error"))
          )

          val result = controller.confirm(employmentID)(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad().url)
        }
      }
    }
  }

  "handleConfirm" must {
    "for valid user" must {
      "for successful submit, redirect user to success page" in {
        val request    = RequestBuilder.buildFakeGetRequestWithAuth()
        val controller = createTestIncomeController()

        when(
          updateNextYearsIncomeService.submit(any(), any(), any())(any(), any())
        ).thenReturn(
          Future.successful(Done)
        )

        val result = controller.handleConfirm(employmentID)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.success(employmentID).url)
      }

      "for unsuccessful submit, return an Internal Server error Response" in {
        val request    = RequestBuilder.buildFakeGetRequestWithAuth()
        val controller = createTestIncomeController()

        when(
          updateNextYearsIncomeService.submit(any(), any(), any())(any(), any())
        ).thenReturn(
          Future.failed(new Exception("Error"))
        )

        val result = controller.handleConfirm(employmentID)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "update" should {
    "render sameEstimatedPay view with correct model when new amount matches cached new amount" in {
      val controller = createTestIncomeController()
      val newEstPay  = "999"

      when(updateNextYearsIncomeService.getNewAmount(any(), any()))
        .thenReturn(Future.successful(Right(newEstPay.toInt)))

      val result = controller.update(employmentID)(
        RequestBuilder
          .buildFakeRequestWithOnlySession(POST)
          .withFormUrlEncodedBody("income" -> newEstPay)
      )

      status(result) mustBe OK
      val body = Jsoup.parse(contentAsString(result)).body().text()
      body must include("999")
      body must include(employerName)
    }
  }

  private def createTestIncomeController(isCyPlusOneEnabled: Boolean = true): UpdateIncomeNextYearController =
    new TestUpdateIncomeNextYearController() {
      val model: UpdateNextYearsIncomeCacheModel =
        UpdateNextYearsIncomeCacheModel("employer name", employmentID, isPension, Some(currentEstPay))

      when(mockFeatureFlagService.get(org.mockito.ArgumentMatchers.eq(CyPlusOneToggle))) thenReturn
        Future.successful(FeatureFlag(CyPlusOneToggle, isEnabled = isCyPlusOneEnabled))

      when(updateNextYearsIncomeService.get(meq(employmentID), any(), any[UserAnswers])(any()))
        .thenReturn(Future.successful(model))

      when(updateNextYearsIncomeService.getNewAmount(any(), any()))
        .thenReturn(Future.successful(Right(newEstPay)))
    }
}
