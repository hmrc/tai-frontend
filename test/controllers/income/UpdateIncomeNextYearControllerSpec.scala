/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.actions.FakeValidatePerson
import controllers.{ControllerViewTestHelper, FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, _}
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.forms.pensions.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.UpdateNextYearsIncomeService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.DuplicateSubmissionCYPlus1EmploymentViewModel
import views.html.incomes.nextYear._

import scala.concurrent.Future

class UpdateIncomeNextYearControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with FormValuesConstants
  with MockitoSugar
  with ControllerViewTestHelper {

  val employmentID = 1
  val currentEstPay = 1234
  val newEstPay = 9999
  val employerName = "EmployerName"
  val isPension = false
  val model = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, isPension, currentEstPay, Some(newEstPay))

  def mockedGet(testController: UpdateIncomeNextYearController) = {
    when(updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.any())(any()))
      .thenReturn(Future.successful(model))
  }

  "onPageLoad" must {
    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {

        val testController = createTestIncomeController()

        when(updateNextYearsIncomeService.isEstimatedPayJourneyComplete(any())).thenReturn(Future.successful(true))

        val result = testController.onPageLoad(employmentID)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe routes.UpdateIncomeNextYearController.duplicateWarning(employmentID).url
      }
    }

    "redirect to the estimatedPayLanding url" when {
      "an income update has already been performed" in {

        val testController = createTestIncomeController()

        when(updateNextYearsIncomeService.isEstimatedPayJourneyComplete(any())).thenReturn(Future.successful(false))

        val result = testController.onPageLoad(employmentID)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe routes.UpdateIncomeNextYearController.start(employmentID).url
      }
    }
  }

  "duplicateSubmissionWarning" must {
    "show employment duplicateSubmissionWarning view" in {
      val testController = createTestIncomeController()

      val vm = DuplicateSubmissionCYPlus1EmploymentViewModel(employerName, newEstPay)
      mockedGet(testController)

      implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

      val result: Future[Result] = testController.duplicateWarning(employmentID)(fakeRequest)

      status(result) mustBe OK
      result rendersTheSameViewAs updateIncomeCYPlus1Warning(DuplicateSubmissionWarningForm.createForm, vm, employmentID)
    }
  }

  "submitDuplicateSubmissionWarning" must {
    "redirect to the start url when yes is selected" in {
      val testController = createTestIncomeController()

      mockedGet(testController)

      val result = testController.submitDuplicateWarning(employmentID)(RequestBuilder
        .buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> YesValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe routes.UpdateIncomeNextYearController.start(employmentID).url
    }

    "redirect to the IncomeTaxComparison page url when no is selected" in {
      val testController = createTestIncomeController()

      mockedGet(testController)

      val result = testController.submitDuplicateWarning(employmentID)(RequestBuilder
        .buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(YesNoChoice -> NoValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe controllers.routes.IncomeTaxComparisonController.onPageLoad().url
    }
  }

  "start" must {
    "return OK with the start view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()

        when(updateNextYearsIncomeService.reset(any())).thenReturn(Future.successful(TaiSuccessResponse))
        mockedGet(testController)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1Start(employerName, employmentID, isPension)
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "edit" must {
    "return OK with the edit view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()
        mockedGet(testController)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.edit(employmentID)(fakeRequest)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1Edit(employerName, employmentID, isPension, currentEstPay, AmountComparatorForm.createForm())
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.edit(employmentID)(fakeRequest)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "update" must {
    "redirect to the confirm page" when {
      "valid input is passed that is different from the current estimated income" in {
        val testController = createTestIncomeController()
        val newEstPay = "999"
        val updatedModel = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, isPension, currentEstPay, Some(newEstPay.toInt))

        when(updateNextYearsIncomeService.setNewAmount(Matchers.eq(newEstPay), Matchers.eq(employmentID), Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(updatedModel))

        val result = testController.update(employmentID)(
          RequestBuilder
            .buildFakeRequestWithOnlySession(POST)
            .withFormUrlEncodedBody("income" -> newEstPay))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.confirm(employmentID).url.toString)
      }
    }

    "redirect to the no change page" when {
      "valid input is passed that matches the current estimated income" in {
        val testController = createTestIncomeController()
        val newEstPay = 1234.toString
        val updatedModel = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, isPension, currentEstPay, Some(newEstPay.toInt))

        when(updateNextYearsIncomeService.setNewAmount(Matchers.eq(newEstPay), Matchers.eq(employmentID), Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(updatedModel))

        val result = testController.update(employmentID)(
          RequestBuilder
            .buildFakeRequestWithOnlySession(POST)
            .withFormUrlEncodedBody("income" -> newEstPay))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.same(employmentID).url.toString)
      }

      "redirect to the edit page" when {
        "new estimated income is not present on the cache model" in {
          val testController = createTestIncomeController()
          val newEstPay = 1234.toString
          val updatedModel = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, isPension, currentEstPay, None)

          when(updateNextYearsIncomeService.setNewAmount(Matchers.eq(newEstPay), Matchers.eq(employmentID), Matchers.eq(nino))(any()))
            .thenReturn(Future.successful(updatedModel))

          val result = testController.update(employmentID)(
            RequestBuilder
              .buildFakeRequestWithOnlySession(POST)
              .withFormUrlEncodedBody("income" -> newEstPay))

          status(result) mustBe SEE_OTHER

          redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.edit(employmentID).url.toString)
        }
      }
    }

    "respond with a BAD_REQUEST" when {
      "no input is passed" in {
        val testController = createTestIncomeController()
        val newEstPay = ""

        implicit val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          RequestBuilder.buildFakeRequestWithOnlySession(POST).withFormUrlEncodedBody("income" -> newEstPay)

        mockedGet(testController)

        val result: Future[Result] = testController.update(employmentID)(fakeRequest)

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs updateIncomeCYPlus1Edit(employerName, employmentID, isPension, currentEstPay, AmountComparatorForm.createForm().bindFromRequest()(fakeRequest))
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.update(employmentID)(fakeRequest)

        status(result) mustBe NOT_FOUND
      }
    }

    "same" must {
      "return OK with the same view" when {
        "the estimated pay is retrieved successfully" in {

          val testController = createTestIncomeController()

          when(updateNextYearsIncomeService.reset(any())).thenReturn(Future.successful(TaiSuccessResponse))
          mockedGet(testController)

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.same(employmentID)(fakeRequest)

          status(result) mustBe OK
          result rendersTheSameViewAs updateIncomeCYPlus1Same(employerName, employmentID, currentEstPay)
        }
      }

      "return NOT_FOUND" when {
        "CY Plus 1 is disabled" in {
          val testController = createTestIncomeController(isCyPlusOneEnabled = false)

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.same(employmentID)(fakeRequest)

          status(result) mustBe NOT_FOUND
        }
      }
    }

    "success" must {
      "return OK with the success view" when {
        "the estimated pay has been successfully submitted" in {

          val testController = createTestIncomeController()

          when(updateNextYearsIncomeService.reset(any())).thenReturn(Future.successful(TaiSuccessResponse))
          mockedGet(testController)

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.success(employmentID)(fakeRequest)

          status(result) mustBe OK
          result rendersTheSameViewAs updateIncomeCYPlus1Success(employerName, isPension)
        }
      }

      "return NOT_FOUND" when {
        "CY Plus 1 is disabled" in {
          val testController = createTestIncomeController(isCyPlusOneEnabled = false)

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.success(employmentID)(fakeRequest)

          status(result) mustBe NOT_FOUND
        }
      }
    }
  }

  "confirm" must {
    "for valid user" must {
      "that has entered an estimated amount" must {
        "respond with and ok and the view" in {
          implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
          val controller = createTestIncomeController()

          val newAmount = 123
          val currentAmount = 1

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentID, false, 1, Some(newAmount))
          when(
            updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(nino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val vm = ConfirmAmountEnteredViewModel.nextYearEstimatedPay(employmentID, employerName, currentAmount, newAmount)
          val expectedView = updateIncomeCYPlus1Confirm(vm)

          val result = controller.confirm(employmentID)(fakeRequest)

          status(result) mustBe OK
        }
      }

      "that did not enter an estimated amount" must {
        "respond with internal server error" in {
          implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
          val controller = createTestIncomeController()

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentID, false, 1, None)
          when(
            updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(nino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val result = controller.confirm(employmentID)(fakeRequest)

          status(result) mustBe INTERNAL_SERVER_ERROR
        }


        "respond with and Bad Request and redirect to the edit page" ignore {
          implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
          val controller = createTestIncomeController()

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentID, false, 1, None)
          when(
            updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(nino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val result = controller.confirm(employmentID)(fakeRequest)

          status(result) mustBe BAD_REQUEST
          result rendersTheSameViewAs updateIncomeCYPlus1Start(employerName, employmentID, isPension)
        }
      }

    }
  }

  "handleConfirm" must {
    "for valid user" must {
      "for successful submit, redirect user to success page" in {
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
        val controller = createTestIncomeController()

        when(
          updateNextYearsIncomeService.submit(Matchers.eq(employmentID), Matchers.eq(nino))(any())
        ).thenReturn(
          Future.successful(TaiSuccessResponse)
        )

        val result = controller.handleConfirm(employmentID)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.success(employmentID).url)
      }

      "for unsuccessful submit, return an Internal Server error Response" in {
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
        val controller = createTestIncomeController()

        when(
          updateNextYearsIncomeService.submit(Matchers.eq(employmentID), Matchers.eq(nino))(any())
        ).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Error"))
        )

        val result = controller.handleConfirm(employmentID)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  private val nino = FakeAuthAction.nino

  private def createTestIncomeController(isCyPlusOneEnabled: Boolean = true): UpdateIncomeNextYearController = new TestUpdateIncomeNextYearController(isCyPlusOneEnabled)

  val updateNextYearsIncomeService = mock[UpdateNextYearsIncomeService]

  private class TestUpdateIncomeNextYearController(isCyPlusOneEnabled: Boolean) extends UpdateIncomeNextYearController(
    updateNextYearsIncomeService,
    mock[AuditConnector],
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    override val cyPlusOneEnabled: Boolean = isCyPlusOneEnabled
  }

}
