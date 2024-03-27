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

import builders.RequestBuilder
import controllers.ErrorPagesHandler
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.BeforeAndAfterEach
import pages.EndEmployment._
import pages._
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.tai.forms.employments.EmploymentEndDateForm
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{EmploymentDecisionConstants, FormValuesConstants, IrregularPayConstants}
import utils.{FakeAuthJourney, NewCachingBaseSpec}
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class EndEmploymentControllerSpec extends NewCachingBaseSpec with BeforeAndAfterEach {

  private def fakeGetRequest: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")

  private def fakePostRequest: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("POST")

  val auditService: AuditService = mock[AuditService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val trackSuccessJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  val userAnswers: UserAnswers = UserAnswers(
    RequestBuilder.uuid,
    nino,
    Json.obj(
      "end-employment-employmentId" -> 1
    )
  )

  val userAnswersWithYesOrNo: UserAnswers =
    userAnswers.copy(data = userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> "Yes"))

  def controller(
    userAnswersAsArg: Option[UserAnswers] = None,
    repository: JourneyCacheNewRepository = mockRepository
  ) = new EndEmploymentController(
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
    new FakeAuthJourney(userAnswersAsArg.getOrElse(userAnswers)),
    repository,
    trackSuccessJourneyCacheService
  )

  override def beforeEach(): Unit = {
    reset(mockFeatureFlagService)
    reset(employmentService, trackSuccessJourneyCacheService)
    when(employmentService.employment(any(), any())(any()))
      .thenReturn(
        Future.successful(
          Some(
            Employment(
              employerName,
              Live,
              None,
              Some(LocalDate.now),
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
    when(auditService.createAndSendAuditEvent(any(), any())(any(), any()))
      .thenReturn(Future.successful(Success))
    when(mockRepository.set(any())).thenReturn(Future.successful(true))
    when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswers)))
  }

  "employmentUpdateRemove" must {
    List(
      FormValuesConstants.YesValue,
      FormValuesConstants.NoValue
    ).foreach { yesOrNo =>
      s"call updateRemoveEmployer page successfully with an authorised session regardless of if optional cache is $yesOrNo" in {
        val request = fakeGetRequest
        val userAnswersWithYesOrNo =
          userAnswers.copy(data = userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> yesOrNo))
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
    "return BAD_REQUEST if the employer id is missing from the cache" in {
      val request = RequestBuilder.buildFakeRequestWithOnlySession("GET")
      val emptyUserAnswers = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

      running(application) {
        val result = controller(Some(emptyUserAnswers)).employmentUpdateRemoveDecision(request)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if the call to retrieve employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val request = fakeGetRequest
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).employmentUpdateRemoveDecision(request)
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "handleEmploymentUpdateRemove" must {
    "redirect to the update employment url if the form has the value Yes in EmploymentDecision" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.YesValue)
      val userAnswersWithYesOrNo =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> FormValuesConstants.YesValue)
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
        fakePostRequest.withFormUrlEncodedBody("employmentDecision" -> FormValuesConstants.NoValue)
      val userAnswersWithNo =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> FormValuesConstants.NoValue)
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
          userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> FormValuesConstants.NoValue)
        )
      val application = applicationBuilder(userAnswers = userAnswersWithNo).build()

      running(application) {
        val result = controller(Some(userAnswersWithNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).map(
          _ mustBe controllers.employments.routes.EndEmploymentController.endEmploymentError().url
        )
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
          userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> FormValuesConstants.NoValue)
        )
      val application = applicationBuilder(userAnswers = userAnswersWithNo).build()

      running(application) {
        val result = controller(Some(userAnswersWithNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).map(
          _ mustBe controllers.employments.routes.EndEmploymentController.irregularPaymentError().url
        )
      }
    }
    "redirect to endEmploymentPage if there is no latest payment data" in {
      val request = fakePostRequest.withFormUrlEncodedBody("employmentDecision" -> FormValuesConstants.NoValue)
      val userAnswersWithNo =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> FormValuesConstants.NoValue)
        )
      val application = applicationBuilder(userAnswers = userAnswersWithNo).build()

      running(application) {
        val result = controller(Some(userAnswersWithNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).map(
          _ mustBe controllers.employments.routes.EndEmploymentController.endEmploymentPage().url
        )
      }
    }
    "return BAD_REQUEST if the employer id is missing from the cache" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val request = FakeRequest("POST", "")
      val emptyUserAnswers = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

      running(application) {
        val result = controller(Some(emptyUserAnswers)).handleEmploymentUpdateRemove(request)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.YesValue)
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswersWithYesOrNo)).handleEmploymentUpdateRemove(request)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST and display form with errors if no form value is passed as EmploymentDecision" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> "")
      val userAnswersWithYesOrNo =
        userAnswers.copy(data = userAnswers.data ++ Json.obj(EmploymentDecisionPage.toString -> ""))
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
    "return BAD_REQUEST if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EndEmploymentLatestPaymentPage.toString -> LocalDate.now().minusWeeks(7)
          )
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentError()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if payment data doesn't exist" in {
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).endEmploymentError()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
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
    "return BAD_REQUEST if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).irregularPaymentError()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if payment data doesn't exist" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).irregularPaymentError()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
  }
  "confirmAndSendEndEmployment is called" must { // TODO - Can't test service failure as long as it's Future[String]
    "redirect to showConfirmationPage if all user answers are present, and end employment call is successful, and cache succeeds" in {
      when(employmentService.endEmployment(any(), any(), any())(any()))
        .thenReturn(Future.successful(""))
      when(mockRepository.clear(any(), any()))
        .thenReturn(Future.successful(true))
      when(trackSuccessJourneyCacheService.cache(any())(any()))
        .thenReturn(
          Future.successful(
            Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-${userAnswers.sessionId}" -> "true")
          )
        )

      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EndEmploymentEndDatePage.toString           -> LocalDate.now(),
          EndEmploymentTelephoneQuestionPage.toString -> "Yes",
          EndEmploymentTelephoneNumberPage.toString   -> "123456789"
        )
      )
      val application = applicationBuilder(userAnswersFull).build()

      running(application) {
        val result = controller(Some(userAnswersFull)).confirmAndSendEndEmployment()(fakePostRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.showConfirmationPage().url
      }
    }
    "redirect to showConfirmationPage if all user answers are present, and end employment call is successful, but cache fails" in {
      when(employmentService.endEmployment(any(), any(), any())(any()))
        .thenReturn(Future.successful(""))
      when(mockRepository.clear(any(), any()))
        .thenReturn(Future.successful(false))
      when(trackSuccessJourneyCacheService.cache(any())(any()))
        .thenReturn(
          Future.successful(
            Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-${userAnswers.sessionId}" -> "true")
          )
        )

      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EndEmploymentEndDatePage.toString           -> LocalDate.now(),
          EndEmploymentTelephoneQuestionPage.toString -> "Yes",
          EndEmploymentTelephoneNumberPage.toString   -> "123456789"
        )
      )
      val application = applicationBuilder(userAnswersFull).build()

      running(application) {
        val result = controller(Some(userAnswersFull)).confirmAndSendEndEmployment()(fakePostRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.showConfirmationPage().url
      }
    }
    "return BAD_REQUEST if values are missing from user answers" in {
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).confirmAndSendEndEmployment()(fakePostRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
  }
  "endEmploymentPage is called" must {
    "return OK and endEmploymentView if users answers data, employment data and end date exist" in {
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(EndEmploymentEndDatePage.toString -> LocalDate.now().minusWeeks(7))
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentPage()(fakeGetRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.endEmployment.endDateForm.pagetitle"))
      }
    }
    "return OK and endEmploymentView if users answers data and employment data exist but no end date in user answers" in {
      val userAnswersWithDate = userAnswers
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentPage()(fakeGetRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.endEmployment.endDateForm.pagetitle"))
      }
    }
    "return BAD_REQUEST if the request for employment data fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EndEmploymentLatestPaymentPage.toString -> LocalDate.now().minusWeeks(7)
          )
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()

      running(application) {
        val result = controller(Some(userAnswersWithDate)).endEmploymentPage()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if payment data doesn't exist" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).endEmploymentPage()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "handleEndEmploymentPage is called" must {
    "redirect to addTelephoneNumber if call to retrieve employment data was successful" in {
      val date = LocalDate.now
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> date.getDayOfMonth.toString,
          EmploymentEndDateForm.EmploymentFormMonth -> date.getMonthValue.toString,
          EmploymentEndDateForm.EmploymentFormYear  -> date.getYear.toString
        )
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EndEmploymentEndDatePage.toString -> date
          )
        )
      val mockRepository = mock[JourneyCacheNewRepository]
      val application = applicationBuilderWithoutRepository(userAnswers = userAnswersWithDate)
        .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
        .build()
      when(mockRepository.set(any())).thenReturn(Future.successful(true))

      running(application) {
        val result = controller(Some(userAnswersWithDate), mockRepository).handleEndEmploymentPage(0)(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
          .addTelephoneNumber()
          .url
        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }
    "return BAD_REQUEST if call to retrieve employment data was successful but form data is invalid" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> "",
          EmploymentEndDateForm.EmploymentFormMonth -> "",
          EmploymentEndDateForm.EmploymentFormYear  -> ""
        )
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).handleEndEmploymentPage(0)(request)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val userAnswersWithDate =
        userAnswers.copy(data =
          userAnswers.data ++ Json.obj(
            EndEmploymentEndDatePage.toString -> LocalDate.now
          )
        )
      val application = applicationBuilder(userAnswers = userAnswersWithDate).build()
      running(application) {
        val result = controller(Some(userAnswersEmpty)).handleEndEmploymentPage(0)(fakePostRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if user answers data exists but employment data does not" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).handleEndEmploymentPage(0)(fakePostRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "endEmploymentCheckYourAnswers is called" must {
    "return OK and addIncomeCheckYourAnswers if users answers data exists" in {
      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EndEmploymentEndDatePage.toString           -> LocalDate.now(),
          EndEmploymentTelephoneQuestionPage.toString -> "Yes",
          EndEmploymentTelephoneNumberPage.toString   -> "123456789"
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
    "return BAD_REQUEST if missing user answers data" in {
      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EndEmploymentEndDatePage.toString           -> "",
          EndEmploymentTelephoneQuestionPage.toString -> "",
          EndEmploymentTelephoneNumberPage.toString   -> ""
        )
      )
      val application = applicationBuilder(userAnswersFull).build()

      running(application) {
        val result = controller(Some(userAnswersFull)).endEmploymentCheckYourAnswers()(fakePostRequest)
        status(result) mustBe BAD_REQUEST
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
  "addTelephoneNumber is called" must {
    "return OK and canWeContactByPhone if users answers data exists" in {
      val userAnswersFull = userAnswers.copy(
        data = userAnswers.data ++ Json.obj(
          EndEmploymentEndDatePage.toString           -> LocalDate.now(),
          EndEmploymentTelephoneQuestionPage.toString -> "Yes",
          EndEmploymentTelephoneNumberPage.toString   -> "123456789"
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
    "return BAD_REQUEST if missing user answers data" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).addTelephoneNumber()(fakePostRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "submitTelephoneNumber when called" must {
      "redirect to endEmploymentCheckYourAnswers and add the phone number to user answers if value Yes and a phone number are submitted" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "123456789"
          )
        val mockRepository = mock[JourneyCacheNewRepository]
        val application = applicationBuilderWithoutRepository(userAnswers)
          .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
          .build()
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val result = controller(repository = mockRepository).submitTelephoneNumber()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
            .endEmploymentCheckYourAnswers()
            .url
          verify(mockRepository, times(1)).set(any[UserAnswers])
        }
      }
      "redirect to endEmploymentCheckYourAnswers if value No is submitted" in {
        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.NoValue,
            FormValuesConstants.YesNoTextEntry -> ""
          )

        val mockRepository = mock[JourneyCacheNewRepository]
        val application = applicationBuilderWithoutRepository(userAnswers)
          .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
          .build()
        when(mockRepository.set(any())).thenReturn(Future.successful(true))

        running(application) {
          val result = controller(repository = mockRepository).submitTelephoneNumber()(request)
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
            .endEmploymentCheckYourAnswers()
            .url
          verify(mockRepository, times(1)).set(any[UserAnswers])
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
      "return BAD_REQUEST if missing user answers data" in {
        val userAnswersEmpty = userAnswers.copy(data = Json.obj())
        val application = applicationBuilder(userAnswersEmpty).build()

        running(application) {
          val result = controller(Some(userAnswersEmpty)).submitTelephoneNumber()(fakePostRequest)
          status(result) mustBe BAD_REQUEST
        }
      }
    }
  }
  "handleIrregularPaymentError is called" must {
    s"redirect to tax summaries if emp id exists and user inputted ${IrregularPayConstants.ContactEmployer}" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> IrregularPayConstants.ContactEmployer)
      val mockRepository = mock[JourneyCacheNewRepository]
      val application = applicationBuilderWithoutRepository(userAnswers)
        .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
        .build()
      when(mockRepository.set(any)).thenReturn(Future.successful(true))

      running(application) {
        val result = controller(repository = mockRepository).handleIrregularPaymentError(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }
    s"redirect to updateEmploymentDetails if emp id exists and user inputted anything besides ${IrregularPayConstants.ContactEmployer}" in {
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> IrregularPayConstants.UpdateDetails)
      val mockRepository = mock[JourneyCacheNewRepository]
      val application = applicationBuilderWithoutRepository(userAnswers)
        .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
        .build()
      when(mockRepository.set(any)).thenReturn(Future.successful(true))

      running(application) {
        val result = controller(repository = mockRepository).handleIrregularPaymentError(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
          .endEmploymentPage()
          .url
        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }
    "return BAD_REQUEST if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).handleIrregularPaymentError(fakePostRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if user answers data exists but employment data does not and there are form errors" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> "")
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller(Some(userAnswers)).handleIrregularPaymentError(request)
        status(result) mustBe BAD_REQUEST
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
    "redirect to employmentUpdateRemove when there is no employment id in the user answers and no tracked successful journey in cache" in {
      when(trackSuccessJourneyCacheService.currentValue(any())(any()))
        .thenReturn(Future.successful(None))
      val empId = 1
      val emptyUserAnswers = userAnswers.copy(data = Json.obj())
      val mockRepository = mock[JourneyCacheNewRepository]
      val application = applicationBuilderWithoutRepository(emptyUserAnswers)
        .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
        .build()
      when(mockRepository.set(any)).thenReturn(Future.successful(true))

      running(application) {
        val result = controller(Some(emptyUserAnswers), mockRepository).onPageLoad(empId)(fakeGetRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.employmentUpdateRemoveDecision().url
        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }
    "redirect to employmentUpdateRemove when there is an employment id in the user answers and no tracked successful journey in cache" in {
      when(trackSuccessJourneyCacheService.currentValue(any())(any()))
        .thenReturn(Future.successful(None))
      val empId = 1
      val mockRepository = mock[JourneyCacheNewRepository]
      val application = applicationBuilderWithoutRepository(userAnswers)
        .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
        .build()
      when(mockRepository.set(any)).thenReturn(Future.successful(true))

      running(application) {
        val result = controller(repository = mockRepository).onPageLoad(empId)(fakeGetRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.employmentUpdateRemoveDecision().url
        verify(mockRepository, never).set(
          eqTo(emptyUserAnswers.copy(data = Json.obj(EndEmploymentIdPage.toString -> empId)))
        )
      }
    }
    "redirect to duplicateSubmissionWarning when there is no employment id in the user answers and a tracked successful journey in cache" in {
      when(trackSuccessJourneyCacheService.currentValue(any())(any()))
        .thenReturn(Future.successful(Some("test")))
      val empId = 1
      val mockRepository = mock[JourneyCacheNewRepository]
      val emptyUserAnswers = userAnswers.copy(data = Json.obj())
      val application = applicationBuilderWithoutRepository(emptyUserAnswers)
        .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
        .build()
      when(mockRepository.set(any)).thenReturn(Future.successful(true))

      running(application) {
        val result = controller(Some(emptyUserAnswers), mockRepository).onPageLoad(empId)(fakeGetRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.duplicateSubmissionWarning().url
        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }
    "redirect to duplicateSubmissionWarning when there is an employment id in the user answers and a tracked successful journey in cache" in {
      when(trackSuccessJourneyCacheService.currentValue(any())(any()))
        .thenReturn(Future.successful(Some("test")))
      val empId = 1
      val mockRepository = mock[JourneyCacheNewRepository]
      val application = applicationBuilderWithoutRepository(userAnswers)
        .overrides(bind[JourneyCacheNewRepository].toInstance(mockRepository))
        .build()
      when(mockRepository.set(any)).thenReturn(Future.successful(true))

      running(application) {
        val result = controller(repository = mockRepository).onPageLoad(empId)(fakeGetRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.duplicateSubmissionWarning().url
        verify(mockRepository, never).set(any())
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
    "return BAD_REQUEST if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).duplicateSubmissionWarning()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if user answers data exists but employment data does not" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().duplicateSubmissionWarning()(fakeGetRequest)
        status(result) mustBe BAD_REQUEST
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
    "return BAD_REQUEST if no user answers data exists" in {
      val userAnswersEmpty = userAnswers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswersEmpty).build()

      running(application) {
        val result = controller(Some(userAnswersEmpty)).submitDuplicateSubmissionWarning()(fakePostRequest)
        status(result) mustBe BAD_REQUEST
      }
    }
    "return BAD_REQUEST if user answers data exists but employment data does not" in {
      when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val result = controller().submitDuplicateSubmissionWarning()(fakePostRequest)
        status(result) mustBe BAD_REQUEST
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
    "redirect to the the IncomeSourceSummarycontroller() if cache successfully clears" in {
      val employmentId = 1
      when(mockRepository.clear(any(), any())).thenReturn(Future.successful(true))

      val result = controller().cancel(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url
    }
    "redirect to the the IncomeSourceSummarycontroller() if cache fails to clear" in {
      val employmentId = 1
      when(mockRepository.clear(any(), any())).thenReturn(Future.successful(false))

      val result = controller().cancel(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url
    }
  }

  def employmentWithAccounts(accounts: List[AnnualAccount]): Employment =
    Employment(
      "employer",
      Live,
      Some("emp123"),
      Some(LocalDate.of(2000, 5, 20)),
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
