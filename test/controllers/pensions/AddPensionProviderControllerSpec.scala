/*
 * Copyright 2024 HM Revenue & Customs
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

import akka.Done
import builders.RequestBuilder
import controllers.ErrorPagesHandler
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.pensions.PensionAddDateForm
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.AddPensionNumberConstants._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{AddPensionFirstPayChoiceConstants, AuditConstants, FormValuesConstants}
import utils.BaseSpec
import views.html.CanWeContactByPhoneView
import views.html.pensions._

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class AddPensionProviderControllerSpec extends BaseSpec {

  private def createSUT = new SUT

  val pensionProviderService: PensionProviderService = mock[PensionProviderService]
  val auditService: AuditService = mock[AuditService]
  val personService: PersonService = mock[PersonService]
  val addPensionProviderJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val trackSuccessJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  private class SUT
      extends AddPensionProviderController(
        pensionProviderService,
        auditService,
        mock[AuditConnector],
        mockAuthJourney,
        mcc,
        inject[CanWeContactByPhoneView],
        inject[AddPensionConfirmationView],
        inject[AddPensionCheckYourAnswersView],
        inject[AddPensionNumberView],
        inject[AddPensionErrorView],
        inject[AddPensionReceivedFirstPayView],
        inject[AddPensionNameView],
        inject[AddPensionStartDateView],
        addPensionProviderJourneyCacheService,
        trackSuccessJourneyCacheService,
        inject[ErrorPagesHandler]
      ) {}

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(addPensionProviderJourneyCacheService)
  }

  "addPensionProviderName" must {
    "show the pensionProvider name form page" when {
      "the request has an authorised session and no previous value in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.currentValue(meq(AddPensionProviderConstants.NameKey))(any()))
          .thenReturn(Future.successful(None))

        val result = sut.addPensionProviderName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.addNameForm.title"))
        doc.toString must not include "testPensionName123"
      }
    }
  }

  "addPensionProviderName" must {
    "show the pensionProvider name form page" when {
      "the request has an authorised session and previous value exists in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.currentValue(meq(AddPensionProviderConstants.NameKey))(any()))
          .thenReturn(Future.successful(Some("testPensionName123")))

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
        when(addPensionProviderJourneyCacheService.cache(meq(expectedCache))(any()))
          .thenReturn(Future.successful(expectedCache))

        val result = sut.submitPensionProviderName()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("pensionProviderName", "the pension provider"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.receivedFirstPay().url
      }
    }

    "reload the page with errors" when {
      "the form entry is invalid" in {
        val sut = createSUT
        val result = sut.submitPensionProviderName()(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("pensionProviderName", ""))
        )

        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.addNameForm.title"))
      }
    }

    "store the pensionProvider name in the cache" when {
      "the name is valid" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        Await.result(
          sut.submitPensionProviderName()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(("pensionProviderName", "the pension provider"))
          ),
          5 seconds
        )

        verify(addPensionProviderJourneyCacheService, times(1))
          .cache(meq(Map("pensionProviderName" -> "the pension provider")))(any())
      }
    }
  }

  "receivedFirstPay" must {
    "show the first pay choice page" when {
      "the request has an authorised session and no previous value is held in the cache" in {
        val sut = createSUT
        val pensionProviderName = "Pension Provider"

        val mandatorySeq = List(AddPensionProviderConstants.NameKey)
        val optionalSeq = List(AddPensionProviderConstants.FirstPaymentKey)

        when(
          addPensionProviderJourneyCacheService
            .collectedJourneyValues(meq(mandatorySeq), meq(optionalSeq))(any(), any())
        )
          .thenReturn(Future.successful(Right((Seq(pensionProviderName), Seq(None)))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
      }

      "the request has an authorised session and a previous value of 'No' is held in the cache" in {
        val sut = createSUT
        val pensionProviderName = "Pension Provider"

        val mandatorySeq = List(AddPensionProviderConstants.NameKey)
        val optionalSeq = List(AddPensionProviderConstants.FirstPaymentKey)

        when(
          addPensionProviderJourneyCacheService
            .collectedJourneyValues(meq(mandatorySeq), meq(optionalSeq))(any(), any())
        )
          .thenReturn(Future.successful(Right((Seq(pensionProviderName), Seq(Some(FormValuesConstants.NoValue))))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithOnlySession("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
        doc.select("input[id=firstPayChoice-2][checked]").size() mustBe 1
      }

      "the request has an authorised session and a previous value of 'Yes' is held in the cache" in {
        val sut = createSUT
        val pensionProviderName = "Pension Provider"

        val mandatorySeq = List(AddPensionProviderConstants.NameKey)
        val optionalSeq = List(AddPensionProviderConstants.FirstPaymentKey)

        when(
          addPensionProviderJourneyCacheService
            .collectedJourneyValues(meq(mandatorySeq), meq(optionalSeq))(any(), any())
        )
          .thenReturn(Future.successful(Right((Seq(pensionProviderName), Seq(Some(FormValuesConstants.YesValue))))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
        doc.select("input[id=firstPayChoice][checked]").size() mustBe 1
      }

      "redirect to the tax summary page if a value is missing from the cache " in {

        val sut = createSUT

        val mandatorySeq = List(AddPensionProviderConstants.NameKey)
        val optionalSeq = List(AddPensionProviderConstants.FirstPaymentKey)

        when(
          addPensionProviderJourneyCacheService
            .collectedJourneyValues(meq(mandatorySeq), meq(optionalSeq))(any(), any())
        )
          .thenReturn(Future.successful(Left("Data missing from the cache")))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }

    }
  }

  "submit first pay choice" must {

    "redirect user to first payment date page" when {
      "yes is selected" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.YesValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate().url
      }
    }

    "redirect user to an error page" when {
      "no is selected (indicating no payment has yet been received)" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.NoValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.AddPensionProviderController
          .cantAddPension()
          .url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val pensionProviderName = "TEST-Pension-Provider"
        when(
          addPensionProviderJourneyCacheService.mandatoryJourneyValue(meq(AddPensionProviderConstants.NameKey))(
            any()
          )
        )
          .thenReturn(Future.successful(Right(pensionProviderName)))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> "")
        )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.firstPay.pagetitle"))
      }
    }
    "raise an audit event" when {
      "no is selected" in {
        val sut = createSUT
        val pensionProviderName = "TEST-Pension-Provider"
        when(
          addPensionProviderJourneyCacheService.mandatoryJourneyValue(meq(AddPensionProviderConstants.NameKey))(
            any()
          )
        )
          .thenReturn(Future.successful(Right(pensionProviderName)))

        Await.result(
          sut.cantAddPension()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(AddPensionFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.NoValue)
          ),
          5 seconds
        )

        verify(auditService, times(1)).createAndSendAuditEvent(
          meq(AuditConstants.AddPensionCantAddPensionProvider),
          meq(Map("nino" -> nino.nino))
        )(any(), any())
      }
    }
  }

  "addPensionProviderStartDate" must {
    "show the pension start date form page" when {
      "the request has an authorised session and no previously cached date present" in {
        val sut = createSUT

        val pensionProviderName = "TEST"
        val mandatorySequence = List(AddPensionProviderConstants.NameKey)
        val optionalSequence = List(AddPensionProviderConstants.StartDateKey)

        when(
          addPensionProviderJourneyCacheService
            .collectedJourneyValues(meq(mandatorySequence), meq(optionalSequence))(any(), any())
        )
          .thenReturn(Future.successful(Right((Seq(pensionProviderName), Seq(None)))))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.startDateForm.pagetitle"))
        doc.toString must not include "2037"
      }

      "the request has an authorised session and a previously cached date is present" in {
        val sut = createSUT
        val pensionProviderName = "TEST"

        val mandatorySequence = List(AddPensionProviderConstants.NameKey)
        val optionalSequence = List(AddPensionProviderConstants.StartDateKey)

        when(
          addPensionProviderJourneyCacheService
            .collectedJourneyValues(meq(mandatorySequence), meq(optionalSequence))(any(), any())
        )
          .thenReturn(Future.successful(Right((Seq(pensionProviderName), Seq(Some("2037-01-18"))))))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.startDateForm.pagetitle"))
        doc.toString must include("2037")
      }

      "redirect to the tax summary page if a value is missing from the cache " in {

        val mandatorySequence = List(AddPensionProviderConstants.NameKey)
        val optionalSequence = List(AddPensionProviderConstants.StartDateKey)
        val sut = createSUT

        when(
          addPensionProviderJourneyCacheService
            .collectedJourneyValues(meq(mandatorySequence), meq(optionalSequence))(any(), any())
        )
          .thenReturn(Future.successful(Left("Data missing from the cache")))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }
    }

    "return error" when {
      "cache throws an exception" in {
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("An error occurred")))
        when(addPensionProviderJourneyCacheService.currentValueAs[String](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = sut.addPensionProviderStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit start date" must {
    "return redirect" when {
      "form is valid" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddPensionProviderConstants.NameKey -> "Test")))
        when(addPensionProviderJourneyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))

        val result =
          sut.submitPensionProviderStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PensionAddDateForm.PensionFormDay   -> "09",
                PensionAddDateForm.PensionFormMonth -> "06",
                PensionAddDateForm.PensionFormYear  -> "2017"
              )
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url
      }
    }

    "return bad request" when {
      "form is invalid" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddPensionProviderConstants.NameKey -> "Test")))

        val result =
          sut.submitPensionProviderStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PensionAddDateForm.PensionFormDay   -> "01",
                PensionAddDateForm.PensionFormMonth -> "02",
                PensionAddDateForm.PensionFormYear  -> (LocalDate.now().getYear + 1).toString
              )
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "save details in cache" when {
      "form is valid" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddPensionProviderConstants.NameKey -> "Test")))
        when(addPensionProviderJourneyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map.empty[String, String]))
        Await.result(
          sut.submitPensionProviderStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PensionAddDateForm.PensionFormDay   -> "01",
                PensionAddDateForm.PensionFormMonth -> "02",
                PensionAddDateForm.PensionFormYear  -> "2017"
              )
          ),
          5 seconds
        )

        verify(addPensionProviderJourneyCacheService, times(1))
          .cache(meq(AddPensionProviderConstants.StartDateKey), meq("2017-02-01"))(any())
      }
    }
  }

  "add pension number" must {
    "show the add pension number page" when {
      "the request has an authorised session and no previously cached pension number present" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(AddPensionProviderConstants.NameKey -> pensionProviderName)
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
      }

      "the request has an authorised session and previously cached pension number choice is 'No', and no payroll number is held in cache" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache =
          Map(
            AddPensionProviderConstants.NameKey             -> pensionProviderName,
            AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.NoValue
          )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached pension number choice is 'No', and a payroll number is held in cache" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(
          AddPensionProviderConstants.NameKey             -> pensionProviderName,
          AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.NoValue,
          AddPensionProviderConstants.PayrollNumberKey    -> Messages("123456789")
        )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached pension number choice is 'Yes' but no payroll number added" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache =
          Map(
            AddPensionProviderConstants.NameKey             -> pensionProviderName,
            AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.YesValue
          )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes().get("value") mustBe ""
      }

      "the request has an authorised session and previously cached pension number choice is 'Yes' and payroll number added" in {
        val sut = createSUT
        val pensionProviderName = "TEST"
        val cache = Map(
          AddPensionProviderConstants.NameKey             -> pensionProviderName,
          AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.YesValue,
          AddPensionProviderConstants.PayrollNumberKey    -> Messages("123456789")
        )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addPensionNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addPensionProvider.pensionNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 1
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
          AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.YesValue,
          AddPensionProviderConstants.PayrollNumberKey    -> payrollNo
        )
        when(addPensionProviderJourneyCacheService.cache(meq(mapWithPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithPayrollNumber))
        Await.result(
          sut.submitPensionNumber()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PayrollNumberChoice -> FormValuesConstants.YesValue,
                PayrollNumberEntry  -> payrollNo
              )
          ),
          5 seconds
        )

        verify(addPensionProviderJourneyCacheService, times(1)).cache(meq(mapWithPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user knows their pension number" in {
        val sut = createSUT
        val payrollNo = "1234"
        val mapWithPayrollNumber = Map(
          AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.YesValue,
          AddPensionProviderConstants.PayrollNumberKey    -> payrollNo
        )
        when(addPensionProviderJourneyCacheService.cache(meq(mapWithPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithPayrollNumber))
        val result = sut.submitPensionNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              PayrollNumberChoice -> FormValuesConstants.YesValue,
              PayrollNumberEntry  -> payrollNo
            )
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url
      }
    }

    "cache pension number as not known value" when {
      "the form is valid and user doesn't know its pension number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.notKnown.response")
        val mapWithoutPayrollNumber = Map(
          AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.NoValue,
          AddPensionProviderConstants.PayrollNumberKey    -> payrollNo
        )

        when(addPensionProviderJourneyCacheService.cache(meq(mapWithoutPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithoutPayrollNumber))

        Await.result(
          sut.submitPensionNumber()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.NoValue, PayrollNumberEntry -> "")
          ),
          5 seconds
        )

        verify(addPensionProviderJourneyCacheService, times(1)).cache(meq(mapWithoutPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user doesn't know its pension number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.notKnown.response")
        val mapWithoutPayrollNumber = Map(
          AddPensionProviderConstants.PayrollNumberChoice -> FormValuesConstants.NoValue,
          AddPensionProviderConstants.PayrollNumberKey    -> payrollNo
        )

        when(addPensionProviderJourneyCacheService.cache(meq(mapWithoutPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithoutPayrollNumber))

        val result = sut.submitPensionNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.NoValue, PayrollNumberEntry -> "")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val pensionName = "TEST"
        val cache =
          Map(
            AddPensionProviderConstants.NameKey                 -> pensionName,
            AddPensionProviderConstants.StartDateWithinSixWeeks -> FormValuesConstants.YesValue
          )
        when(addPensionProviderJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.submitPensionNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.YesValue, PayrollNumberEntry -> "")
        )
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
        when(addPensionProviderJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(None, None)))

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
        when(addPensionProviderJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(FormValuesConstants.NoValue), None)))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'No', and a telephone number is held in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(FormValuesConstants.NoValue), Some("01215485965"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'Yes', and no telephone number is held in cache" in {
        val sut = createSUT

        when(addPensionProviderJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(FormValuesConstants.YesValue), None)))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""

      }

      "the request has an authorised session and previously cached telephone number choice is 'Yes', and a telephone number is held in cache" in {
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(FormValuesConstants.YesValue), Some("01215485965"))))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))

        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe "01215485965"

      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT

        val expectedCache =
          Map(
            AddPensionProviderConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            AddPensionProviderConstants.TelephoneNumberKey   -> "12345678"
          )
        when(addPensionProviderJourneyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(expectedCache))
        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "12345678"
            )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.checkYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber =
          Map(
            AddPensionProviderConstants.TelephoneQuestionKey -> FormValuesConstants.NoValue,
            AddPensionProviderConstants.TelephoneNumberKey   -> ""
          )
        when(addPensionProviderJourneyCacheService.cache(any())(any()))
          .thenReturn(Future.successful(expectedCacheWithErasingNumber))
        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.NoValue,
              FormValuesConstants.YesNoTextEntry -> "this value must not be cached"
            )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.AddPensionProviderController.checkYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT

        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> ""
            )
        )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
      "there is a form validation error (additional, controller specific constraint)" in {
        val sut = createSUT

        val tooFewCharsResult = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234"
            )
        )
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234123412341234123412341234123"
            )
        )
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
        when(addPensionProviderJourneyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
          Future.successful(
            Right(
              (
                Seq[String]("a pension provider", "2017-06-15", "pension-ref-1234", "Yes"),
                Seq[Option[String]](Some("123456789"))
              )
            )
          )
        )

        val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      val sut = createSUT
      when(
        addPensionProviderJourneyCacheService.collectedJourneyValues(
          any(classOf[scala.collection.immutable.List[String]]),
          any(classOf[scala.collection.immutable.List[String]])
        )(any(), any())
      ).thenReturn(
        Future.successful(Left("An error has occurred"))
      )

      val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }

    "result in an error response" when {
      "mandatory values are absent from the journey cache" in {
        val mockedJCExceptionMsg =
          "The mandatory value under key <some key> was not found in the journey cache for add-pension-provider   BBBB"
        val sut = createSUT
        when(addPensionProviderJourneyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
          Future.failed(new RuntimeException(mockedJCExceptionMsg))
        )

        val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit your answers" must {
    "redirect to the confirmation page" in {
      val sut = createSUT

      val expectedModel = AddPensionProvider(
        "a pension provider",
        LocalDate.parse("2017-06-09"),
        "pension-ref-1234",
        "Yes",
        Some("123456789")
      )

      when(pensionProviderService.addPensionProvider(any(), meq(expectedModel))(any(), any()))
        .thenReturn(Future.successful("envelope-123"))
      when(addPensionProviderJourneyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
        Future.successful(
          Right(
            (
              Seq[String]("a pension provider", "2017-06-09", "pension-ref-1234", "Yes"),
              Seq[Option[String]](Some("123456789"))
            )
          )
        )
      )
      when(trackSuccessJourneyCacheService.cache(any(), any())(any()))
        .thenReturn(Future.successful(Map.empty[String, String]))
      when(addPensionProviderJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

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

      when(addPensionProviderJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

      val result = createSUT.cancel()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }

}
