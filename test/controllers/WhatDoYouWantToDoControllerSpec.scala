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

package controllers

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.factories.TaxCodeMismatchFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random


class WhatDoYouWantToDoControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JsoupMatchers
  with BeforeAndAfterEach {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeEach: Unit = {
    Mockito.reset(auditService, employmentService)
  }

  "Calling the What do you want to do page method" must {
    "call whatDoYouWantToDoPage() successfully with an authorised session" when {

      "cy plus one data is available and cy plus one is enabled" in {

        val controller = createTestController(isCyPlusOneEnabled = true)

        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = controller.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "there has not been a tax code change" in {
        val testController = createTestController(isCyPlusOneEnabled = true)

        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        doc.body().toString mustNot include(Messages("check.tax.hasChanged.header"))
      }

      "there has been a tax code change and cyPlusOne is enabled" in {
        val testController = createTestController(isCyPlusOneEnabled = true)

        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeChanged))
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        doc.body().toString must include(Messages("check.tax.hasChanged.header"))
        doc.select(".card").size mustBe 4
      }

      "cyPlusOne is disabled" in {
        val testController = createTestController(isCyPlusOneEnabled = false)

        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeChanged))
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
        doc.body().toString must include(Messages("check.tax.hasChanged.header"))
        doc.select(".card").size mustBe 3
      }
    }

    "return the general 500 error page" when {

      "an internal server error is returned from any HOD call" in {
        val testController = createTestController()
        when(employmentService.employments(any(), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.failed((new InternalServerException("something bad"))))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, we are experiencing technical difficulties - 500")
        doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
        doc must haveParagraphWithText(Messages("tai.technical.error.message"))
      }
    }

    "return a 400 error page" when {

      "a general bad request exception is returned from any HOD call" in {
        val testController = createTestController()
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(new BadRequestException("bad request")))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Bad request - 400")
        doc must haveHeadingWithText(Messages("tai.errorMessage.heading"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message1"))
      }
    }

    "return the 'you can't use this service page'" when {
      "nps tax account hod call has returned a not found exception ('no tax account information found'), indicating no current year data is present, " +
        "and no previous year employment data is present" in {
        val testController = createTestController()
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse(TaiConstants.NpsTaxAccountCYDataAbsentMsg)))
        when(employmentService.employments(any(), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))
        when(employmentService.employments(any(), Matchers.eq(TaxYear().prev))(any()))
          .thenReturn(Future.failed((new NotFoundException("no data found"))))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        verify(employmentService, times(1)).employments(any(), Matchers.eq(TaxYear().prev))(any())
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, there is a problem so you cannot use this service")
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem1"))
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem2"))
      }

      "nps tax account hod call has returned a bad request exception, indicating absence of any tax account data whatsoever, " +
        "and no previous year employment data is present" in {
        val testController = createTestController()
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse(TaiConstants.NpsTaxAccountDataAbsentMsg)))
        when(employmentService.employments(any(), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))
        when(employmentService.employments(any(), Matchers.eq(TaxYear().prev))(any()))
          .thenReturn(Future.failed((new NotFoundException("no data found"))))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        verify(employmentService, times(1)).employments(any(), Matchers.eq(TaxYear().prev))(any())
        redirectLocation(result).get mustBe routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url
      }

      "nps tax account hod call has returned a bad request exception, indicating no employments recorded" in {
        val testController = createTestController()
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse(TaiConstants.NpsNoEmploymentsRecorded)))
        when(employmentService.employments(any(), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, there is a problem so you cannot use this service")
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem1"))
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem2"))
      }

      "nps tax account hod call has returned a bad request exception, indicating no employments for current tax year," +
        "and no previous year employment data is present" in {
        val testController = createTestController()
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse(TaiConstants.NpsNoEmploymentForCurrentTaxYear)))
        when(employmentService.employments(any(), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))
        when(employmentService.employments(any(), Matchers.eq(TaxYear().prev))(any()))
          .thenReturn(Future.failed(new NotFoundException("no data found")))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        verify(employmentService, times(1)).employments(any(), Matchers.eq(TaxYear().prev))(any())
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include("Sorry, there is a problem so you cannot use this service")
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem1"))
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem2"))
      }
    }

    "display the WDYWTD page (not redirect)" when {
      "nps tax account hod call has returned a not found exception, indicating no current year data is present, " +
        "but previous year employment data IS present" in {

        val testController = createTestController()

        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse(TaiConstants.NpsTaxAccountCYDataAbsentMsg)))
        when(employmentService.employments(any(), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))
        when(employmentService.employments(any(), Matchers.eq(TaxYear().prev))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        verify(employmentService, times(1)).employments(any(), Matchers.eq(TaxYear().prev))(any())
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "nps tax account hod call has returned a bad request exception, indicating absence of ANY tax account data, " +
        "but previous year employment data IS present" in {
        val testController = createTestController()

        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse(TaiConstants.NpsTaxAccountDataAbsentMsg)))
        when(employmentService.employments(any(), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))
        when(employmentService.employments(any(), Matchers.eq(TaxYear().prev))(any()))
          .thenReturn(Future.successful(fakeEmploymentData))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        verify(employmentService, times(1)).employments(any(), Matchers.eq(TaxYear().prev))(any())
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "cy plus one data is not available and cy plus one is enabled" in {
        val testController = createTestController(isCyPlusOneEnabled = true)

        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiNotFoundResponse("Not found")
        ))
        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "cy plus one data is available and cy plus one is disabled" in {
        val testController = createTestController(isCyPlusOneEnabled = false)

        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }

      "cy plus one data is not available and cy plus one is disabled" in {
        val testController = createTestController(isCyPlusOneEnabled = false)

        when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() must include(Messages("your.paye.income.tax.overview"))
      }
    }
  }

  "send an user entry audit event" when {
    "landed to the page and get TaiSuccessResponseWithPayload" in {
      val testController = createTestController()

      when(taxAccountService.taxCodeIncomes(any(), any())(any())).
        thenReturn(Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](Seq.empty[TaxCodeIncome])))
      when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))

      val result = Await.result(testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

      result.header.status mustBe OK

      verify(auditService, times(1)).sendUserEntryAuditEvent(any(), any(), any(), any())(any())
    }
    "landed to the page and get TaiSuccessResponse" in {
      val testController = createTestController()

      when(taxAccountService.taxCodeIncomes(any(), any())(any())).
        thenReturn(Future.successful(TaiSuccessResponse))
      when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))


      val result = Await.result(testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

      result.header.status mustBe OK

      verify(auditService, times(1)).sendUserEntryAuditEvent(any(), any(), any(), any())(any())
    }
    "landed to the page and get failure from taxCodeIncomes" in {
      val testController = createTestController()
      val hasTaxCodeChanged = HasTaxCodeChanged(false, Some(TaxCodeMismatchFactory.matchedTaxCode))

      when(taxAccountService.taxCodeIncomes(any(), any())(any())).
        thenReturn(Future.failed(new BadRequestException("bad request")))
      when(taxCodeChangeService.hasTaxCodeChanged(any())(any())).thenReturn(Future.successful(taxCodeNotChanged))


      val result = Await.result(testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

      result.header.status mustBe OK

    }
  }

  "employments retrieval for CY-1" must {

    "supply employment data where data is found" in {
      implicit val hc = HeaderCarrier()
      val testController = createTestController()
      val employments = Await.result(testController.previousYearEmployments(nino), 5 seconds)
      employments mustBe fakeEmploymentData
    }

    "supply an empty list in the event of a downstream failure" in {
      implicit val hc = HeaderCarrier()
      val testController = createTestController()
      when(employmentService.employments(any(), Matchers.eq(TaxYear().prev))(any()))
        .thenReturn(Future.failed((new NotFoundException("no data found"))))
      val employments = Await.result(testController.previousYearEmployments(nino), 5 seconds)
      employments mustBe Nil
    }
  }

  private val fakeEmploymentData = Seq(Employment("TEST", Some("12345"), LocalDate.now(), None,
    List(AnnualAccount("", TaxYear(), Available, Nil, Nil)), "", "", 2, None, false, false),
    Employment("TEST1", Some("123456"), LocalDate.now(), None,
      List(AnnualAccount("", TaxYear(), Unavailable, Nil, Nil)), "", "", 2, None, false, false))

  private val nino = new Generator(new Random).nextNino
  private val taxAccountSummary = TaxAccountSummary(111, 222, 333, 444, 111)
  val taxCodeNotChanged = HasTaxCodeChanged(false, Some(TaxCodeMismatchFactory.matchedTaxCode))
  val taxCodeChanged = HasTaxCodeChanged(true, Some(TaxCodeMismatchFactory.matchedTaxCode))

  private def createTestController(isCyPlusOneEnabled: Boolean = true) = new WhatDoYouWantToDoControllerTest(isCyPlusOneEnabled)

  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val trackingService = mock[TrackingService]
  val auditService = mock[AuditService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]

  class WhatDoYouWantToDoControllerTest(isCyPlusOneEnabled: Boolean = true) extends WhatDoYouWantToDoController(
    employmentService,
    taxCodeChangeService,
    taxAccountService,
    trackingService,
    mock[AuditConnector],
    auditService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    override val cyPlusOneEnabled: Boolean = isCyPlusOneEnabled

    when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(fakeEmploymentData))
    when(auditService.sendUserEntryAuditEvent(any(), any(), any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))
    when(trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(NoTimeToProcess))

    when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
      Future.successful(TaiSuccessResponseWithPayload(taxAccountSummary)))
  }

}
