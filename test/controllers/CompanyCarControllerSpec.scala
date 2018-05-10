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
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.{Matchers, Mockito}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiNoCompanyCarFoundResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.forms.benefits.DateForm
import uk.gov.hmrc.tai.model.domain.{Address, Person}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{JourneyCacheService, PersonService, SessionService}
import uk.gov.hmrc.tai.util.JourneyCacheConstants

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Random

class CompanyCarControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport with JourneyCacheConstants{

  "CompanyCarController" should {

    "Successfully present the update/remove company car view" when {
      "GET'ing the getCompanyCarDetails endpoint with an authorised session" in {
        val sut = createSUT()
        when(sut.companyCarService.companyCarEmploymentId(any())).thenReturn(Future.successful(1))
        when(sut.companyCarService.beginJourney(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(carWithoutFuelBenCache)))

        val result = sut.getCompanyCarDetails()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.changeCompanyCar.title"))
      }
    }

    "redirect top companyCarService" when{
      "the service returns TaiCompanyCarWithdrawnDateFoundResponse" in{
        val sut = createSUT()
        when(sut.companyCarService.companyCarEmploymentId(any())).thenReturn(Future.successful(1))
        when(sut.companyCarService.beginJourney(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(
          TaiNoCompanyCarFoundResponse("A car with date withdrawn found!")))

        val result = sut.getCompanyCarDetails()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe ApplicationConfig.companyCarServiceUrl
      }
    }

    "Redirect to the company car end date view" when {
      "POST'ing to the handleUserJourneyChoice endpoint with a 'removeCar' user choice and the companyCarForceRedirect feature toggle is off" in {
          val sut = createSUT(isCompanyCarForceRedirectEnabled = false)
          val request = FakeRequest("POST", "").withFormUrlEncodedBody("userChoice" -> "removeCar").withSession(
            SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
          )
          val result = sut.handleUserJourneyChoice()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe routes.CompanyCarController.getCompanyCarEndDate().url
          Mockito.verify(sut.sessionService, Mockito.never()).invalidateCache()(any())
        }
    }

    "Redirect to the company car service landing page" when {
      "POST'ing to the handleUserJourneyChoice endpoint with a 'removeCar' user choice and the companyCarForceRedirect feature toggle is on" in {
          val sut = createSUT(isCompanyCarForceRedirectEnabled = true)
          val request = FakeRequest("POST", "").withFormUrlEncodedBody("userChoice" -> "removeCar").withSession(
            SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
          )
          when(sut.sessionService.invalidateCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))

          val result = sut.handleUserJourneyChoice()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe ApplicationConfig.companyCarServiceUrl
          Mockito.verify(sut.sessionService, Mockito.times(1)).invalidateCache()(any())
        }
    }

    "Redirect to the company car service landing page" when {
      "POST'ing to the handleUserJourneyChoice endpoint with a 'changeCarDetails' user choice" in {
        val sut = createSUT()
        val request = FakeRequest("POST", "").withFormUrlEncodedBody("userChoice" -> "changeCarDetails").withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.sessionService.invalidateCache()(any())).thenReturn(Future.successful(HttpResponse(OK)))

        val result = sut.handleUserJourneyChoice()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe ApplicationConfig.companyCarServiceUrl
        Mockito.verify(sut.sessionService, Mockito.times(1)).invalidateCache()(any())
      }
    }

    "Successfully present the company car end date view" when {
      "GET'ing the getCompanyCarEndDate endpoint with an authorised session" in {
        val sut = createSUT()
        when(sut.journeyCacheService.currentValueAsDate(any())(any())).thenReturn(Future.successful(Some(new LocalDate("2017-9-9"))))
        val result = sut.getCompanyCarEndDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.companyCar.endDate.title"))
      }
    }

    "Redirect to the company car fuel benefit date view" when {
      "POST'ing to the handleCompanyCarEndDate endpoint" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "2017",
          sut.dateForm.DateFormMonth -> "9",
          sut.dateForm.DateFormDay -> "9"
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithFuelBenCache))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(carWithFuelBenCache))
        val result = sut.handleCompanyCarEndDate()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.CompanyCarController.getFuelBenefitEndDate().url
      }
    }

    "Redirect to check your answers page view" when {
      "POST'ing to the handleCompanyCarEndDate endpoint with no fuel data in cache" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "2017",
          sut.dateForm.DateFormMonth -> "9",
          sut.dateForm.DateFormDay -> "10"
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithoutFuelBenCache))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(carWithoutFuelBenCache))
        val result = sut.handleCompanyCarEndDate()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.CompanyCarController.checkYourAnswers().url
      }
    }

    "Successfully present the fuel benefits end date view" when {
      "GET'ing the getFuelBenefitEndDate endpoint with an authorised session" in {
        val sut = createSUT()
        when(sut.journeyCacheService.currentValueAsDate(any())(any())).thenReturn(Future.successful(None))
        val result = sut.getFuelBenefitEndDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.companyCar.fuelBenefitEndDate.title"))
      }
    }

    "Successfully present the fuel benefits end date view with pre-populated fuel benefit end date" when {
      "GET'ing the getFuelBenefitEndDate endpoint with an authorised session" in {
        val sut = createSUT()
        when(sut.journeyCacheService.currentValueAsDate(any())(any())).thenReturn(Future.successful(Some(new LocalDate("2016-7-7"))))
        val result = sut.getFuelBenefitEndDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.companyCar.fuelBenefitEndDate.title"))
        doc.getElementById("dateForm_day").attr("value") mustBe "7"
        doc.getElementById("dateForm_month").attr("value") mustBe "7"
        doc.getElementById("dateForm_year").attr("value") mustBe "2016"
      }
    }


    "Redirect to the check your answers view" when {
      "POST'ing to the handleFuelBenefitEndDate endpoint with a value after the fuel ben start date" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "2017",
          sut.dateForm.DateFormMonth -> "9",
          sut.dateForm.DateFormDay -> "10"
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithFuelBenCache))
        when(sut.journeyCacheService.cache(Matchers.eq(CompanyCar_DateFuelBenefitStoppedKey),Matchers.eq("2017-09-10"))(any())).thenReturn(Future.successful(carWithFuelBenCache))
        val result = sut.handleFuelBenefitEndDate()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.CompanyCarController.checkYourAnswers().url
      }

      "POST'ing to the handleFuelBenefitEndDate endpoint with a value after the car ben start date (where fuel start date not known)" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "2017",
          sut.dateForm.DateFormMonth -> "9",
          sut.dateForm.DateFormDay -> "10"
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithFuelBenWithoutStartDateCache))
        when(sut.journeyCacheService.cache(Matchers.eq(CompanyCar_DateFuelBenefitStoppedKey),Matchers.eq("2017-09-10"))(any())).thenReturn(Future.successful(carWithFuelBenWithoutStartDateCache))
        val result = sut.handleFuelBenefitEndDate()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.CompanyCarController.checkYourAnswers().url
      }
    }

    "Successfully present the check your answers view" when {
      "GET'ing the checkYourAnswers endpoint with an authorised session" in {
        val sut = createSUT()
        val cacheMap = Map(
          CompanyCar_CarModelKey -> "Some Car Model",
          CompanyCar_CarProviderKey -> "Some company name",
          CompanyCar_DateGivenBackKey -> "2017-06-15",
          CompanyCar_DateFuelBenefitStoppedKey -> "2017-05-15"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cacheMap))
        val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.companyCar.checkAnswers.title"))
      }
    }

    "Redirect to the confirmation view" when {
      "POST'ing to the handleCheckYourAnswers endpoint" in {
        val sut = createSUT()
        when(sut.companyCarService.withdrawCompanyCarAndFuel(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponse))
        val result = sut.handleCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.CompanyCarController.confirmation().url
      }
    }

    "Successfully present the confirmation view" when {
      "GET'ing the confirmation endpoint with an authorised session" in {
        val sut = createSUT()
        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.companyCar.confirmation.title"))
      }
    }

    "trap an invalid submission and respond with a bad request" when {
      "POST'ing to the handleUserJourneyChoice with an authorised session, and no radio selection" in {
        val sut = createSUT()
        val request = FakeRequest("POST", "").withFormUrlEncodedBody("userChoice" -> "").withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.mandatoryValues(any())(any())).thenReturn(Future.successful(Seq("model", "provider")))

        val result =  sut.handleUserJourneyChoice()(request)
        status(result) mustBe BAD_REQUEST
      }

      "POST'ing to the handleCompanyCarEndDate endpoint with blank form" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "",
          sut.dateForm.DateFormMonth -> "",
          sut.dateForm.DateFormDay -> ""
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithFuelBenCache))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(carWithFuelBenCache))
        val result = sut.handleCompanyCarEndDate()(request)
        status(result) mustBe BAD_REQUEST
      }

      "POST'ing to the handleFuelBenefitDate endpoint with blank form" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "",
          sut.dateForm.DateFormMonth -> "",
          sut.dateForm.DateFormDay -> ""
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithFuelBenCache))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(carWithFuelBenCache))
        val result = sut.handleFuelBenefitEndDate()(request)
        status(result) mustBe BAD_REQUEST
      }

      "POST'ing to the handleFuelBenefitDate endpoint with fuel benefit end date earlier than the known start date" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "2015",
          sut.dateForm.DateFormMonth -> "7",
          sut.dateForm.DateFormDay -> "6"
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithFuelBenCache))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(carWithFuelBenCache))
        val result = sut.handleFuelBenefitEndDate()(request)
        status(result) mustBe BAD_REQUEST
      }

      "POST'ing to the handleFuelBenefitDate endpoint with an end date earlier than the car benefit start start date (fuel benefit start date is not known)" in {
        val sut = createSUT()
        val formData = Json.obj(
          sut.dateForm.DateFormYear -> "2015",
          sut.dateForm.DateFormMonth -> "7",
          sut.dateForm.DateFormDay -> "6"
        )
        val request = FakeRequest("POST", "").withJsonBody(formData).withSession(
          SessionKeys.authProvider -> "IDA", SessionKeys.userId -> s"/path/to/authority"
        )
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(carWithFuelBenWithoutStartDateCache))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(carWithFuelBenWithoutStartDateCache))
        val result = sut.handleFuelBenefitEndDate()(request)
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "redirectCompanyCarSelection" must {
    "redirect to getCompanyCarDetails page" in {
      val sut = createSUT()
      when(sut.journeyCacheService.cache(Matchers.eq(CompanyCar_EmployerIdKey), Matchers.eq("1"))(any())).thenReturn(Future.successful(Map.empty[String, String]))
      val result = sut.redirectCompanyCarSelection(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.CompanyCarController.getCompanyCarDetails().url
    }
  }

  val fakeNino = new Generator(new Random).nextNino
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val carWithFuelBenCache = Map(
    CompanyCar_CarModelKey -> "make model1",
    CompanyCar_CarProviderKey -> "employer ltd",
    CompanyCar_DateStartedKey -> "2015-7-7",
    CompanyCar_HasActiveFuelBenefitdKey -> "true",
    CompanyCar_DateFuelBenefitStartedKey -> "2015-7-7")

  val carWithFuelBenWithoutStartDateCache = Map(
    CompanyCar_CarModelKey -> "make model1",
    CompanyCar_CarProviderKey -> "employer ltd",
    CompanyCar_DateStartedKey -> "2015-7-7",
    CompanyCar_HasActiveFuelBenefitdKey -> "true")

  val carWithoutFuelBenCache = Map(
    CompanyCar_CarModelKey -> "make model2",
    CompanyCar_CarProviderKey -> "jobs done",
    CompanyCar_DateStartedKey -> "2015-7-7",
    CompanyCar_HasActiveFuelBenefitdKey -> "false")

  val fakeAuthority = AuthBuilder.createFakeAuthority(fakeNino.nino)

  val fakePerson = Person(fakeNino, "firstname", "surname", Some(new LocalDate(1985, 10, 10)), Address("l1", "l2", "l3", "pc", "country"), false, false)


  def createSUT(isCompanyCarForceRedirectEnabled: Boolean = false) = new SUT(isCompanyCarForceRedirectEnabled)

  class SUT(isCompanyCarForceRedirectEnabled: Boolean) extends CompanyCarController {

    override val personService: PersonService = mock[PersonService]
    override val sessionService: SessionService = mock[SessionService]
    override val companyCarService: CompanyCarService = mock[CompanyCarService]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = MockPartialRetriever
    override val companyCarForceRedirectEnabled: Boolean = isCompanyCarForceRedirectEnabled

    when(authConnector.currentAuthority(any(), any())).thenReturn(Future.successful(Some(fakeAuthority)))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson))

    val dateForm = DateForm(Messages("tai.companyCar.endDate.blank"))
  }

  override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

}
