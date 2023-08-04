/*
 * Copyright 2023 HM Revenue & Customs
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

import akka.Done
import builders.RequestBuilder
import controllers.FakeAuthAction
import controllers.actions.FakeValidatePerson
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm
import uk.gov.hmrc.tai.model.domain.AddEmployment
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.AddEmploymentPayrollNumberConstants._
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{AddEmploymentFirstPayChoiceConstants, AuditConstants, FormValuesConstants}
import utils.BaseSpec
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class AddEmploymentControllerSpec extends BaseSpec with BeforeAndAfterEach {

  private def createSUT = new SUT

  val auditService: AuditService = mock[AuditService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val addEmploymentJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val trackSuccessJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  private class SUT
      extends AddEmploymentController(
        auditService,
        employmentService,
        FakeAuthAction,
        FakeValidatePerson,
        addEmploymentJourneyCacheService,
        trackSuccessJourneyCacheService,
        mock[AuditConnector],
        mcc,
        inject[AddEmploymentStartDateFormView],
        inject[AddEmploymentNameFormView],
        inject[AddEmploymentFirstPayFormView],
        inject[AddEmploymentErrorPageView],
        inject[AddEmploymentPayrollNumberFormView],
        inject[CanWeContactByPhoneView],
        inject[ConfirmationView],
        inject[AddIncomeCheckYourAnswersView]
      )

  override def beforeEach(): Unit =
    Mockito.reset(addEmploymentJourneyCacheService)

  "addEmploymentName" must {
    "show the employment name form page" when {
      "the request has an authorised session and no previously supplied employment name is present in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.currentValue(meq(AddEmploymentConstants.NameKey))(any()))
          .thenReturn(Future.successful(None))
        val result = sut.addEmploymentName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.addNameForm.title"))
      }
      "the request has an authorised session and a previously supplied employment name is present in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.currentValue(meq(AddEmploymentConstants.NameKey))(any()))
          .thenReturn(Future.successful(Some("employer one plc")))
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
        when(addEmploymentJourneyCacheService.cache(meq(expectedCache))(any()))
          .thenReturn(Future.successful(expectedCache))

        val result = sut.submitEmploymentName()(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("employmentName", "the employer"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url
      }
    }

    "reload the page with errors" when {
      "the form entry is invalid" in {
        val sut = createSUT
        val result = sut.submitEmploymentName()(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(("employmentName", ""))
        )

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
        when(addEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right((Seq(employmentName), Seq(None)))))

        val result = sut.addEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.startDateForm.pagetitle"))
        doc.select("#tellUsStartDateForm-year").get(0).attributes.get("value") mustBe ""
      }

      "the request has an authorised session and a previously supplied start date is present in cache" in {
        val sut = createSUT
        val employmentName = "TEST"
        when(addEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right((Seq(employmentName), Seq(Some("2017-12-12"))))))

        val result = sut.addEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.startDateForm.pagetitle"))
        doc.select("#tellUsStartDateForm-year").get(0).attributes.get("value") mustBe "2017"
      }

      "redirect to the tax summary page if a value is missing from the cache " in {

        val sut = createSUT
        when(addEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(Future.successful(Left("Mandatory value missing from cache")))

        val result = sut.addEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }

  }

  "submit start date" must {
    "return redirect" when {
      "form is valid and start date is over 6 weeks ago" in {
        val sut = createSUT

        when(addEmploymentJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddEmploymentConstants.NameKey -> "Test")))

        val result =
          sut.submitEmploymentStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                EmploymentAddDateForm.EmploymentFormDay   -> "01",
                EmploymentAddDateForm.EmploymentFormMonth -> "02",
                EmploymentAddDateForm.EmploymentFormYear  -> "2017"
              )
          )
        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url
      }

      "form is valid and start date is less than 6 weeks" in {
        val sut = createSUT
        val date = LocalDate.now.minusDays(4)

        when(addEmploymentJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddEmploymentConstants.NameKey -> "Test")))

        val result =
          sut.submitEmploymentStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                EmploymentAddDateForm.EmploymentFormDay   -> date.getDayOfMonth.toString,
                EmploymentAddDateForm.EmploymentFormMonth -> date.getMonthValue.toString,
                EmploymentAddDateForm.EmploymentFormYear  -> date.getYear.toString
              )
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController
          .receivedFirstPay()
          .url
      }
    }

    "return bad request" when {
      "form is invalid" in {
        val sut = createSUT

        when(addEmploymentJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddEmploymentConstants.NameKey -> "Test")))

        val result =
          sut.submitEmploymentStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                EmploymentAddDateForm.EmploymentFormDay   -> "01",
                EmploymentAddDateForm.EmploymentFormMonth -> "02",
                EmploymentAddDateForm.EmploymentFormYear  -> (LocalDate.now().getYear + 1).toString
              )
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "save details in cache" when {
      "form is valid and start date is over 6 weeks ago" in {
        val sut = createSUT

        when(addEmploymentJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddEmploymentConstants.NameKey -> "Test")))
        val result =
          sut.submitEmploymentStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                EmploymentAddDateForm.EmploymentFormDay   -> "01",
                EmploymentAddDateForm.EmploymentFormMonth -> "02",
                EmploymentAddDateForm.EmploymentFormYear  -> "2017"
              )
          )

        status(result) mustBe SEE_OTHER
        verify(addEmploymentJourneyCacheService, times(1)).cache(
          meq(
            Map(
              AddEmploymentConstants.NameKey                 -> "Test",
              AddEmploymentConstants.StartDateKey            -> "2017-02-01",
              AddEmploymentConstants.StartDateWithinSixWeeks -> FormValuesConstants.NoValue
            )
          )
        )(any())
      }

      "form is valid and start date is less than 6 weeks" in {
        val sut = createSUT
        val date = LocalDate.now.minusDays(4)

        when(addEmploymentJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddEmploymentConstants.NameKey -> "Test")))
        val result =
          sut.submitEmploymentStartDate()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                EmploymentAddDateForm.EmploymentFormDay   -> date.getDayOfMonth.toString,
                EmploymentAddDateForm.EmploymentFormMonth -> date.getMonthValue.toString,
                EmploymentAddDateForm.EmploymentFormYear  -> date.getYear.toString
              )
          )

        status(result) mustBe SEE_OTHER
        verify(addEmploymentJourneyCacheService, times(1)).cache(
          meq(
            Map(
              AddEmploymentConstants.NameKey                 -> "Test",
              AddEmploymentConstants.StartDateKey            -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
              AddEmploymentConstants.StartDateWithinSixWeeks -> FormValuesConstants.YesValue
            )
          )
        )(any())
      }
    }
  }

  "received First Pay" must {
    "show the first pay choice page" when {
      "the request has an authorised session and no previous response is held in cache" in {
        val sut = createSUT
        val employmentName = "TEST"
        when(
          addEmploymentJourneyCacheService.collectedJourneyValues(
            meq(Seq(AddEmploymentConstants.NameKey)),
            meq(Seq(AddEmploymentConstants.ReceivedFirstPayKey))
          )(any(), any())
        )
          .thenReturn(Future.successful(Right((Seq(employmentName), Seq(None)))))

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
        when(
          addEmploymentJourneyCacheService.collectedJourneyValues(
            meq(Seq(AddEmploymentConstants.NameKey)),
            meq(Seq(AddEmploymentConstants.ReceivedFirstPayKey))
          )(any(), any())
        ).thenReturn(Future.successful(Right((Seq(employmentName), Seq(Some(FormValuesConstants.YesValue))))))

        val result = sut.receivedFirstPay()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentFirstPay.title", employmentName))
        doc.select("input[id=firstPayChoice][checked]").size() mustBe 1
        doc.select("input[id=firstPayChoice-2][checked]").size() mustBe 0
      }
    }
  }

  "submit first pay choice" must {

    "redirect user to payroll number page" when {
      "yes is selected" in {
        val sut = createSUT
        when(
          addEmploymentJourneyCacheService
            .cache(meq(AddEmploymentConstants.ReceivedFirstPayKey), any())(any())
        )
          .thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddEmploymentFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.YesValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url
      }
    }

    "redirect user to error page" when {
      "no is selected" in {
        val sut = createSUT

        when(
          addEmploymentJourneyCacheService
            .cache(meq(AddEmploymentConstants.ReceivedFirstPayKey), any())(any())
        )
          .thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddEmploymentFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.NoValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.sixWeeksError().url
      }
    }

    "raise an audit event" when {
      "no is selected" in {
        val sut = createSUT
        val employmentName = "TEST-Employer"
        when(addEmploymentJourneyCacheService.mandatoryJourneyValue(meq(AddEmploymentConstants.NameKey))(any()))
          .thenReturn(Future.successful(Right(employmentName)))

        Await.result(
          sut.sixWeeksError()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                AddEmploymentFirstPayChoiceConstants.FirstPayChoice -> FormValuesConstants.NoValue
              )
          ),
          5 seconds
        )

        verify(auditService, times(1)).createAndSendAuditEvent(
          meq(AuditConstants.AddEmploymentCantAddEmployer),
          meq(Map("nino" -> FakeAuthAction.nino.nino))
        )(any(), any())
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val employmentName = "TEST-Employer"

        when(addEmploymentJourneyCacheService.mandatoryJourneyValue(meq(AddEmploymentConstants.NameKey))(any()))
          .thenReturn(Future.successful(Right(employmentName)))

        val result = sut.submitFirstPay()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(AddEmploymentFirstPayChoiceConstants.FirstPayChoice -> "")
        )
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
        val cache = Map(
          AddEmploymentConstants.NameKey                 -> employerName,
          AddEmploymentConstants.StartDateWithinSixWeeks -> FormValuesConstants.YesValue
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice][checked=checked]").size() mustBe 0
        doc.select("input[id=payrollNumberChoice-2][checked=checked]").size() mustBe 0
        doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'no' response is held in cache" in {
        val sut = createSUT
        val employerName = "TEST"
        val cache = Map(
          AddEmploymentConstants.NameKey                  -> employerName,
          AddEmploymentConstants.StartDateWithinSixWeeks  -> FormValuesConstants.YesValue,
          AddEmploymentConstants.PayrollNumberQuestionKey -> FormValuesConstants.NoValue,
          AddEmploymentConstants.PayrollNumberKey         -> "should be ignored"
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 0
        doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'yes' response is held in cache" in {
        val sut = createSUT
        val employerName = "TEST"
        val cache = Map(
          AddEmploymentConstants.NameKey                  -> employerName,
          AddEmploymentConstants.StartDateWithinSixWeeks  -> FormValuesConstants.YesValue,
          AddEmploymentConstants.PayrollNumberQuestionKey -> FormValuesConstants.YesValue,
          AddEmploymentConstants.PayrollNumberKey         -> "should be displayed"
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.addEmploymentPayrollNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
        doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 1
        doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 0
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
          AddEmploymentConstants.PayrollNumberQuestionKey -> FormValuesConstants.YesValue,
          AddEmploymentConstants.PayrollNumberKey         -> payrollNo
        )
        when(addEmploymentJourneyCacheService.cache(meq(mapWithPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithPayrollNumber))
        Await.result(
          sut.submitEmploymentPayrollNumber()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(
                PayrollNumberChoice -> FormValuesConstants.YesValue,
                PayrollNumberEntry  -> payrollNo
              )
          ),
          5 seconds
        )

        verify(addEmploymentJourneyCacheService, times(1)).cache(meq(mapWithPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user knows their payroll number" in {
        val sut = createSUT
        val payrollNo = "1234"
        val mapWithPayrollNumber = Map(
          AddEmploymentConstants.PayrollNumberQuestionKey -> FormValuesConstants.YesValue,
          AddEmploymentConstants.PayrollNumberKey         -> payrollNo
        )
        when(addEmploymentJourneyCacheService.cache(meq(mapWithPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithPayrollNumber))
        val result = sut.submitEmploymentPayrollNumber()(
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
        ).get mustBe controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url
      }
    }

    "cache payroll number as not known value" when {
      "the form is valid and user doesn't know its payroll number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.addEmployment.employmentPayrollNumber.notKnown")
        val mapWithPayrollNumber = Map(
          AddEmploymentConstants.PayrollNumberQuestionKey -> FormValuesConstants.NoValue,
          AddEmploymentConstants.PayrollNumberKey         -> payrollNo
        )

        when(addEmploymentJourneyCacheService.cache(meq(mapWithPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithPayrollNumber))

        Await.result(
          sut.submitEmploymentPayrollNumber()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.NoValue, PayrollNumberEntry -> "")
          ),
          5 seconds
        )

        verify(addEmploymentJourneyCacheService, times(1)).cache(meq(mapWithPayrollNumber))(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user doesn't know its payroll number" in {
        val sut = createSUT
        val payrollNo = Messages("tai.addEmployment.employmentPayrollNumber.notKnown")
        val mapWithPayrollNumber = Map(
          AddEmploymentConstants.PayrollNumberQuestionKey -> FormValuesConstants.NoValue,
          AddEmploymentConstants.PayrollNumberKey         -> payrollNo
        )

        when(addEmploymentJourneyCacheService.cache(meq(mapWithPayrollNumber))(any()))
          .thenReturn(Future.successful(mapWithPayrollNumber))

        val result = sut.submitEmploymentPayrollNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.NoValue, PayrollNumberEntry -> "")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT
        val employerName = "TEST"
        val cache = Map(
          AddEmploymentConstants.NameKey                 -> employerName,
          AddEmploymentConstants.StartDateWithinSixWeeks -> FormValuesConstants.YesValue
        )
        when(addEmploymentJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.submitEmploymentPayrollNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.YesValue, PayrollNumberEntry -> "")
        )
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
        when(addEmploymentJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(None, None)))
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
        when(addEmploymentJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(FormValuesConstants.NoValue), Some("should be ignored"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'yes' response is held in cache" in {
        val sut = createSUT
        when(addEmploymentJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some(FormValuesConstants.YesValue), Some("should be displayed"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 1
        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe "should be displayed"
      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT

        val expectedCache =
          Map(
            AddEmploymentConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            AddEmploymentConstants.TelephoneNumberKey   -> "12345678"
          )
        when(addEmploymentJourneyCacheService.cache(any())(any()))
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
        ).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers().url
      }
      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber =
          Map(
            AddEmploymentConstants.TelephoneQuestionKey -> FormValuesConstants.NoValue,
            AddEmploymentConstants.TelephoneNumberKey   -> ""
          )
        when(addEmploymentJourneyCacheService.cache(any())(any()))
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
        ).get mustBe controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers().url
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
        when(addEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
          Future.successful(
            Right(
              (
                Seq[String]("emp-name", "2017-06-15", "emp-ref-1234", "Yes"),
                Seq[Option[String]](Some("123456789"))
              )
            )
          )
        )

        val result = sut.addEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      val sut = createSUT
      when(
        addEmploymentJourneyCacheService.collectedJourneyValues(
          any(classOf[scala.collection.immutable.List[String]]),
          any(classOf[scala.collection.immutable.List[String]])
        )(any(), any())
      ).thenReturn(
        Future.successful(Left("An error has occurred"))
      )

      val result = sut.addEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submit your answers" must {
    "invoke the back end 'addEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session amd a telephone number has been provided" in {
        val sut = createSUT

        val expectedModel =
          AddEmployment("empName", LocalDate.parse("2017-04-04"), "I do not know", "Yes", Some("123456789"))

        when(addEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
          Future.successful(
            Right(
              (
                Seq("empName", "2017-04-04", "I do not know", "Yes"),
                Seq(Some("123456789"))
              )
            )
          )
        )

        when(employmentService.addEmployment(any(), meq(expectedModel))(any(), any()))
          .thenReturn(Future.successful("envelope-123"))
        when(addEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))
        when(
          trackSuccessJourneyCacheService
            .cache(meq(TrackSuccessfulJourneyConstants.AddEmploymentKey), meq("true"))(any())
        )
          .thenReturn(Future.successful(Map(TrackSuccessfulJourneyConstants.AddEmploymentKey -> "true")))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.confirmation().url
      }

      "the request has an authorised session amd no telephone number was provided" in {
        val sut = createSUT

        val expectedModel = AddEmployment("empName", LocalDate.parse("2017-04-04"), "I do not know", "No", None)
        val expectedSuccessfulJourneyCache = Map("addEmployment" -> "true")

        when(addEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right((Seq("empName", "2017-04-04", "I do not know", "No"), Seq(None)))))

        when(employmentService.addEmployment(any(), meq(expectedModel))(any(), any()))
          .thenReturn(Future.successful("envelope-123"))
        when(addEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))
        when(trackSuccessJourneyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(expectedSuccessfulJourneyCache))

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

      when(addEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

      val result = createSUT.cancel()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }
}
