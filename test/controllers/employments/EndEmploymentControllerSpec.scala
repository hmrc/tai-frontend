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

package controllers.employments

import builders.{AuthBuilder, RequestBuilder}
import controllers.FakeTaiPlayApplication
import data.TaiData
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms.employments.EmploymentEndDateForm
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.model.{SessionData, TaiRoot, TaxSummaryDetails}
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, JourneyCacheService, TaiService}
import uk.gov.hmrc.tai.util._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EndEmploymentControllerSpec
  extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JourneyCacheConstants
  with FormValuesConstants
  with DatePatternConstants
  with IrregularPayConstants
  with EmploymentDecisionConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "employmentUpdateRemove" must {
    "call updateRemoveEmployer page successfully with an authorised session" in {
      val sut = createSUT

      val result = sut.employmentUpdateRemove(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() mustBe Messages("tai.employment.decision.title", employerName)
    }

    "call the Employment service to get the correct employment details" in {
      val sut = createSUT
      Await.result(sut.employmentUpdateRemove(1)(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
      verify(sut.employmentService, times(1)).employment(any(), any())(any())
    }

    "redirect to GG login" when {
      "user is not authorised" in {
        val sut = createSUT
        val result = sut.employmentUpdateRemove(1)(RequestBuilder.buildFakeRequestWithoutAuth("GET"))
        status(result) mustBe 303

        val nextUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => "" + ""
        }
        nextUrl.contains("/gg/sign-in") mustBe true
      }
    }
  }

  "handleEmploymentUpdateRemove" must {
    "redirect to the update employment url" when {
      "the form has the value Yes in EmploymentDecision" in {
        val sut = createSUT

        val request = FakeRequest("POST", "").withFormUrlEncodedBody(EmploymentDecision -> YesValue).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )

        val result = sut.handleEmploymentUpdateRemove(1)(request)

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }
        redirectUrl mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(1).url
      }
    }

    "redirect to the update within 6 week error page" when {
      "the form has the value No in EmploymentDecision and the employment has a payment within 6 weeks of todays date" in {

        val sut = createSUT

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        when(sut.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val request = RequestBuilder.buildFakeRequestWithAuth("GET")
          .withFormUrlEncodedBody(EmploymentDecision -> NoValue)

        val result = sut.handleEmploymentUpdateRemove(1)(request)

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => ""
        }

        redirectUrl mustBe controllers.employments.routes.EndEmploymentController.endEmploymentError().url
      }

      "the form has the value No in EmploymentDecision and the employment has a payment 6 weeks of todays date" in {

        val sut = createSUT

        val payment = paymentOnDate(LocalDate.now().minusWeeks(6))
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        when(sut.employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))

        val request = RequestBuilder.buildFakeRequestWithAuth("GET")
          .withFormUrlEncodedBody(EmploymentDecision -> NoValue)

        val result = sut.handleEmploymentUpdateRemove(1)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController.endEmploymentError().url
      }

      "cache the employment details for error page" in {
        val sut = createSUT

        val date = LocalDate.now().minusWeeks(6)

        val payment = paymentOnDate(date)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val dataToCache = Map(sut.EndEmployment_LatestPaymentDateKey -> date.toString,
          sut.EndEmployment_NameKey -> "employer name")

        when(sut.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(dataToCache))

        val request = RequestBuilder.buildFakeRequestWithAuth("GET")
          .withFormUrlEncodedBody(EmploymentDecision -> NoValue)

        Await.result(sut.handleEmploymentUpdateRemove(1)(request), 5 seconds)
        verify(sut.journeyCacheService, times(1)).cache(any())(any())
      }
    }

    "redirect to the irregular payment error page" when {
      "the form has the value No in EmploymentDecision and the employment has a irregular payment" in {

        val sut = createSUT

        val request = RequestBuilder.buildFakeRequestWithAuth("GET")
          .withFormUrlEncodedBody(EmploymentDecision -> NoValue)

        val payment = paymentOnDate(LocalDate.now().minusWeeks(8)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount("", TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        when(sut.employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(sut.auditService.createAndSendAuditEvent(any(), any())(any(), any())).thenReturn(Future.successful(Success))

        val result = sut.handleEmploymentUpdateRemove(1)(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController.irregularPaymentError(1).url
        }
    }

    "render the what do you want to do page with form errors" when {
      "no value is present in EmploymentDecision" in {
        val sut = createSUT

        val request = FakeRequest("POST", "").withFormUrlEncodedBody(EmploymentDecision -> "").withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )

        val result = sut.handleEmploymentUpdateRemove(1)(request)

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to GG login" when {
      "user is not authorised" in {
        val sut = createSUT
        val result = sut.handleEmploymentUpdateRemove(1)(RequestBuilder.buildFakeRequestWithoutAuth("POST"))
        status(result) mustBe 303

        val nextUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => "" + ""
        }
        nextUrl.contains("/gg/sign-in") mustBe true
      }
    }
  }

  "tell us about employment error page" must {
    "show the error page" in {
      val sut = createSUT

      val dataFromCache = Seq(new LocalDate().minusWeeks(6).minusDays(1).toString, employerName, "1")

      when(sut.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(dataFromCache))

      val result = sut.endEmploymentError()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() mustBe Messages("tai.endEmploymentWithinSixWeeksError.heading", new LocalDate().toString("d MMMM yyyy"))
    }

    "show the irregular payment error page" in {
      val sut = createSUT

      when(sut.journeyCacheService.mandatoryValue(any())(any())).thenReturn(Future.successful("Employer Name"))

      val result = sut.irregularPaymentError(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() mustBe Messages("tai.irregular.preHeadingText")
    }

    "submit the details to backend" in {
      val sut = createSUT
      val dataFromCache = (Seq("0", new LocalDate(2017, 2, 1).toString,
        "Yes"), Seq(Some("EXT-TEST")))

      when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(dataFromCache))
      when(sut.employmentService.endEmployment(any(), any(), any())(any())).thenReturn(Future.successful("123-456-789"))
      when(sut.successfulJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_EndEmploymentKey), Matchers.eq("true"))(any())).
        thenReturn(Future.successful(Map(TrackSuccessfulJourney_EndEmploymentKey -> "true")))
      when(sut.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = sut.confirmAndSendEndEmployment()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.EndEmploymentController.showConfirmationPage().url
    }
  }

  "tellUsAboutEmploymentPage" must {
    "call whatDoYouWantToDoPage() successfully with an authorised session" in {
      val sut = createSUT
      val result = sut.endEmploymentPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() mustBe Messages("tai.tellUsAboutEmployment.title")
    }

    "call the Employment service to get the correct employment details" in {
      val sut = createSUT
      Await.result(sut.endEmploymentPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
      verify(sut.employmentService, times(1)).employment(any(), any())(any())
    }

    "redirect to GG login" when {
      "user is not authorised" in {
        val sut = createSUT
        val result = sut.endEmploymentPage(1)(RequestBuilder.buildFakeRequestWithoutAuth("GET"))
        status(result) mustBe 303

        val nextUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _ => "" + ""
        }
          nextUrl.contains("/gg/sign-in") mustBe true
      }
    }
  }

  "Calling the handleTellUsAboutEmploymentPage method" must {
    "call processTellUsAboutEmploymentPage successfully with an authorised session" in {
      val sut = createSUT

      val formData = Json.obj(
        sut.employmentEndDateForm.EmploymentFormDay -> "01",
        sut.employmentEndDateForm.EmploymentFormMonth -> "02",
        sut.employmentEndDateForm.EmploymentFormYear -> "2017"
      )

      val request = FakeRequest("POST", "")
        .withJsonBody(formData)
        .withSession(SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority")

      val result = sut.handleEndEmploymentPage(0)(request)

      status(result) mustBe SEE_OTHER
    }

    "reload the page when there are form errors" in {
      val sut = createSUT
      val formWithErrors = Json.obj(
        sut.employmentEndDateForm.EmploymentFormDay -> "01",
        sut.employmentEndDateForm.EmploymentFormMonth -> "02",
        sut.employmentEndDateForm.EmploymentFormYear -> "abc"
      )

      val request = FakeRequest("POST", "/")
        .withJsonBody(formWithErrors)
        .withSession(SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority")

      val result = sut.handleEndEmploymentPage(0)(request)

      status(result) mustBe BAD_REQUEST
    }

    "redirect to telephone page" in {
      val sut = createSUT
      val dataToCache = Map(EndEmployment_EmploymentIdKey -> "0",
        EndEmployment_NameKey -> employerName,
        EndEmployment_EndDateKey -> new LocalDate(2017, 2, 1).toString)

      when(sut.journeyCacheService.cache(Matchers.eq(dataToCache))(any())).thenReturn(Future.successful(dataToCache))

      val formData = Json.obj(
        sut.employmentEndDateForm.EmploymentFormDay -> "01",
        sut.employmentEndDateForm.EmploymentFormMonth -> "02",
        sut.employmentEndDateForm.EmploymentFormYear -> "2017"
      )

      val request = FakeRequest("POST", "")
        .withJsonBody(formData)
        .withSession(SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority")

      val result = sut.handleEndEmploymentPage(0)(request)

      status(result) mustBe SEE_OTHER
    }

    "save data into journey cache" in {
      val sut = createSUT
      val dataToCache = Map(EndEmployment_EmploymentIdKey -> "0",
        EndEmployment_NameKey -> employerName,
        EndEmployment_EndDateKey -> new LocalDate(2017, 2, 1).toString)

      when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(dataToCache))

      val formData = Json.obj(
        sut.employmentEndDateForm.EmploymentFormDay -> "01",
        sut.employmentEndDateForm.EmploymentFormMonth -> "02",
        sut.employmentEndDateForm.EmploymentFormYear -> "2017"
      )

      val request = FakeRequest("POST", "")
        .withJsonBody(formData)
        .withSession(SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority")

      Await.result(sut.handleEndEmploymentPage(0)(request), 5 seconds)
      verify(sut.journeyCacheService, times(1)).cache(Matchers.eq(dataToCache))(any())
    }


    "check your answers page" must {
      "show the check your answers page" in {
        val sut = createSUT

        val dataFromCache = (Seq("0", new LocalDate(2017, 2, 1).toString, "No"), Seq(Some("EXT-TEST")))

        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(dataFromCache))

        val result = sut.endEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() mustBe Messages("tai.endEmploymentConfirmAndSend.heading")
      }

      "submit the details to backend" in {
        val sut = createSUT
        val dataFromCache = (Seq("0", new LocalDate(2017, 2, 1).toString,
          "Yes"), Seq(Some("EXT-TEST")))

        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(dataFromCache))
        when(sut.employmentService.endEmployment(any(), any(), any())(any())).thenReturn(Future.successful("123-456-789"))
        when(sut.successfulJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_EndEmploymentKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_EndEmploymentKey -> "true")))
        when(sut.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = sut.confirmAndSendEndEmployment()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.showConfirmationPage().url
        verify(sut.journeyCacheService, times(1)).flush()(any())
      }
    }

    "showConfirmationPage" must {
      "show confirmation view" in {
        val sut = createSUT

        val result = sut.showConfirmationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() mustBe Messages("tai.employmentConfirmation.heading")
      }
    }
  }

  "add telephone number" must {
    "show the contact by telephone page" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValueAsInt(Matchers.eq(EndEmployment_EmploymentIdKey))(any())).thenReturn(Future.successful(0))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.canWeContactByPhone.title")
      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT

        val expectedCache = Map(EndEmployment_TelephoneQuestionKey -> YesValue, EndEmployment_TelephoneNumberKey -> "12345678")
        when(sut.journeyCacheService.cache(Matchers.eq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))
        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber = Map(EndEmployment_TelephoneQuestionKey -> NoValue, EndEmployment_TelephoneNumberKey -> "")
        when(sut.journeyCacheService.cache(Matchers.eq(expectedCacheWithErasingNumber))(any())).thenReturn(Future.successful(expectedCacheWithErasingNumber))
        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> NoValue, YesNoTextEntry -> "this value must not be cached"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT

        val empId = 1

        when(sut.journeyCacheService.mandatoryValueAsInt(any())(any())).thenReturn(Future.successful(empId))

        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.canWeContactByPhone.title")
      }

      "there is a form validation error (additional, controller specific constraint)" in {
        val sut = createSUT

        val empId = 1

        when(sut.journeyCacheService.mandatoryValueAsInt(any())(any())).thenReturn(Future.successful(empId))

        val tooFewCharsResult = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234"))
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() mustBe Messages("tai.canWeContactByPhone.title")

        val tooManyCharsResult = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234123412341234123412341234123"))
        status(tooManyCharsResult) mustBe BAD_REQUEST
        val tooManyDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooManyDoc.title() mustBe Messages("tai.canWeContactByPhone.title")
      }
    }
  }

  "handleIrregularPay" must {
    "return bad request" when {
      "there are errors in form" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValue(Matchers.eq(EndEmployment_NameKey))(any())).thenReturn(Future.successful("Employer"))

        val result = sut.handleIrregularPaymentError(1)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody())

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to income summary view" when {
      "user selected an option to contact the employer" in {
        val sut = createSUT

        val result = sut.handleIrregularPaymentError(1)(RequestBuilder.
          buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(IrregularPayDecision -> ContactEmployer))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad.url
      }
    }

    "redirect to end employment journey" when {
      "user selected an option to update the details" in {
        val sut = createSUT

        val result = sut.handleIrregularPaymentError(1)(RequestBuilder.
          buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(IrregularPayDecision -> UpdateDetails))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController.endEmploymentPage(1).url
      }
    }
  }

  def employmentWithAccounts(accounts:List[AnnualAccount]) = Employment("employer", Some("emp123"), new LocalDate(2000, 5, 20),
    None, accounts, "", "", 8, None, false)

  def paymentOnDate(date: LocalDate) = Payment(
    date = date,
    amountYearToDate = 2000,
    taxAmountYearToDate = 200,
    nationalInsuranceAmountYearToDate = 100,
    amount = 1000,
    taxAmount = 100,
    nationalInsuranceAmount = 50,
    payFrequency = Monthly)

  private def createSUT = new SUT

  private class SUT extends EndEmploymentController {

    override implicit def templateRenderer = MockTemplateRenderer
    override val taiService: TaiService = mock[TaiService]
    override val auditService: AuditService = mock[AuditService]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override implicit val partialRetriever: PartialRetriever = mock[PartialRetriever]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

    val employmentEndDateForm = EmploymentEndDateForm("employer")

    def generateNino: Nino = new Generator().nextNino
    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(TaiRoot("", 1, "", "", None, "", "", manualCorrespondenceInd = false, deceasedIndicator = None)))

    when(employmentService.employment(any(), any())(any()))
      .thenReturn(Future.successful(Some(Employment(employerName, None, new LocalDate(), None, Nil, "", "", 1, None, false))))

    when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
  }

  private val employerName = "employer name"
}
