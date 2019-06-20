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

package controllers.pensions

import builders.{RequestBuilder}
import controllers.actions.FakeValidatePerson
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{when, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.forms.pensions.AddPensionProviderNumberForm._
import uk.gov.hmrc.tai.forms.pensions.{AddPensionProviderFirstPayForm, PensionAddDateForm}
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class AddPensionProviderControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with AuditConstants
  with FormValuesConstants
  with JourneyCacheConstants
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(addPensionProviderJourneyCacheService)
  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "addPensionProviderName" must {
    "show the pensionProvider name form page" when {
      "the request has an authorised session and no previous value in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.currentValue(Matchers.eq(AddPensionProvider_NameKey))(any())).thenReturn(Future.successful(None))

        val result = sut.addPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.addNameForm.title"))
        doc.toString must not include ("testPensionName123")
      }
    }
  }

  "addPensionProviderName" must {
    "show the pensionProvider name form page" when {
      "the request has an authorised session and previous value exists in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.currentValue(Matchers.eq(AddPensionProvider_NameKey))(any())).thenReturn(Future.successful(Some("testPensionName123")))

        val result = sut.addPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.addNameForm.title"))
        doc.toString must include("testPensionName123")
      }
    }
  }

  "submitPensionProviderName" must {
    "redirect to the received first pay page" when {
      "the form submission is valid" in {
        val sut = createSUT

        val expectedCache = Map("pensionProviderName" -> "the pension provider")
        when(addPensionProviderJourneyCacheService.cache(Matchers.eq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))

        val result = sut.submitPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("pensionProviderName", "the pension provider")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.receivedFirstPay().url
      }
    }

    "reload the page with errors" when {
      "the form entry is invalid" in {
        val sut = createSUT
        val result = sut.submitPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("pensionProviderName", "")))

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.addNameForm.title"))
      }
    }

    "store the pensionProvider name in the cache" when {
      "the name is valid" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        Await.result(sut.submitPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("pensionProviderName", "the pension provider"))), 5 seconds)

        verify(addPensionProviderJourneyCacheService, times(1)).cache(mockEq(Map("pensionProviderName" -> "the pension provider")))(any())
      }
    }
  }

  "receivedFirstPay" must {
    "show the first pay choice page" when {
      "the request has an authorised session and no previous value is held in the cache" in {
        val sut = createSUT
        val pensionProviderName = "Pension Provider"
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Seq(pensionProviderName), Seq(None)))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
      }

      "the request has an authorised session and a previous value of 'No' is held in the cache" in {
        val sut = createSUT
        val pensionProviderName = "Pension Provider"
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Seq(pensionProviderName), Seq(Some(NoValue))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
        doc.select("input[id=firstPayChoice-no][checked=checked]").size() mustBe 1
      }
      "the request has an authorised session and a previous value of 'Yes' is held in the cache" in {
        val sut = createSUT
        val pensionProviderName = "Pension Provider"
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Seq(pensionProviderName), Seq(Some(YesValue))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
        doc.select("input[id=firstPayChoice-yes][checked=checked]").size() mustBe 1
      }
    }
  }


  "submit first pay choice" must {

    "redirect user to first payment date page" when {
      "yes is selected" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddPensionProviderFirstPayForm.FirstPayChoice -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate().url
      }
    }

    "redirect user to an error page" when {
      "no is selected (indicating no payment has yet been received)" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddPensionProviderFirstPayForm.FirstPayChoice -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.cantAddPension().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val pensionProviderName = "TEST-Pension-Provider"
        when(addPensionProviderJourneyCacheService.mandatoryValue(Matchers.eq(AddPensionProvider_NameKey))(any())).thenReturn(Future.successful(pensionProviderName))

        val result = sut.submitFirstPay()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddPensionProviderFirstPayForm.FirstPayChoice -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
      }
    }
    "raise an audit event" when {
      "no is selected" in {
        val sut = createSUT
        val pensionProviderName = "TEST-Pension-Provider"
        val nino = generateNino.nino
        when(addPensionProviderJourneyCacheService.mandatoryValue(Matchers.eq(AddPensionProvider_NameKey))(any())).thenReturn(Future.successful(pensionProviderName))

        Await.result(sut.cantAddPension()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          AddPensionProviderFirstPayForm.FirstPayChoice -> NoValue)), 5 seconds)

        verify(auditService, times(1)).createAndSendAuditEvent(Matchers.eq(AddPension_CantAddPensionProvider), Matchers.eq(Map("nino" -> FakeAuthAction.nino.nino)))(Matchers.any(), Matchers.any())
      }
    }
  }

  "addPensionProviderStartDate" must {
    "show the pension start date form page" when {
      "the request has an authorised session and no previously cached date present" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Seq(pensionProviderName), Seq(None)))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.startDateForm.pagetitle"))
        doc.toString must not include ("2037")
      }

      "the request has an authorised session and a previously cached date is present" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(Future.successful(Seq(pensionProviderName), Seq(Some("2037-01-18"))))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.startDateForm.pagetitle"))
        doc.toString must include("2037")
      }
    }

    "return error" when {
      "cache doesn't return data" in {
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any()))
          .thenReturn(Future.successful(Seq.empty, Seq.empty))
        when(addPensionProviderJourneyCacheService.currentValueAs[String](any(), any())(any())).thenReturn(Future.successful(None))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit start date" must {
    "return redirect" when {
      "form is valid" in {
        val sut = createSUT
        val formData = Json.obj(
          sut.pensionStartDateForm.PensionFormDay -> "09",
          sut.pensionStartDateForm.PensionFormMonth -> "06",
          sut.pensionStartDateForm.PensionFormYear -> "2017"
        )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddPensionProvider_NameKey -> "Test")))
        when(addPensionProviderJourneyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url
      }
    }

    "return bad request" when {
      "form is invalid" in {
        val sut = createSUT
        val formData = Json.obj(
          sut.pensionStartDateForm.PensionFormDay -> "01",
          sut.pensionStartDateForm.PensionFormMonth -> "02",
          sut.pensionStartDateForm.PensionFormYear -> (LocalDate.now().getYear + 1).toString
        )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddPensionProvider_NameKey -> "Test")))

        val result = sut.submitPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData))

        status(result) mustBe BAD_REQUEST
      }
    }

    "save details in cache" when {
      "form is valid" in {
        val sut = createSUT
        val formData = Json.obj(
          sut.pensionStartDateForm.PensionFormDay -> "01",
          sut.pensionStartDateForm.PensionFormMonth -> "02",
          sut.pensionStartDateForm.PensionFormYear -> "2017"
        )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(Map(AddPensionProvider_NameKey -> "Test")))
        when(addPensionProviderJourneyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
        Await.result(sut.submitPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("POST").withJsonBody(formData)), 5 seconds)

        verify(addPensionProviderJourneyCacheService, times(1)).cache(Matchers.eq(AddPensionProvider_StartDateKey), Matchers.eq("2017-02-01"))(any())
      }
    }
  }

  "add pension number" must {
    "show the add pension number page" when {
      "the request has an authorised session and no previously cached pension number present" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(AddPensionProvider_NameKey -> pensionProviderName)
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
      }

      "the request has an authorised session and previously cached pension number choice is 'No', and no payroll number is held in cache" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(
          AddPensionProvider_NameKey -> pensionProviderName,
          AddPensionProvider_PayrollNumberChoice -> NoValue)
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK


        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-no][checked=checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached pension number choice is 'No', and a payroll number is held in cache" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(
          AddPensionProvider_NameKey -> pensionProviderName,
          AddPensionProvider_PayrollNumberChoice -> NoValue,
          AddPensionProvider_PayrollNumberKey -> Messages("123456789"))
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK


        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-no][checked=checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached pension number choice is 'Yes' but no payroll number added" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(
          AddPensionProvider_NameKey -> pensionProviderName,
          AddPensionProvider_PayrollNumberChoice -> YesValue)
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-yes][checked=checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""
      }

      "the request has an authorised session and previously cached pension number choice is 'Yes' and payroll number added" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(
          AddPensionProvider_NameKey -> pensionProviderName,
          AddPensionProvider_PayrollNumberChoice -> YesValue,
          AddPensionProvider_PayrollNumberKey -> Messages("123456789"))
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-yes][checked=checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe "123456789"
      }
    }
  }

  "submit pension number" must {
    "cache pension number" when {
      "the form is valid and user knows their pension number" in {
        val sut = createSUT
        val payrollNo = "1234"
        val mapWithPayrollNumber = Map(
          AddPensionProvider_PayrollNumberChoice -> YesValue,
          AddPensionProvider_PayrollNumberKey -> payrollNo
        )
        when(addPensionProviderJourneyCacheService.cache(mockEq(mapWithPayrollNumber))(any())).thenReturn(Future.successful(mapWithPayrollNumber))
        Await.result(sut.submitPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> YesValue, PayrollNumberEntry -> payrollNo)), 5 seconds)

        verify(addPensionProviderJourneyCacheService, times(1)).cache(mockEq(mapWithPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user knows their pension number" in {
        val sut = createSUT
        val payrollNo = "1234"
        val mapWithPayrollNumber = Map(
          AddPensionProvider_PayrollNumberChoice -> YesValue,
          AddPensionProvider_PayrollNumberKey -> payrollNo
        )
        when(addPensionProviderJourneyCacheService.cache(mockEq(mapWithPayrollNumber))(any())).thenReturn(Future.successful(mapWithPayrollNumber))
        val result = sut.submitPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> YesValue, PayrollNumberEntry -> payrollNo))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url
      }
    }


    "cache pension number as not known value" when {
      "the form is valid and user doesn't know its pension number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.notKnown.response")
        val mapWithoutPayrollNumber = Map(
          AddPensionProvider_PayrollNumberChoice -> NoValue,
          AddPensionProvider_PayrollNumberKey -> payrollNo
        )

        when(addPensionProviderJourneyCacheService.cache(mockEq(mapWithoutPayrollNumber))(any())).thenReturn(Future.successful(mapWithoutPayrollNumber))

        Await.result(sut.submitPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> NoValue, PayrollNumberEntry -> "")), 5 seconds)

        verify(addPensionProviderJourneyCacheService, times(1)).cache(mockEq(mapWithoutPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user doesn't know its pension number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.notKnown.response")
        val mapWithoutPayrollNumber = Map(
          AddPensionProvider_PayrollNumberChoice -> NoValue,
          AddPensionProvider_PayrollNumberKey -> payrollNo
        )

        when(addPensionProviderJourneyCacheService.cache(mockEq(mapWithoutPayrollNumber))(any())).thenReturn(Future.successful(mapWithoutPayrollNumber))

        val result = sut.submitPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> NoValue, PayrollNumberEntry -> ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val pensionName = "TEST"
        val cache = Map(AddPensionProvider_NameKey -> pensionName, AddPensionProvider_StartDateWithinSixWeeks -> YesValue)
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.submitPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          PayrollNumberChoice -> YesValue, PayrollNumberEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
      }
    }
  }

  "add telephone number" must {
    "show the contact by telephone page" when {
      "the request has an authorised session and no previously cached pension number present" in {
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(None, None)))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'No', and no telephone number is held in cache" in {
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(Some(NoValue), None)))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'No', and a telephone number is held in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(Some(NoValue), Some("01215485965"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'Yes', and no telephone number is held in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(Some(YesValue), None)))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'Yes', and a telephone number is held in cache" in {
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.optionalValues(any())(any())).thenReturn(Future.successful(Seq(Some(YesValue), Some("01215485965"))))

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

        val expectedCache = Map(AddPensionProvider_TelephoneQuestionKey -> YesValue, AddPensionProvider_TelephoneNumberKey -> "12345678")
        when(addPensionProviderJourneyCacheService.cache(mockEq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))
        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.checkYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber = Map(AddPensionProvider_TelephoneQuestionKey -> NoValue, AddPensionProvider_TelephoneNumberKey -> "")
        when(addPensionProviderJourneyCacheService.cache(mockEq(expectedCacheWithErasingNumber))(any())).thenReturn(Future.successful(expectedCacheWithErasingNumber))
        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> NoValue, YesNoTextEntry -> "this value must not be cached"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.checkYourAnswers().url
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
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("a pension provider", "2017-06-15", "pension-ref-1234", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )

        val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }

    "result in an error response" when {
      "mandatory values are absent from the journey cache" in {
        val mockedJCExceptionMsg = "The mandatory value under key <some key> was not found in the journey cache for add-pension-provider   BBBB"
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenThrow(
          new RuntimeException(mockedJCExceptionMsg)
        )

        val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit your answers" must {
    "redirect to the confirmation page" in {
      val sut = createSUT

      val expectedModel = AddPensionProvider("a pension provider", LocalDate.parse("2017-06-09"), "pension-ref-1234", "Yes", Some("123456789"))

      when(pensionProviderService.addPensionProvider(any(), Matchers.eq(expectedModel))(any())).thenReturn(Future.successful("envelope-123"))
      when(addPensionProviderJourneyCacheService.collectedValues(any(), any())(any())).thenReturn(
        Future.successful(
          Seq[String]("a pension provider", "2017-06-09", "pension-ref-1234", "Yes"),
          Seq[Option[String]](Some("123456789"))
        ))
      when(trackSuccessJourneyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
      when(addPensionProviderJourneyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController.confirmation().url
    }
  }

  "confirmation" must {
    "show the add pension confirmation page" in {
      val sut = createSUT

      val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.pensionConfirmation.heading"))
    }
  }

  "cancel" must {
    "redirect to the the TaxAccountSummaryController" in {

      when(addPensionProviderJourneyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = createSUT.cancel()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createSUT = new SUT

  val generateNino: Nino = new Generator().nextNino

  val pensionProviderService = mock[PensionProviderService]
  val auditService = mock[AuditService]
  val personService = mock[PersonService]
  val addPensionProviderJourneyCacheService = mock[JourneyCacheService]
  val trackSuccessJourneyCacheService = mock[JourneyCacheService]

  private class SUT extends AddPensionProviderController(
    pensionProviderService,
    auditService,
    mock[AuditConnector],
    FakeAuthAction,
    FakeValidatePerson,
    addPensionProviderJourneyCacheService,
    trackSuccessJourneyCacheService,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    val pensionStartDateForm = PensionAddDateForm("pension provider")
  }

}
