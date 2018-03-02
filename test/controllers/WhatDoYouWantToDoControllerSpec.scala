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


import builders.{AuthBuilder, RequestBuilder}
import data.TaiData
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.model.{SessionData, TaiRoot, TaxSummaryDetails}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random


class WhatDoYouWantToDoControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with I18nSupport with JsoupMatchers{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Calling the What do you want to do page method" must {
    "call whatDoYouWantToDoPage() successfully with an authorised session" when {
      "cy plus one data is available and cy plus one is enabled" in {
        val testController = createSUT(isCyPlusOneEnabled = true)
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))
        when(testController.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() mustBe Messages("tai.whatDoYouWantToDo.heading")
        doc.select("fieldset input").size mustBe 3
      }

    }

    "redirect to GG login" when {
      "user is not authorised" in {
        val testController = createSUT()

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithoutAuth("GET"))
        status(result) mustBe SEE_OTHER

        val nextUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => "" + ""
        }
        nextUrl.contains("/gg/sign-in") mustBe true
      }
    }

    "redirect to mci page" when {
      "mci indicator is true" in {
        val taxSummaryDetails = TaxSummaryDetails(nino = "", version = 1)
        val sessionData = Some(SessionData(nino = nino.nino, taiRoot = Some(TaiRoot(nino.nino, 0, "", "", None, manualCorrespondenceInd = true)),
          taxSummaryDetailsCY = taxSummaryDetails))
        val testController = createSUT(sessionData = sessionData)
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }

        redirectUrl mustBe "/check-income-tax/tax-estimate-unavailable"
      }
    }

    "redirect to deceased page" when {

      "a 500 response is returned from nps tax account call (as part of session data retrieval)" in {
        val testController = createSUT(sessionDataFailure = Some(new InternalServerException("deceased")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe "/check-income-tax/deceased"
      }

      "the deceased indicator is set on the retrieved TaiRoot" in {
        val sessionData = Some(SessionData(
          nino = nino.nino,
          taiRoot = Some(TaiRoot(nino.nino, 0, "", "", None, manualCorrespondenceInd = false, deceasedIndicator = Some(true))),
          taxSummaryDetailsCY = TaxSummaryDetails(nino = nino.nino, version = 1)))

        val testController = createSUT(sessionData = sessionData)
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe "/check-income-tax/deceased"
      }

      "the deceased AND mci indicators are set on the retrived TaiRoot" in {
        val sessionData = Some(SessionData(
          nino = nino.nino,
          taiRoot = Some(TaiRoot(nino.nino, 0, "", "", None, manualCorrespondenceInd = true, deceasedIndicator = Some(true))),
          taxSummaryDetailsCY = TaxSummaryDetails(nino = nino.nino, version = 1)))

        val testController = createSUT(sessionData = sessionData)
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe "/check-income-tax/deceased"
      }
    }

    "return the general 500 error page" when {

      "an internal server error is returned from any HOD call" in {
        val testController = createSUT(sessionDataFailure = Some(new InternalServerException("a remote failure of some kind")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Sorry, we are experiencing technical difficulties - 500"
        doc must haveHeadingWithText(Messages("tai.technical.error.heading"))
        doc must haveParagraphWithText(Messages("tai.technical.error.message"))
      }
    }

    "return a 400 error page" when {

      "a not found exception is returned from the RTI hod call" in {
        val testController = createSUT(sessionDataFailure = Some(new NotFoundException("a no data response from rti")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe NOT_FOUND
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Page not found - 404"
        doc must haveHeadingWithText(Messages("tai.errorMessage.heading"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message1"))
      }

      "a bad request exception is returned from any HOD call" in {
        val testController = createSUT(sessionDataFailure = Some(new BadRequestException("an example bad request response")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Bad request - 400"
        doc must haveHeadingWithText(Messages("tai.errorMessage.heading"))
        doc must haveParagraphWithText(Messages("tai.errorMessage.frontend400.message1"))
      }
    }

    "return the 'you can't use this service page" when {
      "nps tax account hod call has returned a not found exception, indicating no current year data is present, " +
        "and no previous year employment data is present" in {
        val testController = createSUT(
          sessionDataFailure = Some(new NotFoundException(TaiConstants.NpsTaxAccountCYDataAbsentMsg)),
          employmentDataFailure = Some(new NotFoundException("no data found")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        verify(testController.employmentService, times(1)).employments(any(), any())(any())
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Sorry, there is a problem so you can’t use this service"
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem1"))
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem2"))
      }

      "nps tax account hod call has returned a bad request exception, indicating absence of any tax account data whatsoever, " +
        "and no previous year employment data is present" in {
        val testController = createSUT(
          sessionDataFailure = Some(new BadRequestException(TaiConstants.NpsTaxAccountDataAbsentMsg)),
          employmentDataFailure = Some(new NotFoundException("no data found")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        verify(testController.employmentService, times(1)).employments(any(), any())(any())
        redirectLocation(result).get mustBe routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage().url
      }

      "nps tax account hod call has returned a bad request exception, indicating no employments recorded" in {
        val testController = createSUT(
          sessionDataFailure = Some(new BadRequestException(TaiConstants.NpsNoEmploymentsRecorded)),
          employmentDataFailure = Some(new NotFoundException("no data found")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Sorry, there is a problem so you can’t use this service"
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem1"))
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem2"))
      }

      "nps tax account hod call has returned a bad request exception, indicating no employments for current tax year," +
        "and no previous year employment data is present" in {
        val testController = createSUT(
          sessionDataFailure = Some(new BadRequestException(TaiConstants.NpsNoEmploymentForCurrentTaxYear)),
          employmentDataFailure = Some(new NotFoundException("no data found")))
        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe BAD_REQUEST
        verify(testController.employmentService, times(1)).employments(any(), any())(any())
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe "Sorry, there is a problem so you can’t use this service"
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem1"))
        doc must haveListItemWithText(Messages("tai.noPrimary.reasonItem2"))
      }
    }

    "display of the WDYWTD page (not redirect)" when {
      "nps tax account hod call has returned a not found exception, indicating no current year data is present, " +
        "but previous year employment data IS present" in {
        val testController = createSUT(
          sessionDataFailure = Some(new NotFoundException("no tax account information found"))
        )
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))
        when(testController.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        verify(testController.employmentService, times(1)).employments(any(), any())(any())
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe Messages("tai.whatDoYouWantToDo.heading")
      }

      "nps tax account hod call has returned a bad request exception, indicating absence of ANY tax account data, " +
        "but previous year employment data IS present" in {
        val testController = createSUT(
          sessionDataFailure = Some(new BadRequestException("cannot complete a coding calculation without a primary employment"))
        )
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))
        when(testController.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        verify(testController.employmentService, times(1)).employments(any(), any())(any())
        val doc = Jsoup.parse( contentAsString(result) )
        doc.title() mustBe Messages("tai.whatDoYouWantToDo.heading")
      }

      "cy plus one data is not available and cy plus one is enabled" in {
        val testController = createSUT(isCyPlusOneEnabled = true)
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))
        when(testController.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiNotFoundResponse("Not found")
        ))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() mustBe Messages("tai.whatDoYouWantToDo.heading")
        doc.select("fieldset input").size mustBe 2
      }

      "cy plus one data is available and cy plus one is disabled" in {
        val testController = createSUT(isCyPlusOneEnabled = false)
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() mustBe Messages("tai.whatDoYouWantToDo.heading")
        doc.select("fieldset input").size mustBe 2
      }

      "cy plus one data is not available and cy plus one is disabled" in {
        val testController = createSUT(isCyPlusOneEnabled = false)
        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))

        val result = testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK

        doc.title() mustBe Messages("tai.whatDoYouWantToDo.heading")
        doc.select("fieldset input").size mustBe 2
      }
    }
  }

  "send an user entry audit event" when {
    "landed to the page" in {
      val testController = createSUT()
      when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))
      when(testController.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
        TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
      )

      val result = Await.result(testController.whatDoYouWantToDoPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5.seconds)

      result.header.status mustBe OK

      verify(testController.auditService, times(1)).sendUserEntryAuditEvent(any(), any())(any())
    }
  }

  "Calling the handleWhatDoYouWantToDoPage method" must {
    "redirect to the current year information url" when {
      "the form has the value currentTaxYear in taxYears" in {
        val testController = createSUT()
        val request = FakeRequest("POST", "").withFormUrlEncodedBody("taxYears" -> "currentTaxYear").withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )

        val result = testController.handleWhatDoYouWantToDoPage()(request)

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }
    }

    "redirect to previous year url" when {
      "the form has the value previousTaxYear in taxYears" in {
        val testController = createSUT()
        val request = RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxYears" -> "lastTaxYear")

        val result = testController.handleWhatDoYouWantToDoPage()(request)

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl mustBe routes.PayeControllerHistoric.payePage(TaxYear(TaxYearResolver.currentTaxYear-1)).url
      }
    }

    "redirect to next year comparison url" when {
      "when CY+1 is enabled" in {
        val testController = createSUT()
        val request = RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("taxYears" -> "nextTaxYear")

        val result = testController.handleWhatDoYouWantToDoPage()(request)

        status(result) mustBe SEE_OTHER

        redirectLocation(result).get mustBe routes.IncomeTaxComparisonController.onPageLoad().url
      }
    }

    "render the what do you want to do page with form errors" when {
      "no value is present in taxYears and cy plus data is available" in {
        val testController = createSUT(isCyPlusOneEnabled = true)

        val request = FakeRequest("POST", "").withFormUrlEncodedBody("taxYears" -> "").withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )

        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))
        when(testController.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary))
        )

        val result = testController.handleWhatDoYouWantToDoPage()(request)

        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        doc.select("fieldset input").size mustBe 3
      }

      "no value is present in taxYears and cy plus data is not available" in {
        val testController = createSUT(isCyPlusOneEnabled = true)

        val request = FakeRequest("POST", "").withFormUrlEncodedBody("taxYears" -> "").withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )

        when(testController.trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(false))
        when(testController.taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(
          TaiNotFoundResponse("Not found")
        ))

        val result = testController.handleWhatDoYouWantToDoPage()(request)

        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        doc.select("fieldset input").size mustBe 2
      }
    }
  }

  "employments retrieval for CY-1" should {

    "supply employment data where data is found" in {
      implicit val hc = HeaderCarrier()
      val testController = createSUT()
      val employments = Await.result(testController.previousYearEmployments.apply(nino), 5 seconds)
      employments mustBe fakeEmploymentData
    }

    "supply an empty list in the event of a downstream failure" in {
      implicit val hc = HeaderCarrier()
      val testController = createSUT(employmentDataFailure = Some(new NotFoundException("no data found")))
      val employments = Await.result(testController.previousYearEmployments.apply(nino), 5 seconds)
      employments mustBe Nil
    }
  }

  private def createFakeSessionDataWithPY: SessionData = AuthBuilder.createFakeSessionDataWithPY

  private val fakeEmploymentData = Seq(Employment("TEST", Some("12345"), LocalDate.now(), None,
    List(AnnualAccount("", TaxYear(TaxYearResolver.currentTaxYear), Available, Nil, Nil)), "", "", 2),
    Employment("TEST1", Some("123456"), LocalDate.now(), None,
      List(AnnualAccount("", TaxYear(TaxYearResolver.currentTaxYear), Unavailable, Nil, Nil)), "", "", 2))

  private val nino = new Generator(new Random).nextNino

  private val taxAccountSummary = TaxAccountSummary(111,222, 333)

  private def createSUT(sessionData: Option[SessionData] = None,
                        sessionDataFailure: Option[Throwable] = None,
                        employmentData: Option[Seq[Employment]] = None,
                        employmentDataFailure: Option[Throwable] = None,
                        isCyPlusOneEnabled: Boolean = true) =
    new WhatDoYouWantToDoControllerTest(sessionData, sessionDataFailure, employmentData, employmentDataFailure, isCyPlusOneEnabled)

  class WhatDoYouWantToDoControllerTest(sessionData: Option[SessionData],
                                        sessionDataFailure: Option[Throwable],
                                        employmentData: Option[Seq[Employment]],
                                        employmentDataFailure: Option[Throwable],
                                        isCyPlusOneEnabled: Boolean = true) extends WhatDoYouWantToDoController {
    override val taiService: TaiService = mock[TaiService]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override val employmentService: EmploymentService = mock[EmploymentService]
    override implicit val partialRetriever: PartialRetriever = mock[PartialRetriever]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditService: AuditService = mock[AuditService]
    override val trackingService: TrackingService = mock[TrackingService]
    override val cyPlusOneEnabled: Boolean = isCyPlusOneEnabled
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]


    val ad: Future[Some[Authority]] = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    val sd: SessionData = sessionData.getOrElse(createFakeSessionDataWithPY)
    sessionDataFailure match {
      case None => when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sd))
      case Some(throwable) => when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.failed(throwable))
    }
    val ed: Seq[Employment] = employmentData.getOrElse(fakeEmploymentData)
    employmentDataFailure match {
      case None => when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(ed))
      case Some(throwable) => when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(throwable))
    }
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(sd.taiRoot.get))
    when(auditService.sendUserEntryAuditEvent(any(), any())(any())).thenReturn(Future.successful(AuditResult.Success))
  }
}
