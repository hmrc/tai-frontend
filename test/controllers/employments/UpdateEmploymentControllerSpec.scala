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
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.model.domain.{Employment, IncorrectIncome}
import uk.gov.hmrc.tai.service.{EmploymentService, JourneyCacheService, PersonService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Random

class UpdateEmploymentControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants
  with BeforeAndAfter
  with BeforeAndAfterEach {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeEach: Unit = {
    Mockito.reset(journeyCacheService, successfulJourneyCacheService, personService)
  }

  "employmentDetailsUpdate" must {
    "show the 'What Do You Want To Tell Us' Page" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(sut.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        val cache = Map(UpdateEmployment_EmploymentIdKey -> "1", UpdateEmployment_NameKey -> employment.name)
        when(sut.journeyCacheService.cache(Matchers.eq(cache))(any())).thenReturn(Future.successful(cache))
        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updateEmployment.whatDoYouWantToTellUs.title", employment.name))
      }
    }

    "retrieve the employer name from the cache" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(sut.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        val cache = Map(UpdateEmployment_EmploymentIdKey -> "1", UpdateEmployment_NameKey -> employment.name)
        when(sut.journeyCacheService.cache(Matchers.eq(cache))(any())).thenReturn(Future.successful(cache))
        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        verify(sut.journeyCacheService, times(1)).cache(Matchers.eq(cache))(any())
      }
    }
    "retrieve the employment update details from the cache" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(sut.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        val cacheDetails = Some("updateDetails")
        when(sut.journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(cacheDetails))
        val cache = Map(UpdateEmployment_EmploymentIdKey -> "1", UpdateEmployment_NameKey -> employment.name)
        when(sut.journeyCacheService.cache(Matchers.eq(cache))(any())).thenReturn(Future.successful(cache))

        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updateEmployment.whatDoYouWantToTellUs.title", employment.name))
        doc.toString must include("updateDetails")
        verify(sut.journeyCacheService, times(1)).currentValue(any())(any())
      }
    }

    "throw exception" when {
      "employment not found" in {
        val sut = createSUT
        when(sut.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submitUpdateEmploymentDetails" must {

    "redirect to the 'TODO' page" when {
      "the form submission is valid" in {

        val sut = createSUT

        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(("employmentDetails", "test details")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
      }
    }

    "add employment details to the journey cache" when {
      "the form submission is valid" in {

        val sut = createSUT

        val employmentDetailsFormData = ("employmentDetails", "test details")
        val employmentDetails = Map("employmentDetails" -> "test details")

        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(employmentDetailsFormData))

        status(result) mustBe SEE_OTHER

        verify(sut.journeyCacheService, times(1)).cache(mockEq(employmentDetails))(any())
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {

        val sut = createSUT

        val employmentDetailsFormData = ("employmentDetails", "")

        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddEmployment_NameKey -> "Test")))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(employmentDetailsFormData))

        status(result) mustBe BAD_REQUEST
      }
    }

    "not add employment details to the journey cache" when {
      "the form submission is invalid" in {

        val sut = createSUT

        val employmentDetailsFormData = ("employmentDetails", "")
        val employmentDetails = Map("employmentDetails" -> "")

        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddEmployment_NameKey -> "Test")))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(employmentDetailsFormData))

        status(result) mustBe BAD_REQUEST

        verify(sut.journeyCacheService, never()).cache(mockEq(employmentDetails))(any())
      }
    }
  }

  "addTelephoneNumber" must {
    "show the contact by telephone page" when {
      "valid details has been passed" in {
        val sut = createSUT
        val cache = Map(UpdateEmployment_EmploymentIdKey -> "1", UpdateEmployment_NameKey -> employment.name)
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(sut.journeyCacheService.mandatoryValueAsInt(any())(any())).thenReturn(Future.successful(1))
        when(sut.journeyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(None, None)))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""
      }
      "we fetch telephone details form cache" in {
        val sut = createSUT
        val cache = Map(UpdateEmployment_EmploymentIdKey -> "1", UpdateEmployment_NameKey -> employment.name)
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(sut.journeyCacheService.mandatoryValueAsInt(any())(any())).thenReturn(Future.successful(1))
        when(sut.journeyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(Some(YesValue), Some("01215485965"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe "01215485965"
      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT
        val expectedCache = Map(UpdateEmployment_TelephoneQuestionKey -> YesValue, UpdateEmployment_TelephoneNumberKey -> "12345678")
        when(sut.journeyCacheService.cache(mockEq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))

        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber = Map(UpdateEmployment_TelephoneQuestionKey -> NoValue, UpdateEmployment_TelephoneNumberKey -> "")
        when(sut.journeyCacheService.cache(mockEq(expectedCacheWithErasingNumber))(any())).thenReturn(Future.successful(expectedCacheWithErasingNumber))

        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> NoValue, YesNoTextEntry -> "this value must not be cached"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT
        val cache = Map(UpdateEmployment_EmploymentIdKey -> "1")
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

      "there is a form validation error (additional, controller specific constraint)" in {
        val sut = createSUT
        val cache = Map(UpdateEmployment_EmploymentIdKey -> "1")
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val tooFewCharsResult = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234"))
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234123412341234123412341234123"))
        status(tooManyCharsResult) mustBe BAD_REQUEST
        val tooManyDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooManyDoc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
  }

  "check your answers" must {
    "show summary page" when {
      "valid details has been passed" in {
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("1", "emp-name", "whatYouToldUs", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )

        val result = sut.updateEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }
  }

  "submit your answers" must {
    "invoke the back end 'incorrectEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {
        val sut = createSUT
        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "Yes", Some("123456789"))
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("1", "whatYouToldUs", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )
        when(sut.employmentService.incorrectEmployment(any(), Matchers.eq(1), Matchers.eq(incorrectEmployment))(any())).
          thenReturn(Future.successful("1"))
        when(sut.successfulJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_UpdateEmploymentKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_UpdateEmploymentKey -> "true")))
        when(sut.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.confirmation().url
        verify(sut.journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has not been provided" in {
        val sut = createSUT
        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "No", None)
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("1", "whatYouToldUs", "No"),
            Seq[Option[String]](None)
          ))
        )
        when(sut.employmentService.incorrectEmployment(any(), Matchers.eq(1), Matchers.eq(incorrectEmployment))(any())).
          thenReturn(Future.successful("1"))
        when(sut.successfulJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_UpdateEmploymentKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_UpdateEmploymentKey -> "true")))
        when(sut.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.confirmation().url
        verify(sut.journeyCacheService, times(1)).flush()(any())
      }
    }
  }

  "confirmation" must {
    "show the update employment confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT

        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.employmentConfirmation.heading"))
      }
    }
  }

  private val employment = Employment("company name", Some("123"), new LocalDate("2016-05-26"),
    Some(new LocalDate("2016-05-26")), Nil, "", "", 2, None, false, false)

  def generateNino: Nino = new Generator(new Random).nextNino

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createSUT = new SUT

  val personService: PersonService = mock[PersonService]
  val journeyCacheService = mock[JourneyCacheService]
  val successfulJourneyCacheService = mock[JourneyCacheService]


  private class SUT extends UpdateEmploymentController(
    mock[EmploymentService],
    personService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    journeyCacheService,
    successfulJourneyCacheService,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))
  }

}
