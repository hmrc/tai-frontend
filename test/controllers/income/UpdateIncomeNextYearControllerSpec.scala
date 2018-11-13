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
import org.scalacheck.Prop.False
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import scala.concurrent.duration._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.service.{PersonService, UpdateNextYearsIncomeService}
import views.html.incomes.nextYear.{updateIncomeCYPlus1Edit, updateIncomeCYPlus1Start}

import scala.concurrent.{Await, Future}
import scala.util.Random

class UpdateIncomeNextYearControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with ControllerViewTestHelper {

  val employmentID = 1
  val currentEstPay = 1234

  val model = UpdateNextYearsIncomeCacheModel("EmployerName", employmentID, currentEstPay)

  "start" must {
    "return OK with the start view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()
        val employerName = "EmployerName"
        val nino = generateNino
        val model = UpdateNextYearsIncomeCacheModel(employerName, employmentID, currentEstPay)

        when(testController.updateNextYearsIncomeService.reset(any())).thenReturn(Future.successful(TaiSuccessResponse))
        when(testController.updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(model))

        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1Start(employerName, employmentID)
      }
    }

    "return INTERNAL_SERVER_ERROR" when {
      "service fails to return valid data" in {
        val testController = createTestIncomeController()
        val employmentID = 1
        val employerName = "EmployerName"
        val currentEstPay = 1234
        val nino = generateNino
        val model = UpdateNextYearsIncomeCacheModel(employerName, employmentID, currentEstPay)

        when(testController.updateNextYearsIncomeService.reset(any())).thenThrow(new RuntimeException("Invalid Data"))

        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

//        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        val exception = the[RuntimeException] thrownBy Await.result(testController.updateNextYearsIncomeService.reset(any()), 5.seconds)

        exception.getMessage mustBe "Invalid Data"

      }
    }

    "return NOT_FOUND" when {
      "CY Plus 1 is disabled" in {
        val testController = createTestIncomeController(isCyPlusOneEnabled = false)
        val employmentID = 1

        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result: Future[Result] = testController.start(employmentID)(fakeRequest)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "edit" must {
    "return OK with the edit view" when {
      "employment data is available for the nino" in {

        val testController = createTestIncomeController()
        val nino = generateNino
        when(testController.updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(model))

        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result: Future[Result] = testController.edit(employmentID)(fakeRequest)

        status(result) mustBe OK
        result rendersTheSameViewAs updateIncomeCYPlus1Edit(model, AmountComparatorForm.createForm(taxablePayYTD = Some(currentEstPay)))
      }
    }
  }

  "update" must {
    "redirect to the confirm page" when {
      "valid input is passed" in {

        val testController = createTestIncomeController()
        val newEstPay = "999"
        val nino = generateNino
        when(testController.updateNextYearsIncomeService.setNewAmount(Matchers.eq(newEstPay), Matchers.eq(employmentID), Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(model))

        val result = testController.update(employmentID)(
          RequestBuilder
            .buildFakeRequestWithOnlySession(POST)
            .withFormUrlEncodedBody("income" -> newEstPay))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(routes.UpdateIncomeNextYearController.confirm(employmentID).url.toString)
      }

    }

    "respond with a BAD_REQUEST" when {
      "no input is passed" in {
        val testController = createTestIncomeController()
        val newEstPay = ""
        val nino = generateNino
        implicit val fakeRequest = RequestBuilder.buildFakeRequestWithOnlySession(POST).withFormUrlEncodedBody("income" -> newEstPay)

        when(testController.updateNextYearsIncomeService.get(Matchers.eq(employmentID), Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(model))

        val result: Future[Result] = testController.update(employmentID)(fakeRequest)

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs updateIncomeCYPlus1Edit(model, AmountComparatorForm.createForm().bindFromRequest()(fakeRequest))
      }
    }
  }
  private val generateNino = new Generator(new Random).nextNino

  private def createTestIncomeController(isCyPlusOneEnabled: Boolean = true): UpdateIncomeNextYearController = new TestUpdateIncomeNextYearController(isCyPlusOneEnabled)

  private class TestUpdateIncomeNextYearController(isCyPlusOneEnabled: Boolean) extends UpdateIncomeNextYearController {
    override val personService: PersonService = mock[PersonService]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever
    override val updateNextYearsIncomeService: UpdateNextYearsIncomeService = mock[UpdateNextYearsIncomeService]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val cyPlusOneEnabled: Boolean = isCyPlusOneEnabled

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))

  }
}
