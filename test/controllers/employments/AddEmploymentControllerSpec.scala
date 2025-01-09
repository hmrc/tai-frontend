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

package controllers.employments

import builders.RequestBuilder
import controllers.ErrorPagesHandler
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, times, verify, when}
import pages.addEmployment._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.employments.EmploymentAddDateForm
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.AddEmployment
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.AddEmploymentPayrollNumberConstants._
import uk.gov.hmrc.tai.util.constants.{AddEmploymentFirstPayChoiceConstants, FormValuesConstants}
import utils.{FakeAuthJourney, NewCachingBaseSpec}
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class AddEmploymentControllerSpec extends NewCachingBaseSpec {

  private def fakeGetRequest: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")

  val auditService: AuditService = mock[AuditService]
  val employmentService: EmploymentService = mock[EmploymentService]

  val userAnswers: UserAnswers = UserAnswers(
    RequestBuilder.uuid,
    nino,
    Json.obj(
      "employmentName" -> "TEST-Employer"
    )
  )

  def createSUT(
    userAnswersAsArg: Option[UserAnswers] = None,
    repository: JourneyCacheRepository = mockRepository
  ) = new AddEmploymentController(
    auditService,
    employmentService,
    new FakeAuthJourney(userAnswersAsArg.getOrElse(userAnswers)),
    repository,
    mock[AuditConnector],
    mcc,
    inject[AddEmploymentStartDateFormView],
    inject[AddEmploymentNameFormView],
    inject[AddEmploymentFirstPayFormView],
    inject[AddEmploymentErrorPageView],
    inject[AddEmploymentPayrollNumberFormView],
    inject[CanWeContactByPhoneView],
    inject[ConfirmationView],
    inject[AddIncomeCheckYourAnswersView],
    inject[ErrorPagesHandler]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRepository, employmentService)
    when(mockRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(userAnswers)))
  }

  "addEmploymentName" must {
    "show the employment name form page" when {
      "the request has an authorised session and no previously supplied employment name is present in cache" in {
        val sut = createSUT()

        val result = sut.addEmploymentName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.addNameForm.title"))
      }
      "the request has an authorised session and a previously supplied employment name is present in cache" in {
        val sut = createSUT()

        val result = sut.addEmploymentName()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.addNameForm.title"))
        doc.toString must include("TEST-Employer")
      }
    }
  }

  "submitEmploymentName" must {
    "redirect to the Start Date form" when {
      "the form submission is valid" in {
        val sut = createSUT()

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

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
        val sut = createSUT()
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
        val sut = createSUT()

        val result = sut.addEmploymentStartDate()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.addEmployment.startDateForm.pagetitle"))
        doc.select("#tellUsStartDateForm-year").get(0).attributes.get("value") mustBe ""
      }

      "the request has an authorised session and a previously supplied start date is present in cache" in {
        val request = fakeGetRequest
        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(AddEmploymentStartDatePage.toString -> LocalDate.parse("2017-12-12"))
          )
        val application = applicationBuilder(userAnswers = updatedUserAnswers).build()
        running(application) {
          val sut = createSUT(Some(updatedUserAnswers))
          val result = sut.addEmploymentStartDate()(request)

          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.addEmployment.startDateForm.pagetitle"))
          doc.select("#tellUsStartDateForm-year").get(0).attributes.get("value") mustBe "2017"
        }
      }

      "redirect to the tax summary page if a value is missing from the cache " in {
        val request = fakeGetRequest
        val userAnswersWithYesOrNo =
          userAnswers.copy(data = Json.obj())
        val application = applicationBuilder(userAnswers = userAnswersWithYesOrNo).build()
        running(application) {
          val sut = createSUT(Some(userAnswersWithYesOrNo))
          val result = sut.addEmploymentStartDate()(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
        }
      }
    }

  }

  "submit start date" must {
    "return redirect" when {
      "form is valid and start date is over 6 weeks ago" in {
        val sut = createSUT()

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

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
        val sut = createSUT()
        val date = LocalDate.now.minusDays(4)

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

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
        val sut = createSUT()

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
        val sut = createSUT()

        when(mockRepository.set(any()))
          .thenReturn(Future.successful(true))

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
      }

      "form is valid and start date is less than 6 weeks" in {
        val sut = createSUT()
        val date = LocalDate.now.minusDays(4)

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

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
      }
    }
  }

  "received First Pay" must {
    "show the first pay choice page" when {
      "the request has an authorised session and no previous response is held in cache" in {
        val employmentName = "TEST-Employer"
        val request = fakeGetRequest

        val application = applicationBuilder(userAnswers).build()
        running(application) {
          val result = createSUT(Some(userAnswers)).receivedFirstPay()(request)
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.addEmployment.employmentFirstPay.title", employmentName))
          doc.select("input[id=firstPayChoice-yes][checked=checked]").size() mustBe 0
          doc.select("input[id=firstPayChoice-no][checked=checked]").size() mustBe 0
        }
      }

      "the request has an authorised session and a previous response is held in cache" in {
        val request = fakeGetRequest
        val userAnswersWithYesOrNo =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(AddEmploymentReceivedFirstPayPage.toString -> FormValuesConstants.YesValue)
          )
        val application = applicationBuilder(userAnswers = userAnswersWithYesOrNo).build()
        running(application) {
          val result = createSUT(Some(userAnswersWithYesOrNo)).receivedFirstPay()(request)
          val doc = Jsoup.parse(contentAsString(result))
          doc.select("input[id=firstPayChoice][checked]").size() mustBe 1
          doc.select("input[id=firstPayChoice-2][checked]").size() mustBe 0
        }
      }
    }
  }

  "submit first pay choice" must {

    "redirect user to payroll number page" when {
      "yes is selected" in {
        val sut = createSUT()

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

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
        val sut = createSUT()

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

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
        val sut = createSUT()

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
          any(),
          any()
        )(any(), any())
      }
    }

    "return BadRequest" when {
      "there is a form validation error" in {
        val sut = createSUT()
        val employmentName = "TEST-Employer"

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
        val request = fakeGetRequest
        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(
              AddEmploymentStartDateWithinSixWeeksPage.toString -> FormValuesConstants.YesValue
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).addEmploymentPayrollNumber()(request)
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
          doc.select("input[id=payrollNumberChoice][checked=checked]").size() mustBe 0
          doc.select("input[id=payrollNumberChoice-2][checked=checked]").size() mustBe 0
          doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe ""
        }
      }

      "the request has an authorised session and a previous 'no' response is held in cache" in {
        val request = fakeGetRequest
        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(
              AddEmploymentStartDateWithinSixWeeksPage.toString -> FormValuesConstants.YesValue,
              AddEmploymentPayrollQuestionPage.toString         -> FormValuesConstants.NoValue,
              AddEmploymentPayrollNumberPage.toString           -> "should be ignored"
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).addEmploymentPayrollNumber()(request)
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
          doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
          doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 0
          doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 1
          doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe ""
        }
      }
      "the request has an authorised session and a previous 'yes' response is held in cache" in {
        val request = fakeGetRequest
        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(
              AddEmploymentStartDateWithinSixWeeksPage.toString -> FormValuesConstants.YesValue,
              AddEmploymentPayrollQuestionPage.toString         -> FormValuesConstants.YesValue,
              AddEmploymentPayrollNumberPage.toString           -> "should be displayed"
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).addEmploymentPayrollNumber()(request)
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
          doc.title() must include(Messages("tai.addEmployment.employmentPayrollNumber.pagetitle"))
          doc.select("input[id=payrollNumberChoice][checked]").size() mustBe 1
          doc.select("input[id=payrollNumberChoice-2][checked]").size() mustBe 0
          doc.select("input[id=payrollNumberEntry]").get(0).attributes.get("value") mustBe "should be displayed"
        }
      }
    }
  }

  "submit employment payroll number" must {
    "cache payroll number" when {
      "the form is valid and user knows their payroll number" in {
        val sut = createSUT()
        val payrollNo = "1234"
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

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

        verify(mockRepository, times(1)).set(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user knows their payroll number" in {
        val sut = createSUT()
        val payrollNo = "1234"

        when(mockRepository.set(any())).thenReturn(Future.successful(true))

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

        verify(mockRepository, times(1)).set(any())
      }
    }

    "cache payroll number as not known value" when {
      "the form is valid and user doesn't know its payroll number" in {
        val sut = createSUT()

        when(mockRepository.set(any)).thenReturn(Future.successful(true))

        val result =
          sut.submitEmploymentPayrollNumber()(
            RequestBuilder
              .buildFakeRequestWithAuth("POST")
              .withFormUrlEncodedBody(PayrollNumberChoice -> FormValuesConstants.NoValue, PayrollNumberEntry -> "")
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url

        verify(mockRepository, times(1)).set(any())
      }
    }

    "redirect to add telephone number page" when {
      "the form is valid and user doesn't know its payroll number" in {
        val sut = createSUT()

        when(mockRepository.set(any)).thenReturn(Future.successful(true))

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
        val sut = createSUT()

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
        val sut = createSUT()

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe ""
      }
      "the request has an authorised session and a previous 'no' response is held in cache" in {
        val request = fakeGetRequest
        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(
              AddEmploymentTelephoneQuestionPage.toString -> FormValuesConstants.NoValue,
              AddEmploymentTelephoneNumberPage.toString   -> "should be ignored"
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).addTelephoneNumber()(request)
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.canWeContactByPhone.title"))
          doc.select("input[id=yesNoChoice][checked]").size() mustBe 0
          doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 1
          doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe ""
        }
      }
      "the request has an authorised session and a previous 'yes' response is held in cache" in {
        val request = fakeGetRequest
        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(
              AddEmploymentTelephoneQuestionPage.toString -> FormValuesConstants.YesValue,
              AddEmploymentTelephoneNumberPage.toString   -> "should be displayed"
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).addTelephoneNumber()(request)
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.canWeContactByPhone.title"))
          doc.select("input[id=yesNoChoice][checked]").size() mustBe 1
          doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 0
          doc.select("input[id=yesNoTextEntry]").get(0).attributes.get("value") mustBe "should be displayed"
        }
      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT()

        when(mockRepository.set(any())).thenReturn(Future.successful(true))
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
        val sut = createSUT()

        when(mockRepository.set(any())).thenReturn(Future.successful(true))
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
        val sut = createSUT()

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
        val sut = createSUT()

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
        val request = fakeGetRequest

        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(
              AddEmploymentNamePage.toString              -> "TEST-employer",
              AddEmploymentStartDatePage.toString         -> LocalDate.of(2017, 6, 15),
              AddEmploymentPayrollNumberPage.toString     -> "emp-ref-1234",
              AddEmploymentTelephoneQuestionPage.toString -> "Yes",
              AddEmploymentTelephoneNumberPage.toString   -> "should be displayed"
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).addEmploymentCheckYourAnswers()(request)
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.checkYourAnswers.title"))
        }
      }
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      val sut = createSUT()

      val result = sut.addEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submit your answers" must {
    "invoke the back end 'addEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session amd a telephone number has been provided" in {
        val expectedModel =
          AddEmployment("empName", LocalDate.parse("2017-04-04"), "I do not know", "Yes", Some("123456789"))

        when(employmentService.addEmployment(any(), meq(expectedModel))(any(), any()))
          .thenReturn(Future.successful("envelope-123"))
        when(mockRepository.set(any())).thenReturn(Future.successful(true))
        when(mockRepository.clear(any(), any())).thenReturn(Future.successful(true))

        val request = fakeGetRequest

        val updatedUserAnswers =
          userAnswers.copy(data =
            userAnswers.data ++ Json.obj(
              AddEmploymentNamePage.toString              -> expectedModel.employerName,
              AddEmploymentStartDatePage.toString         -> expectedModel.startDate,
              AddEmploymentPayrollNumberPage.toString     -> expectedModel.payrollNumber,
              AddEmploymentTelephoneQuestionPage.toString -> expectedModel.telephoneContactAllowed,
              AddEmploymentTelephoneNumberPage.toString   -> expectedModel.telephoneNumber
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).submitYourAnswers()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.confirmation().url
        }
      }

      "the request has an authorised session amd no telephone number was provided" in {
        val expectedModel = AddEmployment("empName", LocalDate.parse("2017-04-04"), "I do not know", "No", None)

        when(employmentService.addEmployment(any(), meq(expectedModel))(any(), any()))
          .thenReturn(Future.successful("envelope-123"))
        when(mockRepository.set(any())).thenReturn(Future.successful(true))
        when(mockRepository.clear(any(), any())).thenReturn(Future.successful(true))

        val request = fakeGetRequest

        val updatedUserAnswers =
          userAnswers.copy(data =
            Json.obj(
              AddEmploymentNamePage.toString              -> expectedModel.employerName,
              AddEmploymentStartDatePage.toString         -> expectedModel.startDate,
              AddEmploymentPayrollNumberPage.toString     -> expectedModel.payrollNumber,
              AddEmploymentTelephoneQuestionPage.toString -> expectedModel.telephoneContactAllowed,
              AddEmploymentTelephoneNumberPage.toString   -> expectedModel.telephoneNumber
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).submitYourAnswers()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.employments.routes.AddEmploymentController.confirmation().url
        }
      }
    }
    "show a bad request page" when {
      "a value is missing from the needed userAnswers" in {
        val expectedModel = AddEmployment("empName", LocalDate.parse("2017-04-04"), "I do not know", "No", None)

        val request = fakeGetRequest

        val updatedUserAnswers =
          userAnswers.copy(data =
            Json.obj(
              AddEmploymentStartDatePage.toString         -> expectedModel.startDate,
              AddEmploymentPayrollNumberPage.toString     -> expectedModel.payrollNumber,
              AddEmploymentTelephoneQuestionPage.toString -> expectedModel.telephoneContactAllowed,
              AddEmploymentTelephoneNumberPage.toString   -> expectedModel.telephoneNumber
            )
          )
        val application = applicationBuilder(updatedUserAnswers).build()
        running(application) {
          val result = createSUT(Some(updatedUserAnswers)).submitYourAnswers()(request)
          status(result) mustBe BAD_REQUEST
        }

        verify(mockRepository, times(0)).clear(any(), any())
        verify(mockRepository, times(0)).set(any())
        verify(employmentService, times(0)).addEmployment(any(), any())(any(), any())
      }
    }
  }

  "confirmation" must {
    "show the add employment confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT()

        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.employmentConfirmation.heading"))
      }
    }
  }

  "cancel" must {
    "redirect to the the TaxAccountSummaryController" in {

      when(mockRepository.clear(any(), any())).thenReturn(Future.successful(true))

      val result = createSUT().cancel()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }
}
