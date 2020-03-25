/*
 * Copyright 2020 HM Revenue & Customs
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
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, _}
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.forms.pensions.DuplicateSubmissionWarningForm
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.UpdateNextYearsIncomeService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.income.estimatedPay.update.DuplicateSubmissionCYPlus1EmploymentViewModel
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, NextYearPay}
import views.html.incomes.nextYear._

import scala.concurrent.Future

class UpdateIncomeNextYearControllerSpec
    extends PlaySpec with FakeTaiPlayApplication with FormValuesConstants with MockitoSugar
    with ControllerViewTestHelper {

  val employmentID = 1
  val currentEstPay = 1234
  val newEstPay = 9999
  val employerName = "EmployerName"
  val isPension = false

  val updateNextYearsIncomeService = mock[UpdateNextYearsIncomeService]

  "onPageLoad" must {
    "redirect to the duplicateSubmissionWarning url" when {
      "an income update has already been performed" in {

        val testController = createTestIncomeController()

        when(updateNextYearsIncomeService.isEstimatedPayJourneyCompleteForEmployer(meq(employmentID))(any()))
          .thenReturn(Future.successful(true))

        val result = testController.onPageLoad(employmentID)(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.duplicateWarning(employmentID).url)
      }
    }

    "redirect to the estimatedPayLanding url" in {
      val testController = createTestIncomeController()

      when(updateNextYearsIncomeService.isEstimatedPayJourneyCompleteForEmployer(meq(employmentID))(any()))
        .thenReturn(Future.successful(false))

      val result = testController.onPageLoad(employmentID)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.start(employmentID).url)
    }
  }

  "duplicateWarning" must {
    "show employment duplicateSubmissionWarning view" in {
      val testController = createTestIncomeController()

      val vm = DuplicateSubmissionCYPlus1EmploymentViewModel(employerName, newEstPay)

      implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
        RequestBuilder.buildFakeRequestWithOnlySession("GET")

      val result = testController.duplicateWarning(employmentID)(fakeRequest)

      status(result) mustBe OK

      result rendersTheSameViewAs updateIncomeCYPlus1Warning(
        DuplicateSubmissionWarningForm.createForm,
        vm,
        employmentID)
    }

    "redirect to the landing page if there is no new amount entered" in {
      val testController = createTestIncomeController()

      when(
        updateNextYearsIncomeService.getNewAmount(meq(employmentID))(any())
      ).thenReturn(
        Future.successful(Left("error"))
      )

      val result = testController.duplicateWarning(employmentID)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad.url)
    }
  }

  "submitDuplicateWarning" must {
    "redirect to the start url when yes is selected" in {
      val testController = createTestIncomeController()

      val result = testController.submitDuplicateWarning(employmentID)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(YesNoChoice -> YesValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.start(employmentID).url)
    }

    "redirect to the IncomeTaxComparison page url when no is selected" in {
      val testController = createTestIncomeController()

      val result = testController.submitDuplicateWarning(employmentID)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(YesNoChoice -> NoValue))

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad().url)
    }

    "redirect to the landing page if there is no new amount entered" in {
      val testController = createTestIncomeController()

      when(
        updateNextYearsIncomeService.getNewAmount(meq(employmentID))(any())
      ).thenReturn(
        Future.successful(Left("error"))
      )

      val result = testController.submitDuplicateWarning(employmentID)(
        RequestBuilder
          .buildFakeRequestWithAuth("POST"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad.url)
    }
  }

  "start" must {
    "return OK with the start view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1Start(employerName, employmentID, isPension)
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "edit" must {
    "return OK with the edit view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.edit(employmentID)(fakeRequest)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1Edit(
          employerName,
          employmentID,
          isPension,
          currentEstPay,
          AmountComparatorForm.createForm())
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

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

        when(
          updateNextYearsIncomeService
            .setNewAmount(meq(newEstPay), meq(employmentID), meq(nino))(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        val result = testController.update(employmentID)(
          RequestBuilder
            .buildFakeRequestWithOnlySession(POST)
            .withFormUrlEncodedBody("income" -> newEstPay))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.confirm(employmentID).url.toString)
      }

      "redirect to the no change page" when {
        "valid input is passed that matches the current estimated income" in {
          val testController = createTestIncomeController()
          val newEstPay = "1234"
          val result = testController.update(employmentID)(
            RequestBuilder
              .buildFakeRequestWithOnlySession(POST)
              .withFormUrlEncodedBody("income" -> newEstPay))
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.same(employmentID).url.toString)
        }

        "valid input is passed and no current amount has been cached" in {
          val testController = createTestIncomeController()
          val newEstPay = "1234"

          when(
            updateNextYearsIncomeService.getNewAmount(meq(employmentID))(any())
          ).thenReturn(
            Future.successful(Left("no amount entered"))
          )

          val result = testController.update(employmentID)(
            RequestBuilder
              .buildFakeRequestWithOnlySession(POST)
              .withFormUrlEncodedBody("income" -> newEstPay))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.same(employmentID).url.toString)
        }

        "valid input is passed and the new amount is the same as the current cached amount" in {
          val testController = createTestIncomeController()
          val newEstPay = "999"

          when(
            updateNextYearsIncomeService.getNewAmount(meq(employmentID))(any())
          ).thenReturn(
            Future.successful(Right(newEstPay.toInt))
          )

          val result = testController.update(employmentID)(
            RequestBuilder
              .buildFakeRequestWithOnlySession(POST)
              .withFormUrlEncodedBody("income" -> newEstPay))

          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.updateEmployment.incomeSame.title", ""))
        }
      }
    }

    "respond with a BAD_REQUEST" when {
      "no input is passed" in {
        val testController = createTestIncomeController()
        val newEstPay = ""

        implicit val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          RequestBuilder.buildFakeRequestWithOnlySession(POST).withFormUrlEncodedBody("income" -> newEstPay)

        val result: Future[Result] = testController.update(employmentID)(fakeRequest)

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs updateIncomeCYPlus1Edit(
          employerName,
          employmentID,
          isPension,
          currentEstPay,
          AmountComparatorForm.createForm().bindFromRequest()(fakeRequest))
      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)

        implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
          RequestBuilder.buildFakeRequestWithOnlySession("GET")

        val result: Future[Result] = testController.update(employmentID)(fakeRequest)

        status(result) mustBe NOT_FOUND
      }
    }

    "same" must {
      "return OK with the same view" when {
        "the estimated pay is retrieved successfully" in {

          val testController = createTestIncomeController()

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.same(employmentID)(fakeRequest)

          status(result) mustBe OK
          result rendersTheSameViewAs updateIncomeCYPlus1Same(employerName, employmentID, currentEstPay)
        }
      }

      "return NOT_FOUND" when {
        "CY Plus 1 is disabled" in {
          val testController = createTestIncomeController(isCyPlusOneEnabled = false)

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.same(employmentID)(fakeRequest)

          status(result) mustBe NOT_FOUND
        }
      }
    }

    "success" must {
      "return OK with the success view" when {
        "the estimated pay has been successfully submitted" in {

          val testController = createTestIncomeController()

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

          val result: Future[Result] = testController.success(employmentID)(fakeRequest)

          status(result) mustBe OK
          result rendersTheSameViewAs updateIncomeCYPlus1Success(employerName, isPension)
        }
      }

      "return NOT_FOUND" when {
        "CY Plus 1 is disabled" in {
          val testController = createTestIncomeController(isCyPlusOneEnabled = false)

          implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
            RequestBuilder.buildFakeRequestWithOnlySession("GET")

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

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentID, false, 1)
          when(
            updateNextYearsIncomeService.get(meq(employmentID), meq(nino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val vm = ConfirmAmountEnteredViewModel(employmentID, employerName, currentAmount, newAmount, NextYearPay)
          val expectedView = updateIncomeCYPlus1Confirm(vm)

          val result = controller.confirm(employmentID)(fakeRequest)

          status(result) mustBe OK
        }
      }

      "that did not enter an estimated amount" must {
        "redirect to the start of the journey" in {
          implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
          val controller = createTestIncomeController()

          when(
            updateNextYearsIncomeService.getNewAmount(meq(employmentID))(any())
          ).thenReturn(
            Future.successful(Left("error"))
          )

          val result = controller.confirm(employmentID)(fakeRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.IncomeTaxComparisonController.onPageLoad.url)
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
          updateNextYearsIncomeService.submit(meq(employmentID), meq(nino))(any())
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
          updateNextYearsIncomeService.submit(meq(employmentID), meq(nino))(any())
        ).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Error"))
        )

        val result = controller.handleConfirm(employmentID)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  private val nino = FakeAuthAction.nino

  private def createTestIncomeController(isCyPlusOneEnabled: Boolean = true): UpdateIncomeNextYearController =
    new TestUpdateIncomeNextYearController(isCyPlusOneEnabled) {
      val model = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, isPension, currentEstPay)

      when(updateNextYearsIncomeService.get(meq(employmentID), Matchers.any())(any()))
        .thenReturn(Future.successful(model))

      when(updateNextYearsIncomeService.getNewAmount(meq(employmentID))(any()))
        .thenReturn(Future.successful(Right(newEstPay)))
    }

  private class TestUpdateIncomeNextYearController(isCyPlusOneEnabled: Boolean)
      extends UpdateIncomeNextYearController(
        updateNextYearsIncomeService,
        mock[AuditConnector],
        FakeAuthAction,
        FakeValidatePerson,
        MockPartialRetriever,
        MockTemplateRenderer
      ) {
    override val cyPlusOneEnabled: Boolean = isCyPlusOneEnabled
  }

}
