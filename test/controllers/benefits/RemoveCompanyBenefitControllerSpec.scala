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

package controllers.benefits


import builders.{AuthBuilder, RequestBuilder}
import controllers.FakeTaiPlayApplication
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.benefits.EndedCompanyBenefit
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{AuditService, JourneyCacheService, TaiService}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.util.{DateFormatConstants, FormValuesConstants, JourneyCacheConstants, RemoveCompanyBenefitStopDateConstants}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future
import scala.util.Random

class RemoveCompanyBenefitControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with FormValuesConstants
  with JourneyCacheConstants
  with RemoveCompanyBenefitStopDateConstants
  with DateFormatConstants
  with JsoupMatchers {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "stop date" must {
    "show 'When did you stop getting benefits from company?' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT
        val cache = Map(EndCompanyBenefit_EmploymentNameKey -> "Test",
                        EndCompanyBenefit_BenefitNameKey -> "Test",
                        EndCompanyBenefit_RefererKey -> "Test")

        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = SUT.stopDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.benefits.ended.stopDate.title")

        verify(SUT.journeyCacheService, times(1)).currentCache(any())
      }
    }
  }

  "submit stop date" must {
    "redirect to the 'Can we call you if we need more information?' page" when {
      "the form has the value beforeTaxYearEnd" in {

        val SUT = createSUT
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitStopDate(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(StopDateChoice -> BeforeTaxYearEnd))
        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url

        verify(SUT.journeyCacheService, times(1)).cache(mockEq(Map("stopDate" -> Messages("tai.remove.company.benefit.beforeTaxYearEnd",startOfTaxYear))))(any())
      }
    }

    "redirect to the 'What was the total value of your benefit' page" when {
      "the form has the value onOrAfterTaxYearEnd" in{

        val SUT = createSUT
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitStopDate(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(StopDateChoice -> OnOrAfterTaxYearEnd))
        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url

        verify(SUT.journeyCacheService, times(1)).cache(mockEq(Map("stopDate" -> Messages("tai.remove.company.benefit.onOrAfterTaxYearEnd",startOfTaxYear))))(any())
      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val SUT = createSUT

        when(SUT.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq("EmployerA", "Expenses","Url")))
        val result = SUT.submitStopDate(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(StopDateChoice -> ""))

        status(result) mustBe BAD_REQUEST

        verify(SUT.journeyCacheService, times(1)).mandatoryValues(Matchers.anyVararg())(any())
      }
    }
  }

  "totalValueOfBenefit" must {
    "show what was the total value page" when {
      "the request has an authorised session with employment name and benefit name" in {
        val SUT = createSUT
        val employmentName = "HMRC"
        val benefitName = "Employer Provided Services"
        val referer = "Url"
        when(SUT.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq(employmentName, benefitName, referer)))
        val result = SUT.totalValueOfBenefit()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.remove.company.benefit.total.value.title")
      }
    }
  }

  "submitBenefitValue" must {
    "redirect to the 'Can we contact you' page" when {
      "the form submission is valid" in {
        val SUT = createSUT
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(("totalValue", "1000")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url
      }
    }

    "add total value input to the journey cache with decimal values" when {
      "the form submission is valid" in {
        val SUT = createSUT
        val removeCompanyBenefitFormData = ("totalValue", "1000.00")
        val totalValue = Map("benefitValue" -> "1000")

        when(SUT.journeyCacheService.cache(mockEq(totalValue))(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(removeCompanyBenefitFormData))

        status(result) mustBe SEE_OTHER

      }
    }

    "add total value input to the journey cache with comma separated values removed" when {
      "the form submission is valid" in {
        val SUT = createSUT
        val removeCompanyBenefitFormData = ("totalValue", "123,000.00")
        val totalValue = Map("benefitValue" -> "123000")

        when(SUT.journeyCacheService.cache(mockEq(totalValue))(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(removeCompanyBenefitFormData))

        status(result) mustBe SEE_OTHER

      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val SUT = createSUT
        val employmentName = "HMRC"
        val benefitName = "Employer Provided Services"
        val referer = "Url"

        val removeCompanyBenefitFormData = ("totalValue", "")

        when(SUT.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq(employmentName, benefitName, referer)))
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(removeCompanyBenefitFormData))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {
        val SUT = createSUT
        val employmentName = "HMRC"
        val benefitName = "Employer Provided Services"
        val referer = "Url"

        val removeCompanyBenefitFormData = ("totalValue", "1234Â£$%@")

        when(SUT.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq(employmentName, benefitName, referer)))
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(removeCompanyBenefitFormData))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "telephoneNumber" must {

    "show the contact by telephone page" when {
      "navigating from 'when did you stop getting benefits' page" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefit_EmploymentIdKey -> "1",
          EndCompanyBenefit_EmploymentNameKey -> employment.name,
          EndCompanyBenefit_BenefitTypeKey -> "amazing",
          EndCompanyBenefit_BenefitStopDateKey -> "before6April2017",
          EndCompanyBenefit_RefererKey -> "Test"
        )
        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        val result = SUT.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.canWeContactByPhone.title")
        doc.getElementsByClass("heading-secondary").text() must endWith(Messages("tai.benefits.ended.journey.preHeader"))
        doc must haveBackLink
        doc.getElementById("cancelLink").attr("href") mustBe controllers.benefits.routes.RemoveCompanyBenefitController.cancel.url
      }
    }

    "show the contact by telephone page" when {
      "navigating from 'total value of benefit' page" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefit_EmploymentIdKey -> "1",
          EndCompanyBenefit_EmploymentNameKey -> employment.name,
          EndCompanyBenefit_BenefitTypeKey -> "amazing",
          EndCompanyBenefit_BenefitStopDateKey -> "before6April2017",
          EndCompanyBenefit_BenefitValueKey -> "12345",
          EndCompanyBenefit_RefererKey -> "Test"
        )
        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        val result = SUT.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.canWeContactByPhone.title")
        doc.getElementsByClass("heading-secondary").text() must endWith(Messages("tai.benefits.ended.journey.preHeader"))
        doc must haveBackLink
        doc.getElementById("cancelLink").attr("href") mustBe controllers.benefits.routes.RemoveCompanyBenefitController.cancel.url
      }
    }
  }

  "submitTelephoneNumber" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val SUT = createSUT
        val expectedCache = Map(EndCompanyBenefit_TelephoneQuestionKey -> YesValue, EndCompanyBenefit_TelephoneNumberKey -> "12345678")
        when(SUT.journeyCacheService.cache(mockEq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))
        val result = SUT.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val SUT = createSUT
        val expectedCacheWithErasingNumber = Map(EndCompanyBenefit_TelephoneQuestionKey -> NoValue, EndCompanyBenefit_TelephoneNumberKey -> "")
        when(SUT.journeyCacheService.cache(mockEq(expectedCacheWithErasingNumber))(any())).thenReturn(Future.successful(expectedCacheWithErasingNumber))
        val result = SUT.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> NoValue, YesNoTextEntry -> "this value must not be cached"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val SUT = createSUT
        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(EndCompanyBenefit_RefererKey -> "Test")))
        val result = SUT.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> ""))

        status(result) mustBe BAD_REQUEST
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.canWeContactByPhone.title")
      }

      "there is a form validation error (additional, controller specific constraint)" in {
        val SUT = createSUT
        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(EndCompanyBenefit_RefererKey -> "Test")))

        val tooFewCharsResult = SUT.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234"))
        status(tooFewCharsResult) mustBe BAD_REQUEST

        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() mustBe Messages("tai.canWeContactByPhone.title")

        val tooManyCharsResult = SUT.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234123412341234123412341234123"))
        status(tooManyCharsResult) mustBe BAD_REQUEST

        val tooManyDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooManyDoc.title() mustBe Messages("tai.canWeContactByPhone.title")

      }
    }
  }


  "checkYourAnswers" must {
    "display check your answers containing populated values from the journey cache" in {
      val SUT = createSUT
      when(SUT.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
        Future.successful((
          Seq[String]("TestCompany","AwesomeType","After 6th","Yes","Url"),
          Seq[Option[String]](Some("10000"),Some("123456789"))
        ))
      )
      val result = SUT.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() mustBe Messages("tai.checkYourAnswers")
    }
  }

  "submit your answers" must {
    "invoke the back end 'end employment benefit' service and redirect to the confirmation page" when {

      "the request has an authorised session and a telephone number and benefit value have been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit = EndedCompanyBenefit(
          "Accommodation",
          Messages("tai.noLongerGetBenefit"),
          "Before 6th April",
          Some("1000000"),
          "Yes",
          Some("0123456789"))

        when(SUT.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](employmentId,"TestCompany","Accommodation","Before 6th April","Yes"),
            Seq[Option[String]](Some("1000000"),Some("0123456789"))
          ))
        )
        when(SUT.benefitsService.endedCompanyBenefit(any(), Matchers.eq(employmentId.toInt), Matchers.eq(endedCompanyBenefit))(any())).
          thenReturn(Future.successful("1"))
        when(SUT.trackingJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_EndEmploymentBenefitKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_EndEmploymentBenefitKey -> "true")))
        when(SUT.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation().url
        verify(SUT.journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and neither telephone number nor benefit value have been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit = EndedCompanyBenefit(
          "Accommodation",
          Messages("tai.noLongerGetBenefit"),
          "Before 6th April",
          None,
          "No",
          None)

        when(SUT.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](employmentId,"TestCompany","Accommodation","Before 6th April","No"),
            Seq[Option[String]](None,None)
          ))
        )
        when(SUT.benefitsService.endedCompanyBenefit(any(), Matchers.eq(employmentId.toInt), Matchers.eq(endedCompanyBenefit))(any())).
          thenReturn(Future.successful("1"))
        when(SUT.trackingJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_EndEmploymentBenefitKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_EndEmploymentBenefitKey -> "true")))
        when(SUT.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation().url
        verify(SUT.journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has not been provided but benefit value has been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit = EndedCompanyBenefit(
          "Accommodation",
          Messages("tai.noLongerGetBenefit"),
          "Before 6th April",
          Some("1000000"),
          "No",
          None)

        when(SUT.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](employmentId,"TestCompany","Accommodation","Before 6th April","No"),
            Seq[Option[String]](Some("1000000"),None)
          ))
        )
        when(SUT.benefitsService.endedCompanyBenefit(any(), Matchers.eq(employmentId.toInt), Matchers.eq(endedCompanyBenefit))(any())).
          thenReturn(Future.successful("1"))
        when(SUT.trackingJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_EndEmploymentBenefitKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_EndEmploymentBenefitKey -> "true")))
        when(SUT.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation().url
        verify(SUT.journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has been provided but benefit value has not been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit = EndedCompanyBenefit(
          "Accommodation",
          Messages("tai.noLongerGetBenefit"),
          "Before 6th April",
          None,
          "Yes",
          Some("0123456789"))

        when(SUT.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](employmentId,"TestCompany","Accommodation","Before 6th April","Yes"),
            Seq[Option[String]](None,Some("0123456789"))
          ))
        )
        when(SUT.benefitsService.endedCompanyBenefit(any(), Matchers.eq(employmentId.toInt), Matchers.eq(endedCompanyBenefit))(any())).
          thenReturn(Future.successful("1"))
        when(SUT.trackingJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_EndEmploymentBenefitKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_EndEmploymentBenefitKey -> "true")))
        when(SUT.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation().url
        verify(SUT.journeyCacheService, times(1)).flush()(any())
      }
    }
  }

  "cancel" must {
    "flush the cache and redirect to start of journey" in {
      val SUT = createSUT

      when(SUT.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq("Url")))
      when(SUT.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = SUT.cancel(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe "Url"
      verify(SUT.journeyCacheService, times(1)).flush()(any())
      verify(SUT.journeyCacheService, times(1)).mandatoryValues(any())(any())
    }
  }

  "confirmation" must {
    "show the update income details confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT

        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.income.previousYears.confirmation.heading")
      }
    }
  }


  private val employment = Employment("company name", Some("123"), new LocalDate("2016-05-26"),
    Some(new LocalDate("2016-05-26")), Nil, "", "", 2)

  def generateNino: Nino = new Generator(new Random).nextNino

  val startOfTaxYear = TaxYearResolver.startOfCurrentTaxYear.toString(DateWithYearFormat)

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private def createSUT = new SUT

  private class SUT extends RemoveCompanyBenefitController {
    override val taiService: TaiService = mock[TaiService]
    override val auditService: AuditService = mock[AuditService]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val trackingJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: PartialRetriever = mock[PartialRetriever]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val benefitsService: BenefitsService = mock[BenefitsService]

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(TaiRoot("", 1, "", "", None, "", "", false, None)))
  }

}