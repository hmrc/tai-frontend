/*
 * Copyright 2018 HM Revenue & Customs
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
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses._
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.{PersonService, UpdateNextYearsIncomeService}
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import views.html.incomes.confirmAmountEntered
import views.html.incomes.nextYear.updateIncomeCYPlus1Start

import scala.concurrent.Future
import scala.util.Random

class UpdateIncomeNextYearControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with ControllerViewTestHelper {

  "start" must {
    "return OK with the start view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController
        val employmentID = 1
        val currentEstPay = 1234
        val nino = generateNino
        val model = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, currentEstPay)
        when(testController.updateNextYearsIncomeService.setup(Matchers.eq(employmentID), Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(model))

        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1Start(model)
      }
    }
  }

  "confirm" must {
    "for valid user" must {
      "that has entered an estimated amount" must {
        "respond with and ok and the view" in {
          implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
          val controller = createTestIncomeController

          val employerName = "EmployerName"
          val employmentId = 1
          val newAmount = 123

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentId, 1, Some(newAmount))
          when(
            controller.updateNextYearsIncomeService.get(Matchers.eq(employmentId), Matchers.eq(generateNino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val vm = ConfirmAmountEnteredViewModel.nextYearEstimatedPay(employmentId, employerName, newAmount)
          val expectedView = confirmAmountEntered(vm)

          val result = controller.confirm(employmentId)(fakeRequest)

          status(result)mustBe OK
          result rendersTheSameViewAs expectedView
        }
      }

      "that did not enter an estimated amount" must {
        "respond with internal server error" in {
          implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
          val controller = createTestIncomeController

          val employerName = "EmployerName"
          val employmentId = 1
          val newAmount = 123

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentId, 1, None)
          when(
            controller.updateNextYearsIncomeService.get(Matchers.eq(employmentId), Matchers.eq(generateNino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val result = controller.confirm(employmentId)(fakeRequest)

          status(result) mustBe INTERNAL_SERVER_ERROR
        }


        "respond with and Bad Request and redirect to the edit page " ignore {
          implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
          val controller = createTestIncomeController

          val employerName = "EmployerName"
          val employmentId = 1
          val newAmount = 123

          val serviceResponse = UpdateNextYearsIncomeCacheModel(employerName, employmentId, 1, None)
          when(
            controller.updateNextYearsIncomeService.get(Matchers.eq(employmentId), Matchers.eq(generateNino))(any())
          ).thenReturn(
            Future.successful(serviceResponse)
          )

          val result = controller.confirm(employmentId)(fakeRequest)

          status(result) mustBe BAD_REQUEST
          result rendersTheSameViewAs updateIncomeCYPlus1Start(serviceResponse)
        }
      }

    }
  }

  "handleConfirm" must {
    "for valid user" must {
      "for successful submit, redirect user to success page" in {
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
        val controller = createTestIncomeController

        val employerName = "EmployerName"
        val employmentId = 1
        val newAmount = 123

        when(
          controller.updateNextYearsIncomeService.submit(Matchers.eq(employmentId), Matchers.eq(generateNino))(any())
        ).thenReturn(
          Future.successful(TaiSuccessResponse)
        )

        val result = controller.handleConfirm(employmentId)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.success(employmentId).url)
      }

      "for unsuccessful submit, return an Internal Server error Response" in {
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
        val controller = createTestIncomeController

        val employerName = "EmployerName"
        val employmentId = 1
        val newAmount = 123

        when(
          controller.updateNextYearsIncomeService.submit(Matchers.eq(employmentId), Matchers.eq(generateNino))(any())
        ).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("Error"))
        )

        val result = controller.handleConfirm(employmentId)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }
  }

  private val generateNino = new Generator(new Random).nextNino

  private def createTestIncomeController: UpdateIncomeNextYearController = new UpdateIncomeNextYearController {
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever

    override val updateNextYearsIncomeService: UpdateNextYearsIncomeService = mock[UpdateNextYearsIncomeService]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]


    override protected val authConnector: AuthConnector = mock[AuthConnector]
    when(authConnector.currentAuthority(any(), any())).thenReturn {
      Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    }

    override val personService: PersonService = mock[PersonService]
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))

  }
}
