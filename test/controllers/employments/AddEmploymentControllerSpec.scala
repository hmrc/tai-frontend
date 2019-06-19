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

package controllers.employments

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms.employments.AddEmploymentPayrollNumberForm._
import uk.gov.hmrc.tai.forms.employments.{AddEmploymentFirstPayForm, EmploymentAddDateForm}
import uk.gov.hmrc.tai.model.domain.AddEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, PersonService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class AddEmploymentControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(addEmploymentJourneyCacheService)
  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "addEmploymentName" must {
    "show the employment name form page" when {
      "the request has an authorised session and no previously supplied employment name is present in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.currentValue(Matchers.eq(AddEmployment_NameKey))(any())).thenReturn(Future.successful(None))
        val result = sut.addEmploymentName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.addNameForm.title"))
      }
      "the request has an authorised session and a previously supplied employment name is present in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.currentValue(Matchers.eq(AddEmployment_NameKey))(any())).thenReturn(Future.successful(Some("employer one plc")))
        val result = sut.addEmploymentName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.addNameForm.title"))
        doc.toString must include("employer one plc")
      }
    }
  }

  "submitEmploymentName" must {
    "redirect to the Start Date form" when {
      "the form submission is valid" in {
        val sut = createSUT

        val expectedCache = Map("employmentName" -> "the employer")
        when(addEmploymentJourneyCacheService.cache(Matchers.eq(expectedCache))(any()))
          .thenReturn(Future.successful(expectedCache))

        val result = sut.submitEmploymentName()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("employmentName", "the employer")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url
      }
    }

    "reload the page with errors" when {
      "the form entry is invalid" in {
        val sut = createSUT
        val result = sut.submitEmploymentName()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("employmentName", "")))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.addNameForm.title"))
      }
    }
  }

  "addStartDate" must {
    "show the employment start date form page" when {
      "the request has an authorised session and no previously supplied start date is present in cache" in {
        val sut = createSUT
        val employmentName = "TEST"
        when(addEmploymentJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Seq(employmentName), Seq(None)))

        val result = sut.addEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.startDateForm.pagetitle"))
        doc.select("#tellUsStartDateForm_year").get(0).attributes.get("value") mustBe ""
      }

      "the request has an authorised session and a previously supplied start date is present in cache" in {
        val sut = createSUT
        val employmentName = "TEST"
        when(addEmploymentJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Seq(employmentName), Seq(Some("2017-12-12"))))

        val result = sut.addEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.startDateForm.pagetitle"))
        doc.select("#tellUsStartDateForm_year").get(0).attributes.get("value") mustBe "2017"
      }
    }

  }

  "submit start date" must {
    "return redirect" when {
      "form is valid and start date is over 6 weeks ago" in {
        val sut = createSUT
        val formData = Json.obj(
          sut.employmentStartDateForm.EmploymentFormDay -> "01",
          sut.employmentStartDateForm.EmploymentFormMonth -> "02",
          sut.employmentStartDateForm.EmploymentFormYear -> "2017"
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddEmployment_NameKey -> "Test")))

        val result = sut.submitEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url
      }

      "form is valid and start date is less than 6 weeks" in {
        val sut = createSUT
        val date = new LocalDate().minusDays(4)

        val formData = Json.obj(
          sut.employmentStartDateForm.EmploymentFormDay -> date.getDayOfMonth.toString,
          sut.employmentStartDateForm.EmploymentFormMonth -> date.getMonthOfYear.toString,
          sut.employmentStartDateForm.EmploymentFormYear -> date.getYear.toString
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddEmployment_NameKey -> "Test")))

        val result = sut.submitEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.receivedFirstPay().url
      }
    }

    "return bad request" when {
      "form is invalid" in {
        val sut = createSUT
        val formData = Json.obj(
          sut.employmentStartDateForm.EmploymentFormDay -> "01",
          sut.employmentStartDateForm.EmploymentFormMonth -> "02",
          sut.employmentStartDateForm.EmploymentFormYear -> (LocalDate.now().getYear + 1).toString
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddEmployment_NameKey -> "Test")))

        val result = sut.submitEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe BAD_REQUEST
      }
    }

    "save details in cache" when {
      "form is valid and start date is over 6 weeks ago" in {
        val sut = createSUT
        val formData = Json.obj(
          sut.employmentStartDateForm.EmploymentFormDay -> "01",
          sut.employmentStartDateForm.EmploymentFormMonth -> "02",
          sut.employmentStartDateForm.EmploymentFormYear -> "2017"
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddEmployment_NameKey -> "Test")))
        val result = sut.submitEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        verify(addEmploymentJourneyCacheService, times(1)).cache(Matchers.eq(Map(AddEmployment_NameKey -> "Test",
          AddEmployment_StartDateKey -> "2017-02-01", AddEmployment_StartDateWithinSixWeeks -> NoValue)))(any())
      }

      "form is valid and start date is less than 6 weeks" in {
        val sut = createSUT
        val date = new LocalDate().minusDays(4)

        val formData = Json.obj(
          sut.employmentStartDateForm.EmploymentFormDay -> date.getDayOfMonth.toString,
          sut.employmentStartDateForm.EmploymentFormMonth -> date.getMonthOfYear.toString,
          sut.employmentStartDateForm.EmploymentFormYear -> date.getYear.toString
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddEmployment_NameKey -> "Test")))
        val result = sut.submitEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        verify(addEmploymentJourneyCacheService, times(1)).cache(Matchers.eq(Map(AddEmployment_NameKey -> "Test",
          AddEmployment_StartDateKey -> date.toString("yyyy-MM-dd"), AddEmployment_StartDateWithinSixWeeks -> YesValue)))(any())
      }
    }
  }

  "received First Pay" must {
    "show the first pay choice page" when {
      "the request has an authorised session and no previous response is held in cache" in {
        val sut = createSUT
        val employmentName = "TEST"
        when(addEmploymentJourneyCacheService.collectedValues(Matchers.eq(Seq(AddEmployment_NameKey)), Matchers.eq(Seq(AddEmployment_RecewivedFirstPayKey)))(any()))
          .thenReturn(Future.successful((Seq(employmentName), Seq(None))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentFirstPay.title", employmentName))
        doc.select("input[id=firstPayChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=firstPayChoice-no][checked=checked]").size() mustBe 0
      }
      "the request has an authorised session and a previous response is held in cache" in {
        val sut = createSUT
        val employmentName = "TEST"
        when(addEmploymentJourneyCacheService.collectedValues(Matchers.eq(Seq(AddEmployment_NameKey)), Matchers.eq(Seq(AddEmployment_RecewivedFirstPayKey)))(any()))
          .thenReturn(Future.successful((Seq(employmentName), Seq(Some(YesValue)))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentFirstPay.title", employmentName))
        doc.select("input[id=firstPayChoice-yes][checked=checked]").size() mustBe 1
        doc.select("input[id=firstPayChoice-no][checked=checked]").size() mustBe 0
      }
    }
  }

  "submit first pay choice" must {

    "redirect user to payroll number page" when {
      "yes is selected" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.cache(mockEq(AddEmployment_RecewivedFirstPayKey), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddEmploymentFirstPayForm.FirstPayChoice -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url
      }
    }

    "redirect user to error page" when {
      "no is selected" in {
        val sut = createSUT
        val employmentName = "TEST-Employer"
        when(addEmploymentJourneyCacheService.cache(mockEq(AddEmployment_RecewivedFirstPayKey), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddEmploymentFirstPayForm.FirstPayChoice -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.sixWeeksError().url
      }
    }

    "raise an audit event" when {
      "no is selected" in {
        val sut = createSUT
        val employmentName = "TEST-Employer"
        when(addEmploymentJourneyCacheService.mandatoryValue(Matchers.eq(AddEmployment_NameKey))(any())).thenReturn(Future.successful(employmentName))

        Await.result(sut.sixWeeksError()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddEmploymentFirstPayForm.FirstPayChoice -> NoValue)),5 seconds)

        verify(auditService, times(1)).createAndSendAuditEvent(Matchers.eq(AddEmployment_CantAddEmployer),
          Matchers.eq(Map("nino" -> FakeAuthAction.nino.nino)))(Matchers.any(), Matchers.any())
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val employmentName = "TEST-Employer"

        when(addEmploymentJourneyCacheService.mandatoryValue(Matchers.eq(AddEmployment_NameKey))(any())).thenReturn(Future.successful(employmentName))

        val result = sut.submitFirstPay()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddEmploymentFirstPayForm.FirstPayChoice -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentFirstPay.title", employmentName))
      }
    }
  }

  "add employment payroll number" must {
    "show the add payroll number page" when {
      "the request has an authorised session and no previous response is held in cache" in {
        val sut = createSUT
        val employerName = "TEST"
        val cache = Map(AddEmployment_NameKey -> employerName, AddEmployment_StartDateWithinSixWeeks -> YesValue)
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=payrollNumberChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'no' response is held in cache" in {
        val sut = createSUT
        val employerName = "TEST"
        val cache = Map(
          AddEmployment_NameKey -> employerName,
          AddEmployment_StartDateWithinSixWeeks -> YesValue,
          AddEmployment_PayrollNumberQuestionKey -> NoValue,
          AddEmployment_PayrollNumberKey -> "should be ignored")
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=payrollNumberChoice-no][checked=checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'yes' response is held in cache" in {
        val sut = createSUT
        val employerName = "TEST"
        val cache = Map(
          AddEmployment_NameKey -> employerName,
          AddEmployment_StartDateWithinSixWeeks -> YesValue,
          AddEmployment_PayrollNumberQuestionKey -> YesValue,
          AddEmployment_PayrollNumberKey -> "should be displayed")
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-yes][checked=checked]").size() mustBe 1
        doc.select("input[id=payrollNumberChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe "should be displayed"
      }
    }
  }

  "submit employment payroll number" must {
    "cache payroll number" when {
      "the form is valid and user knows their payroll number" in {
        val sut = createSUT
        val payrollNo = "1234"
        val mapWithPayrollNumber = Map(
          AddEmployment_PayrollNumberQuestionKey -> YesValue,
          AddEmployment_PayrollNumberKey -> payrollNo
        )
        when(addEmploymentJourneyCacheService.cache(mockEq(mapWithPayrollNumber))(any())).thenReturn(Future.successful(mapWithPayrollNumber))
        Await.result(sut.submitEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> YesValue, PayrollNumberEntry -> payrollNo)), 5 seconds)

        verify(addEmploymentJourneyCacheService, times(1)).cache(mockEq(mapWithPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user knows their payroll number" in {
        val sut = createSUT
        val payrollNo = "1234"
        val mapWithPayrollNumber = Map(
          AddEmployment_PayrollNumberQuestionKey -> YesValue,
          AddEmployment_PayrollNumberKey -> payrollNo
        )
        when(addEmploymentJourneyCacheService.cache(mockEq(mapWithPayrollNumber))(any())).thenReturn(Future.successful(mapWithPayrollNumber))
        val result = sut.submitEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> YesValue, PayrollNumberEntry -> payrollNo))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url
      }
    }


    "cache payroll number as not known value" when {
      "the form is valid and user doesn't know its payroll number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.addEmployment.employmentPayrollNumber.notKnown")
        val mapWithPayrollNumber = Map(
          AddEmployment_PayrollNumberQuestionKey -> NoValue,
          AddEmployment_PayrollNumberKey -> payrollNo
        )

        when(addEmploymentJourneyCacheService.cache(mockEq(mapWithPayrollNumber))(any())).thenReturn(Future.successful(mapWithPayrollNumber))

        Await.result(sut.submitEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> NoValue, PayrollNumberEntry -> "")), 5 seconds)

        verify(addEmploymentJourneyCacheService, times(1)).cache(mockEq(mapWithPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user doesn't know its payroll number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.addEmployment.employmentPayrollNumber.notKnown")
        val mapWithPayrollNumber = Map(
          AddEmployment_PayrollNumberQuestionKey -> NoValue,
          AddEmployment_PayrollNumberKey -> payrollNo
        )

        when(addEmploymentJourneyCacheService.cache(mockEq(mapWithPayrollNumber))(any())).thenReturn(Future.successful(mapWithPayrollNumber))

        val result = sut.submitEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> NoValue, PayrollNumberEntry -> ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val employerName = "TEST"
        val cache = Map(AddEmployment_NameKey -> employerName, AddEmployment_StartDateWithinSixWeeks -> YesValue)
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.submitEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> YesValue, PayrollNumberEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
      }
    }
  }

  "add telephone number" must {
    "show the contact by telephone page" when {
      "the request has an authorised session and no previous response is held in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(None, None)))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'no' response is held in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(Some(NoValue), Some("should be ignored"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'yes' response is held in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(Some(YesValue), Some("should be displayed"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe "should be displayed"
      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT

        val expectedCache = Map(AddEmployment_TelephoneQuestionKey -> YesValue, AddEmployment_TelephoneNumberKey -> "12345678")
        when(addEmploymentJourneyCacheService.cache(mockEq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))
        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers().url
      }
      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber = Map(AddEmployment_TelephoneQuestionKey -> NoValue, AddEmployment_TelephoneNumberKey -> "")
        when(addEmploymentJourneyCacheService.cache(mockEq(expectedCacheWithErasingNumber))(any())).thenReturn(Future.successful(expectedCacheWithErasingNumber))
        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> NoValue, YesNoTextEntry -> "this value must not be cached"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT

        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
      "there is a form validation error (additional, controller specific constraint)" in {
        val sut = createSUT

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
    "show the check answers summary page" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("emp-name", "2017-06-15", "emp-ref-1234", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )

        val result = sut.addEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }
  }

  "submit your answers" must {
    "invoke the back end 'addEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session amd a telephone number has been provided" in {
        val sut = createSUT

        val expectedModel = AddEmployment("empName", LocalDate.parse("2017-04-04"), "I do not know", "Yes", Some("123456789"))

        when(addEmploymentJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(
          Seq("empName", "2017-04-04", "I do not know", "Yes"), Seq(Some("123456789"))
        ))

        when(employmentService.addEmployment(any(), Matchers.eq(expectedModel))(any())).thenReturn(Future.successful("envelope-123"))
        when(addEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))
        when(trackSuccessJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_AddEmploymentKey), Matchers.eq("true"))(any())).
          thenReturn(Future.successful(Map(TrackSuccessfulJourney_AddEmploymentKey -> "true")))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.confirmation().url
      }

      "the request has an authorised session amd no telephone number was provided" in {
        val sut = createSUT

        val expectedModel = AddEmployment("empName", LocalDate.parse("2017-04-04"), "I do not know", "No", None)
        val expectedSuccessfulJourneyCache = Map("addEmployment" -> "true")

        when(addEmploymentJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(
          (Seq("empName", "2017-04-04", "I do not know", "No"), Seq(None))
        ))

        when(employmentService.addEmployment(any(), Matchers.eq(expectedModel))(any())).thenReturn(Future.successful("envelope-123"))
        when(addEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))
        when(trackSuccessJourneyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(expectedSuccessfulJourneyCache))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.confirmation().url
      }
    }
  }

  "confirmation" must {
    "show the add employment confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT

        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.employmentConfirmation.heading"))
      }
    }
  }

  "cancel" must {
    "redirect to the the TaxAccountSummaryController" in {

      when(addEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = createSUT.cancel()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino: String = new Generator().nextNino.nino

  private def createSUT = new SUT

  val auditService = mock[AuditService]
  val employmentService = mock[EmploymentService]
  val addEmploymentJourneyCacheService = mock[JourneyCacheService]
  val trackSuccessJourneyCacheService = mock[JourneyCacheService]

  private class SUT extends AddEmploymentController(
    auditService,
    employmentService,
    FakeAuthAction,
    FakeValidatePerson,
    addEmploymentJourneyCacheService,
    trackSuccessJourneyCacheService,
    mock[AuditConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer) {

    val employmentStartDateForm = EmploymentAddDateForm("employer")

  }

}
