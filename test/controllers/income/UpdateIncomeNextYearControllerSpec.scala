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

import builders.{AuthBuilder, RequestBuilder}
import controllers.{ControllerViewTestHelper, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, _}
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.{PersonService, UpdateNextYearsIncomeService}
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants
import uk.gov.hmrc.tai.viewModels.GoogleAnalyticsSettings
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import views.html.incomes.nextYear._

import scala.concurrent.Future
import scala.util.Random

class UpdateIncomeNextYearControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with ControllerViewTestHelper {

  val employmentID = 1
  val currentEstPay = 1234
  val employerName = "EmployerName"
  val isPension = false
  val model = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, isPension, currentEstPay)

  def mockedGet(testController: UpdateIncomeNextYearController) = {
    when(updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.any())(any()))
      .thenReturn(Future.successful(model))
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
        val nino = generateNino
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
        val nino = generateNino
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
          val nino = generateNino
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

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentID, false, 1, Some(newAmount))
          when(
            updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(generateNino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val vm = ConfirmAmountEnteredViewModel.nextYearEstimatedPay(employmentID, employerName, newAmount)
          val expectedView = updateIncomeCYPlus1Confirm(vm, GoogleAnalyticsSettings())

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
            updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(generateNino))(any())
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
            updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(generateNino))(any())
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
          updateNextYearsIncomeService.submit(Matchers.eq(employmentID), Matchers.eq(generateNino))(any())
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
          updateNextYearsIncomeService.submit(Matchers.eq(employmentID), Matchers.eq(generateNino))(any())
        ).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Error"))
        )

        val result = controller.handleConfirm(employmentID)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "gaSettings" must {
    "return a Google Analytics settings with the current and new amount in the dimensions" in {
      val controller = createTestIncomeController()
      val expectedDimensions = Some(Map(GoogleAnalyticsConstants.taiCYPlusOneEstimatedIncome -> "currentAmount=£123.00;newAmount=£456"))
      val expected = GoogleAnalyticsSettings(dimensions = expectedDimensions)

      controller.gaSettings(123, 456) mustBe expected
    }
  }

  private val generateNino = new Generator(new Random).nextNino

  private def createTestIncomeController(isCyPlusOneEnabled: Boolean = true): UpdateIncomeNextYearController = new TestUpdateIncomeNextYearController(isCyPlusOneEnabled)

  val personService: PersonService = mock[PersonService]
  val updateNextYearsIncomeService = mock[UpdateNextYearsIncomeService]

  private class TestUpdateIncomeNextYearController(isCyPlusOneEnabled: Boolean) extends UpdateIncomeNextYearController(
    updateNextYearsIncomeService,
    personService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    override val cyPlusOneEnabled: Boolean = isCyPlusOneEnabled

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))

  }

}
