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

package controllers

import TestConnectors.FakeAuthConnector
import builders.{RequestBuilder, UserBuilder}
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.service.{ActivityLoggerService, TaiService}

import scala.concurrent.Future

class IncomeControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with I18nSupport {

  implicit val hc = HeaderCarrier()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val fakeRequest1 = FakeRequest("POST", "").withFormUrlEncodedBody(
    "name" -> "test1", "description" -> "description",
    "employmentId" -> "14",
    "newAmount" -> "1675",
    "oldAmount" -> "11",
    "worksNumber" -> "",
    "startDate" -> "2013-08-03",
    "endDate" -> "",
    "isLive" -> "true",
    "isOccupationalPension" -> "false",
    "hasMultipleIncomes" -> "true")

  "Edit Incomes" should {

    "throw validation if less than zero" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary

      val employments = testTaxSummary.increasesTax.flatMap(_.incomes.flatMap(_.taxCodeIncomes.employments))
      employments.isDefined mustBe true
      val employment = employments.get.taxCodeIncomes(0)

      val SUT = new TestIncomeController()
      val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody(
        "version" -> "1",
        "newAmounts[0].name" -> "test1",
        "newAmounts[0].employmentId" -> employment.employmentId.get.toString,
        "newAmounts[0].newAmount" -> "-100")

      val result = SUT.updateIncomesForNino(nino)(fakeRequest, UserBuilder.apply(),createMockSessionData(testTaxSummary))
      status(result) mustBe 400
    }

    "throw validation if amount entered is more than 9 digits" in {
      val testTaxSummary = TaiData.getEditableCeasedAndIncomeTaxSummary
      val employments = testTaxSummary.increasesTax.flatMap(_.incomes.flatMap(_.taxCodeIncomes.employments))
      employments.isDefined mustBe true

      val SUT = new TestIncomeController()
      val fakeRequest = FakeRequest("POST", "").withFormUrlEncodedBody(
        "name" -> "test1", "description" -> "description",
        "employmentId" -> "14",
        "newAmount" -> "1000000000000000",
        "oldAmount" -> "11",
        "worksNumber" -> "",
        "startDate" -> "2013-08-03",
        "endDate" -> "",
        "isLive" -> "true",
        "isOccupationalPension" -> "false",
        "hasMultipleIncomes" -> "true")

      val result = SUT.updateIncomesForNino(nino)(fakeRequest, UserBuilder.apply(), createMockSessionData(testTaxSummary))
      status(result) mustBe 400
    }

  }

  "Update Income flow" should {

    val singleIncomeRequest = FakeRequest("POST", "").withFormUrlEncodedBody(
      "name" -> "test1", "description" -> "description",
      "employmentId" -> "14",
      "newAmount" -> "1675",
      "oldAmount" -> "11",
      "worksNumber" -> "",
      "startDate" -> "2013-08-03",
      "endDate" -> "",
      "isLive" -> "true",
      "isOccupationalPension" -> "false",
      "hasMultipleIncomes" -> "false")

    "show the correct step for confirmation page for income" in {
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val employments = testTaxSummary.increasesTax.flatMap(_.incomes.flatMap(_.taxCodeIncomes.employments))
      employments.isDefined mustBe true

      val SUT = createSUTwithProgrammedDeps()

      val result = SUT.updateIncomesForNino(nino)(singleIncomeRequest, UserBuilder.apply(),createMockSessionData(testTaxSummary))
      status(result) mustBe 200
    }

    "show the correct step for confirmation page for multiple-incomes" in {
      val testTaxSummary = TaiData.getIncomesAndPensionsTaxSummary
      val employments = testTaxSummary.increasesTax.flatMap(_.incomes.flatMap(_.taxCodeIncomes.employments))
      employments.isDefined mustBe true

      val SUT = createSUTwithProgrammedDeps()

      val result = SUT.updateIncomesForNino(nino)(fakeRequest1, UserBuilder.apply(),createMockSessionData(testTaxSummary))
      status(result) mustBe 200
    }


    "should be allowed to call updateIncomes() with an authorised session  " in {
      val testTaxSummary = TaiData.getIncomesAndPensionsTaxSummary
      val SUT = createSUTwithProgrammedDeps(sessionData = createMockSessionData(testTaxSummary))
      val result = SUT.updateIncomes()(RequestBuilder.buildFakeRequestWithAuth("POST"))
      status(result) mustBe 200
    }

    "should be allowed to call updateIncomes() with an unAuthorised session and should be directed to gg login " in {
      val testTaxSummary = TaiData.getIncomesAndPensionsTaxSummary
      val SUT = createSUTwithProgrammedDeps(sessionData = createMockSessionData(testTaxSummary))
      val result = SUT.updateIncomes()(RequestBuilder.buildFakeRequestWithoutAuth("POST"))

      status(result) mustBe 303
      val nextURL = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => "" +
          ""
      }
      nextURL.contains("/gg/sign-in") mustBe true
    }


    "call updateIncomes() successfully with an authorised session  " in {
      val testTaxSummary = TaiData.getIncomesAndPensionsTaxSummary
      val SUT = createSUTwithProgrammedDeps(sessionData = createMockSessionData(testTaxSummary))
      val result = SUT.updateIncomes()(RequestBuilder.buildFakeRequestWithAuth("POST"))
      status(result) mustBe 200
    }

    "call updateIncomes() unsuccessfully with an unAuthorised session and be directed to the gg login  " in {
      val testTaxSummary = TaiData.getIncomesAndPensionsTaxSummary
      val SUT = createSUTwithProgrammedDeps(sessionData = createMockSessionData(testTaxSummary))
      val result = SUT.updateIncomes()(RequestBuilder.buildFakeRequestWithoutAuth("POST"))

      status(result) mustBe 303
      val nextURL = redirectLocation(result) match {
        case Some(s: String) => s
        case _ => "" +
          ""
      }
      nextURL.contains("/gg/sign-in") mustBe true
    }

    "create editIncomeForm with errors having less new amount  " in {
      val paymentDate = None
      val pensionYTD = 1700
      implicit val request = RequestBuilder.buildFakeRequestWithAuth("POST")
      val testForm = EditIncomeForm.bind(pensionYTD, paymentDate, Some("error.tai.updateDataPension.enterLargerValue"))
      testForm.fold(formWithErrors=>true, income=>false) mustBe true
    }

    "create editIncomeForm with no errors having large new amount  " in {
      val paymentDate = None
      val pensionYTD = 10
      implicit val request = RequestBuilder.buildFakeRequestWithAuth("POST")
      val testForm = EditIncomeForm.bind(pensionYTD, paymentDate, Some("error.tai.updateDataPension.enterLargerValue"))
      testForm.fold(formWithErrors=>true, income=>false) mustBe false
    }

    "create editIncomeForm with no errors having same new amount  " in {
      val paymentDate = None
      val pensionYTD = 1675
      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("POST")
      val testForm = EditIncomeForm.bind(pensionYTD, paymentDate, Some("error.tai.updateDataPension.enterLargerValue"))
      testForm.fold(formWithErrors=>true, income=>false) mustBe false
    }
  }

  val nino = new Generator().nextNino

  def createSUTwithProgrammedDeps(authConnector: AuthConnector = FakeAuthConnector,
                                  sessionData: SessionData = mock[SessionData]) = {

    val mockIabdUpdEmpResp = mock[IabdUpdateEmploymentsResponse]
    val mockTaiSvc = mock[TaiService]

    when(mockTaiSvc.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sessionData))
    when(mockTaiSvc.updateIncome(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(mockIabdUpdEmpResp))
    when(mockTaiSvc.updateTaiSession(any())(any())).thenReturn(Future.successful(mock[SessionData]))

    new TestIncomeController(taiSvc = mockTaiSvc, authConn = authConnector)
  }

  class TestIncomeController(taiSvc: TaiService = mock[TaiService],
                             activityLoggerSvc: ActivityLoggerService = mock[ActivityLoggerService],
                             delegationConn: DelegationConnector = mock[DelegationConnector],
                             authConn: AuthConnector = mock[AuthConnector],
                             auditConn: AuditConnector = mock[AuditConnector]) extends IncomeController {

    override def taiService: TaiService = taiSvc
    override def activityLoggerService: ActivityLoggerService = activityLoggerSvc
    override implicit def templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit def partialRetriever: FormPartialRetriever = MockPartialRetriever
    override protected def delegationConnector: DelegationConnector = delegationConn
    override implicit def authConnector: AuthConnector = authConn
    override def auditConnector: AuditConnector = auditConn
  }

  def createMockSessionData(taxSummaryDets: TaxSummaryDetails, editIncomeForm: Option[EditIncomeForm] = Some(mock[EditIncomeForm])): SessionData = {
    val msd = mock[SessionData]
    when(msd.taxSummaryDetailsCY).thenReturn(taxSummaryDets)
    when(msd.taiRoot).thenReturn(None)
    when(msd.nino).thenReturn(taxSummaryDets.nino)
    when(msd.taiRoot).thenReturn(Some(TaiRoot(taxSummaryDets.nino+"A")))
    when(msd.editIncomeForm).thenReturn(editIncomeForm)
    msd
  }

}
