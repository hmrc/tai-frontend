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
import controllers.ErrorPagesHandler
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import pages.{EmploymentEndDateKeyPage, EmploymentTelephoneNumberKeyPage, EmploymentTelephoneQuestionKeyPage, EmploymentUpdateRemovePage}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.employments.EmploymentEndDateForm
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{EmploymentDecisionConstants, FormValuesConstants, IrregularPayConstants}
import utils.{FakeActionJourney, NewCachingBaseSpec}
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import scala.language.postfixOps

class EndEmploymentControllerSpec extends NewCachingBaseSpec with BeforeAndAfterEach {

  private def fakeGetRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

  private def fakePostRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

  val auditService: AuditService = mock[AuditService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val endEmploymentJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val trackSuccessJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  val userAnswers: UserAnswers = UserAnswers(
    RequestBuilder.uuid,
    Json.obj(
      EndCompanyBenefitConstants.EmploymentIdKey -> 1
    )
  )

  val userAnswersWithYesOrNo =
    userAnswers.copy(data = userAnswers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> "Yes"))

  def controller(userAnswersAsArg: Option[UserAnswers] = None) = new EndEmploymentController(
    auditService,
    employmentService,
    mock[AuditConnector],
    mcc,
    inject[ErrorPagesHandler],
    inject[UpdateRemoveEmploymentDecisionView],
    inject[EndEmploymentWithinSixWeeksErrorView],
    inject[EndEmploymentIrregularPaymentErrorView],
    inject[EndEmploymentView],
    inject[CanWeContactByPhoneView],
    inject[DuplicateSubmissionWarningView],
    inject[ConfirmationView],
    inject[AddIncomeCheckYourAnswersView],
    new FakeActionJourney(userAnswersAsArg.getOrElse(userAnswers))
  )

  override def beforeEach(): Unit = {
    reset(employmentService, endEmploymentJourneyCacheService)
    when(employmentService.employment(any(), any())(any()))
      .thenReturn(
        Future.successful(
          Some(
            Employment(
              employerName,
              Live,
              None,
              LocalDate.now,
              None,
              Nil,
              "",
              "",
              1,
              None,
              hasPayrolledBenefit = false,
              receivingOccupationalPension = false
            )
          )
        )
      )
    when(endEmploymentJourneyCacheService.currentValueAsDate(any())(any())) // TODO - Delete
      .thenReturn(Future.successful(Some(LocalDate.parse("2017-09-09"))))
    when(endEmploymentJourneyCacheService.currentValue(any())(any()))
      .thenReturn(Future.successful(Some("Test Value")))
    when(endEmploymentJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
  }

  when(mockSessionRepository.set(any())).thenReturn(Future.successful(true)) // TODO - Delete?
  when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnswers)))

  "employmentUpdateRemove" must {
    List(
      FormValuesConstants.YesValue,
      FormValuesConstants.NoValue
    ).foreach { yesOrNo =>
      s"call updateRemoveEmployer page successfully with an authorised session regardless of if optional cache is $yesOrNo" in {
        val request = fakeGetRequest
        val userAnswersWithYesOrNo =
          userAnswers.copy(data = userAnswers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> yesOrNo))
        val application = applicationBuilder(userAnswers = userAnswersWithYesOrNo).build()

        running(application) {
          val result = controller(Some(userAnswersWithYesOrNo)).employmentUpdateRemoveDecision(request)
          val doc = Jsoup.parse(contentAsString(result))

          status(result) mustBe OK
          doc.title() must include(messages("tai.employment.decision.legend", employerName))
        }
      }
    }
    s"call updateRemoveEmployer page successfully with an authorised session regardless of if optional cache is empty" in {
      val request = fakeGetRequest
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).employmentUpdateRemoveDecision(request)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(messages("tai.employment.decision.legend", employerName))
      }
    }
    "redirect to the tax summary page if the employer id is missing from the cache" in {
      val request = RequestBuilder.buildFakeRequestWithOnlySession("GET")
      val emptyUserAnswers = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

      running(application) {
        val result = controller(Some(emptyUserAnswers)).employmentUpdateRemoveDecision(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
    "redirect to the tax summary page if the call to retrieve employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val request = fakeGetRequest
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).employmentUpdateRemoveDecision(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
  }

  "handleEmploymentUpdateRemove" must {
    "redirect to the update employment url if the form has the value Yes in EmploymentDecision" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.YesValue)
      val userAnswersWithYesOrNo =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> FormValuesConstants.YesValue)
        )
      val application = applicationBuilder(userAnswers = userAnswersWithYesOrNo).build()

      running(application) {
        val result = controller(Some(userAnswersWithYesOrNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe SEE_OTHER
        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _               => ""
        }
        redirectUrl mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(1).url
      }
    }
    "redirect to end employment page if value no is passed in the form and the employment has a payment no more than 6 weeks 1 day in the past" in {
      val payment = paymentOnDate(LocalDate.now().minusWeeks(6).minusDays(1))
      val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
      val employment = employmentWithAccounts(List(annualAccount))

      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

      val request =
        fakePostRequest.withFormUrlEncodedBody("employmentDecision" -> FormValuesConstants.NoValue) // TODO - Needed?
      val userAnswersWithNo =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> FormValuesConstants.NoValue)
        )
      val application = applicationBuilder(userAnswers = userAnswersWithNo).build()

      running(application) {
        val result = controller(Some(userAnswersWithNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe SEE_OTHER
        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _               => ""
        }
        redirectUrl mustBe controllers.employments.routes.EndEmploymentController.endEmploymentPage().url
      }
    }
    "redirect to error page if value no is passed in the form and the employment has a payment is less than 6 weeks 1 day from today in the past" in {
      val payment = paymentOnDate(LocalDate.now().minusWeeks(6))
      val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
      val employment = employmentWithAccounts(List(annualAccount))

      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

      val request = fakePostRequest.withFormUrlEncodedBody("employmentDecision" -> FormValuesConstants.NoValue)
      val userAnswersWithNo =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> FormValuesConstants.NoValue)
        )
      val application = applicationBuilder(userAnswers = userAnswersWithNo).build()

      running(application) {
        val result = controller(Some(userAnswersWithNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe SEE_OTHER
        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _               => ""
        }
        redirectUrl mustBe controllers.employments.routes.EndEmploymentController.endEmploymentError().url
      }
    }
    "redirect to irregular payment page if value No is passed in the form and the employment has an irregular payment frequency" in {
      val payment = paymentOnDate(LocalDate.now().minusWeeks(8)).copy(payFrequency = Irregular)
      val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
      val employment = employmentWithAccounts(List(annualAccount))

      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

      val request = fakePostRequest.withFormUrlEncodedBody("employmentDecision" -> FormValuesConstants.NoValue)
      val userAnswersWithNo =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> FormValuesConstants.NoValue)
        )
      val application = applicationBuilder(userAnswers = userAnswersWithNo).build()

      running(application) {
        val result = controller(Some(userAnswersWithNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe SEE_OTHER
        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _               => ""
        }
        redirectUrl mustBe controllers.employments.routes.EndEmploymentController.irregularPaymentError().url
      }
    }
    "return INTERNAL_SERVER_ERROR if the employer id is missing from the cache" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val request = FakeRequest("POST", "")
      val emptyUserAnswers = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

      running(application) {
        val result = controller(Some(emptyUserAnswers)).handleEmploymentUpdateRemove(request)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.YesValue)
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswersWithYesOrNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return BAD_REQUEST and display form with errors if no form value is passed as EmploymentDecision" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> "")
      val userAnswersWithYesOrNo =
        userAnswers.copy(data = userAnswers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> ""))
      val application = applicationBuilder(userAnswers = userAnswersWithYesOrNo).build()

      running(application) {
        val result = controller().handleEmploymentUpdateRemove(request)
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "endEmploymentError is called" must {
    "return OK and endEmploymentWithinSixWeeksError if payment data and employment data exist" in {
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EndEmploymentConstants.LatestPaymentDateKey -> LocalDate.now().minusWeeks(7))
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentError()(fakeGetRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(
          Messages(
            "tai.endEmploymentWithinSixWeeksError.heading",
            LocalDate.now.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
          )
        )
      }
    }
    "return INTERNAL_SERVER_ERROR if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EndEmploymentConstants.LatestPaymentDateKey -> LocalDate.now().minusWeeks(7)
          ) // TODO - Change to page instead of constant?
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentError()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if payment data doesn't exist" in {
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).endEmploymentError()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "irregularPaymentError is called" must {
    "return OK and endEmploymentWithinSixWeeksError if payment data and employment data exist" in {
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).irregularPaymentError()(fakeGetRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.irregular.preHeadingText"))
      }
    }
    "return INTERNAL_SERVER_ERROR if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).irregularPaymentError()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if payment data doesn't exist" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).irregularPaymentError()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "confirmAndSendEndEmployment is run" must { // TODO - Can't test service failure as long as it's Future[String]
    "redirect to showConfirmationPage if all user answers are present and end employment call is successful" in {
      when(employmentService.endEmployment(any(), any(), any())(any()))
        .thenReturn(Future.successful(""))

      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EmploymentEndDateKeyPage.toString           -> LocalDate.now(),
          EmploymentTelephoneQuestionKeyPage.toString -> "Yes",
          EmploymentTelephoneNumberKeyPage.toString   -> "123456789"
        )
      )
      val application = applicationBuilder(userAnswersFull).build()

      running(application) {
        val result = controller(Some(userAnswersFull)).confirmAndSendEndEmployment()(fakePostRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.showConfirmationPage().url
      }
    }
    "return INTERNAL_SERVER_ERROR if values are missing from user answers" in {
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).confirmAndSendEndEmployment()(fakePostRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "endEmploymentPage is run" must {
    "return OK and endEmploymentView if users answers data and employment data exist" in {
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentEndDateKeyPage.toString -> LocalDate.now().minusWeeks(7))
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentPage()(fakeGetRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.endEmployment.endDateForm.pagetitle"))
      }
    }
    "return INTERNAL_SERVER_ERROR if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EndEmploymentConstants.LatestPaymentDateKey -> LocalDate.now().minusWeeks(7)
          ) // TODO - Change to page instead of constant?
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentPage()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if payment data doesn't exist" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).endEmploymentPage()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "handleEndEmploymentPage is run" must {
    "redirect to addTelephoneNumber if call to retrieve employment data was successful" in {
      val date = LocalDate.now
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> date.getDayOfWeek.getValue.toString,
          EmploymentEndDateForm.EmploymentFormMonth -> date.getMonthValue.toString,
          EmploymentEndDateForm.EmploymentFormYear  -> date.getYear.toString
        )
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EmploymentEndDateKeyPage.toString -> date
          )
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).handleEndEmploymentPage()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
          .addTelephoneNumber()
          .url
      }
    }
    "redirect to addTelephoneNumber if call to retrieve employment data was successful but form data is invalid" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> "",
          EmploymentEndDateForm.EmploymentFormMonth -> "",
          EmploymentEndDateForm.EmploymentFormYear  -> ""
        )
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).handleEndEmploymentPage()(request)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return INTERNAL_SERVER_ERROR if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EmploymentEndDateKeyPage.toString -> LocalDate.now
          )
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()
      running(application) {
        val result = controller(Some(userAnswersEmpty)).handleEndEmploymentPage()(fakePostRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if user answers data exists but employment data does not" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswers = userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).handleEndEmploymentPage()(fakePostRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "endEmploymentCheckYourAnswers is run" must {
    "return OK and addIncomeCheckYourAnswers if users answers data exists" in {
      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EmploymentEndDateKeyPage.toString           -> LocalDate.now(),
          EmploymentTelephoneQuestionKeyPage.toString -> "Yes",
          EmploymentTelephoneNumberKeyPage.toString   -> "123456789"
        )
      )
      val application = applicationBuilder(userAnswersFull).build()

      running(application) {
        val result = controller(Some(userAnswersFull)).endEmploymentCheckYourAnswers()(fakePostRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.endEmploymentConfirmAndSend.heading"))
      }
    }
    "redirect to tax summaries page if missing user answers data" in {
      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EmploymentEndDateKeyPage.toString           -> "",
          EmploymentTelephoneQuestionKeyPage.toString -> "",
          EmploymentTelephoneNumberKeyPage.toString   -> ""
        )
      )
      val application = applicationBuilder(userAnswersFull).build()

      running(application) {
        val result = controller(Some(userAnswersFull)).endEmploymentCheckYourAnswers()(fakePostRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
  }

  "showConfirmationPage" must {
    "show confirmation view" in {

      val result = controller().showConfirmationPage()(fakeGetRequest)
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() must include(Messages("tai.employmentConfirmation.heading"))
    }
  }
  "addTelephoneNumber when run" must { // TODO - Change to call
    "return OK and canWeContactByPhone if users answers data exists" in {
      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EmploymentEndDateKeyPage.toString           -> LocalDate.now(),
          EmploymentTelephoneQuestionKeyPage.toString -> "Yes",
          EmploymentTelephoneNumberKeyPage.toString   -> "123456789"
        )
      )
      val application = applicationBuilder(userAnswersFull).build()

      running(application) {
        val result = controller(Some(userAnswersFull)).addTelephoneNumber()(fakePostRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
    "redirect to tax summaries page if missing user answers data" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).addTelephoneNumber()(fakePostRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }

    "submitTelephoneNumber when called" must { // TODO - Needs tests to read end user answers to confirm contents
      "redirect to endEmploymentCheckYourAnswers and add the phone number to user answers if value Yes and a phone number are submitted" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "123456789"
          )

        val application = applicationBuilder(userAnswers).build()

        running(application) {
          val result = controller().submitTelephoneNumber()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
            .endEmploymentCheckYourAnswers()
            .url
        }
      }
      "redirect to endEmploymentCheckYourAnswers if value No is submitted" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.NoValue,
            FormValuesConstants.YesNoTextEntry -> ""
          )

        val application = applicationBuilder(userAnswers).build()

        running(application) {
          val result = controller().submitTelephoneNumber()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
            .endEmploymentCheckYourAnswers()
            .url
        }
      }
      "return BAD_REQEST if value Yes but no phone number is submitted" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> ""
          )

        val application = applicationBuilder(userAnswers).build()

        running(application) {
          val result = controller().submitTelephoneNumber()(request)
          status(result) mustBe BAD_REQUEST
        }
      }
      "return BAD_REQEST if value Yes but phone number is too short" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "1243"
          )

        val application = applicationBuilder(userAnswers).build()

        running(application) {
          val result = controller().submitTelephoneNumber()(request)
          status(result) mustBe BAD_REQUEST
        }
      }
      "return BAD_REQEST if value Yes but phone number is too long" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "1243124312431243124312431243124312431243"
          )

        val application = applicationBuilder(userAnswers).build()

        running(application) {
          val result = controller().submitTelephoneNumber()(request)
          status(result) mustBe BAD_REQUEST
        }
      }
      "return BAD_REQEST if form values are invalid" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> "",
            FormValuesConstants.YesNoTextEntry -> ""
          )

        val application = applicationBuilder(userAnswers).build()

        running(application) {
          val result = controller().submitTelephoneNumber()(request)
          status(result) mustBe BAD_REQUEST
        }
      }
      "redirect to tax summaries page if missing user answers data" in {
        val userAnswersEmpty = userAnswers.copy(data = Json.obj())
        val application = applicationBuilder(userAnswersEmpty).build()

        running(application) {
          val result = controller(Some(userAnswersEmpty)).submitTelephoneNumber()(fakePostRequest)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
        }
      }
    }
  }
  "handleIrregularPaymentError is called" must {
    s"redirect to tax summaries if emp id exists and user inputted ${IrregularPayConstants.ContactEmployer}" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> IrregularPayConstants.ContactEmployer)
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().handleIrregularPaymentError(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
    s"redirect to updateEmploymentDetails if emp id exists and user inputted anything besides ${IrregularPayConstants.ContactEmployer}" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> IrregularPayConstants.UpdateDetails)
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().handleIrregularPaymentError(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
          .endEmploymentPage()
          .url
      }
    }
    "return INTERNAL_SERVER_ERROR if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).handleIrregularPaymentError(fakePostRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if user answers data exists but employment data does not and there are form errors" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> "")
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).handleIrregularPaymentError(request)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return BAD_REQUEST if user answers and employment data exists but there are form errors" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> "")
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).handleIrregularPaymentError(request)
        status(result) mustBe BAD_REQUEST
      }
    }
  }
  "onPageLoad is called" must {
    "redirect to employmentUpdateRemove when there is no employment id in the user answers" in {
      val empId = 1
      val emptyUserAnswers = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(emptyUserAnswers).build()

      running(application) {
        val result = controller().onPageLoad(empId)(fakeGetRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.employmentUpdateRemoveDecision().url
      }
    }
    "redirect to duplicateSubmissionWarning when there is an employment id in the user answers" in {
      val empId = 1
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().onPageLoad(empId)(fakeGetRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.duplicateSubmissionWarning().url
      }
    }
  }

  "duplicateSubmissionWarning is called" must {
    "show duplicateSubmissionWarning if emp id and employment data exist" in {
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().duplicateSubmissionWarning()(fakeGetRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.employment.warning.customGaTitle"))
      }
    }
    "return INTERNAL_SERVER_ERROR if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).duplicateSubmissionWarning()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if user answers data exists but employment data does not" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().duplicateSubmissionWarning()(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
  "submitDuplicateSubmissionWarning is called" must {
    "redirect to employmentUpdateRemoveDecision if value Yes was submitted, and emp id and employment data exist" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().submitDuplicateSubmissionWarning()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision().url
      }
    }
    "redirect to onPageLoad if value No was submitted, and emp id and employment data exist" in {
      val empId = 1
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().submitDuplicateSubmissionWarning()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(empId).url
      }
    }
    "return INTERNAL_SERVER_ERROR if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).submitDuplicateSubmissionWarning()(fakePostRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return INTERNAL_SERVER_ERROR if user answers data exists but employment data does not" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().submitDuplicateSubmissionWarning()(fakePostRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "return BAD_REQUEST if user answers and employment data exists but there are form errors" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> "")
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().submitDuplicateSubmissionWarning()(request)
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "cancel" must {
    "redirect to the the IncomeSourceSummarycontroller()" in {
      val employmentId = 1
      when(endEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

      val result = controller().cancel(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url
    }
  }

  def employmentWithAccounts(accounts: List[AnnualAccount]) =
    Employment(
      "employer",
      Live,
      Some("emp123"),
      LocalDate.of(2000, 5, 20),
      None,
      accounts,
      "",
      "",
      8,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false
    )

  def paymentOnDate(date: LocalDate): Payment =
    Payment(
      date = date,
      amountYearToDate = 2000,
      taxAmountYearToDate = 200,
      nationalInsuranceAmountYearToDate = 100,
      amount = 1000,
      taxAmount = 100,
      nationalInsuranceAmount = 50,
      payFrequency = Monthly
    )
}
