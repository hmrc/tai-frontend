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
import controllers.actions.FakeValidatePerson
import controllers.{ErrorPagesHandler, FakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.scalatest.BeforeAndAfterEach
import pages.EmploymentUpdateRemovePage
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys
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
import utils.{FakeActionJourney, NewCachingBaseSpec}
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EndEmploymentControllerSpec extends NewCachingBaseSpec with BeforeAndAfterEach {

  private def fakeGetRequest = RequestBuilder.buildFakeRequestWithAuth("GET")

  private def fakePostRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

  val auditService: AuditService = mock[AuditService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val endEmploymentJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val trackSuccessJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  val userAnwsers: UserAnswers = UserAnswers(
    RequestBuilder.uuid,
    Json.obj(
      EndCompanyBenefitConstants.EmploymentNameKey -> employerName,
      EndCompanyBenefitConstants.EmploymentIdKey   -> 14
    )
  )

  val userAnswersWithYesOrNo =
    userAnwsers.copy(data = userAnwsers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> "Yes"))

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
    endEmploymentJourneyCacheService,
    trackSuccessJourneyCacheService,
    new FakeActionJourney(userAnswersAsArg.getOrElse(userAnwsers)),
    FakeAuthAction, // TODO - Create FakeActionJourney, these no longer used
    FakeValidatePerson
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
  when(mockSessionRepository.get(any())).thenReturn(Future.successful(Some(userAnwsers)))

  "employmentUpdateRemove" must {
    List(
      FormValuesConstants.YesValue,
      FormValuesConstants.NoValue
    ).foreach { yesOrNo =>
      s"call updateRemoveEmployer page successfully with an authorised session regardless of if optional cache is $yesOrNo" in {
        val request = fakeGetRequest
        val userAnswersWithYesOrNo =
          userAnwsers.copy(data = userAnwsers.data ++ Json.obj(EmploymentUpdateRemovePage.toString -> yesOrNo))
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
      val application = applicationBuilder(userAnswers = userAnwsers).build()

      running(application) {
        val result = controller(Some(userAnwsers)).employmentUpdateRemoveDecision(request)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(messages("tai.employment.decision.legend", employerName))
      }
    }
    "redirect to the tax summary page if the employer id is missing from the cache" in {
      val request = RequestBuilder.buildFakeRequestWithOnlySession("GET")
      val emptyUserAnswers = userAnwsers.copy(data = Json.obj())
      val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

      running(application) {
        val result = controller(Some(emptyUserAnswers)).employmentUpdateRemoveDecision(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
    "redirect to the tax summary page if the call to retrieve employments fails" in {
      when(employmentService.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))
      val request = fakeGetRequest
      val application = applicationBuilder(userAnswers = userAnwsers).build()

      running(application) {
        val result = controller(Some(userAnwsers)).employmentUpdateRemoveDecision(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
  }

  "handleEmploymentUpdateRemove" must {
    "redirect to the update employment url" when {
      "the form has the value Yes in EmploymentDecision" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.YesValue)

        val result = controller().handleEmploymentUpdateRemove(request)

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _               => ""
        }
        redirectUrl mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(1).url
      }
    }

    "redirect to the update within 6 week error page" when {
      "the form has the value No in EmploymentDecision and the employment has a payment within 6 weeks of todays date" in {

        val payment = paymentOnDate(LocalDate.now().minusWeeks(5)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val request = fakePostRequest.withFormUrlEncodedBody("employmentDecision" -> "No")

        val result = controller().handleEmploymentUpdateRemove(request)

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result) match {
          case Some(s: String) => s
          case _               => ""
        }

        redirectUrl mustBe controllers.employments.routes.EndEmploymentController.endEmploymentError().url
      }

      "the form has the value No in EmploymentDecision and the employment has a payment 6 weeks of todays date" in {

        val payment = paymentOnDate(LocalDate.now().minusWeeks(6))
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))

        val request = fakePostRequest.withFormUrlEncodedBody(
          EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.NoValue
        )

        val result = controller().handleEmploymentUpdateRemove(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.EndEmploymentController.endEmploymentError().url
      }

      "cache the employment details for error page" in {

        val date = LocalDate.now().minusWeeks(6)

        val payment = paymentOnDate(date)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))

        val dataToCache = Map(
          EndEmploymentConstants.LatestPaymentDateKey -> date.toString,
          EndEmploymentConstants.NameKey              -> "employer name"
        )
        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(endEmploymentJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(dataToCache))

        val request = fakePostRequest.withFormUrlEncodedBody(
          EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.NoValue
        )

        Await.result(controller().handleEmploymentUpdateRemove(request), 5 seconds)
      }
    }

    "redirect to the irregular payment error page" when {
      "the form has the value No in EmploymentDecision and the employment has a irregular payment" in {

        val request = fakePostRequest.withFormUrlEncodedBody(
          EmploymentDecisionConstants.EmploymentDecision -> FormValuesConstants.NoValue
        )

        val payment = paymentOnDate(LocalDate.now().minusWeeks(8)).copy(payFrequency = Irregular)
        val annualAccount = AnnualAccount(TaxYear(), Available, List(payment), Nil)
        val employment = employmentWithAccounts(List(annualAccount))
        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        when(employmentService.employment(any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(auditService.createAndSendAuditEvent(any(), any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = controller().handleEmploymentUpdateRemove(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.EndEmploymentController.irregularPaymentError().url
      }
    }

    "render the what do you want to do page with form errors" when {
      "no value is present in EmploymentDecision" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val request = FakeRequest("POST", "")
          .withFormUrlEncodedBody(EmploymentDecisionConstants.EmploymentDecision -> "")

        val result = controller().handleEmploymentUpdateRemove(request)

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "tell us about employment error page" must {
    "show the error page" in {

      val dataFromCache = Right(Seq(LocalDate.now.minusWeeks(6).minusDays(1).toString, employerName, "1"))

      when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
        .thenReturn(Future.successful(dataFromCache))

      val result = controller().endEmploymentError()(fakeGetRequest)
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() must include(
        Messages(
          "tai.endEmploymentWithinSixWeeksError.heading",
          LocalDate.now.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        )
      )
    }

    "show the irregular payment error page" in {

      val employmentId = 1

      when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
        .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

      val result = controller().irregularPaymentError(fakeGetRequest)
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() must include(Messages("tai.irregular.preHeadingText"))
    }

    "submit the details to backend" in {

      val employmentId = "0"
      val dataFromCache = Right((Seq(employmentId, LocalDate.of(2017, 2, 1).toString, "Yes"), Seq(Some("EXT-TEST"))))
      val cacheMap = Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$employmentId" -> "true")

      when(endEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
        .thenReturn(Future.successful(dataFromCache))
      when(employmentService.endEmployment(any(), any(), any())(any())).thenReturn(Future.successful("123-456-789"))
      when(trackSuccessJourneyCacheService.cache(meq(cacheMap))(any())).thenReturn(Future.successful(cacheMap))
      when(endEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

      val result = controller().confirmAndSendEndEmployment()(fakeGetRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.EndEmploymentController.showConfirmationPage().url
    }
  }

  "tellUsAboutEmploymentPage" must {
    "call tellUsAboutEmploymentPage() successfully with an authorised session" in {

      val employmentId = 1

      when(endEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right((Seq(employerName, employmentId.toString), Seq()))))

      val result = controller().endEmploymentPage(fakeGetRequest)
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() must include(Messages("tai.endEmployment.endDateForm.pagetitle"))
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      when(endEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
        .thenReturn(Future.successful(Left("Mandatory values missing from cache")))

      val result = controller().endEmploymentPage(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }

  "Calling the handleTellUsAboutEmploymentPage method" must {
    "call processTellUsAboutEmploymentPage successfully with an authorised session" in {

      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> "01",
          EmploymentEndDateForm.EmploymentFormMonth -> "02",
          EmploymentEndDateForm.EmploymentFormYear  -> "2017"
        )

      val result = controller().handleEndEmploymentPage(0)(request)

      status(result) mustBe SEE_OTHER
    }

    "reload the page when there are form errors" in {

      val request = FakeRequest("POST", "/")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> "01",
          EmploymentEndDateForm.EmploymentFormMonth -> "02",
          EmploymentEndDateForm.EmploymentFormYear  -> "abc"
        )

      val result = controller().handleEndEmploymentPage(0)(request)

      status(result) mustBe BAD_REQUEST
    }

    "redirect to telephone page" in {

      val dataToCache = Map(
        EndEmploymentConstants.EmploymentIdKey -> "0",
        EndEmploymentConstants.NameKey         -> employerName,
        EndEmploymentConstants.EndDateKey      -> LocalDate.of(2017, 2, 1).toString
      )

      when(endEmploymentJourneyCacheService.cache(meq(dataToCache))(any()))
        .thenReturn(Future.successful(dataToCache))

      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> "01",
          EmploymentEndDateForm.EmploymentFormMonth -> "02",
          EmploymentEndDateForm.EmploymentFormYear  -> "2017"
        )

      val result = controller().handleEndEmploymentPage(0)(request)

      status(result) mustBe SEE_OTHER
    }

    "save data into journey cache" in {

      val dataToCache = Map(EndEmploymentConstants.EndDateKey -> LocalDate.of(2017, 2, 1).toString)

      when(endEmploymentJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(dataToCache))

      val request = FakeRequest("POST", "")
        .withFormUrlEncodedBody(
          EmploymentEndDateForm.EmploymentFormDay   -> "01",
          EmploymentEndDateForm.EmploymentFormMonth -> "02",
          EmploymentEndDateForm.EmploymentFormYear  -> "2017"
        )

      Await.result(controller().handleEndEmploymentPage(0)(request), 5 seconds)
      verify(endEmploymentJourneyCacheService, times(1)).cache(any())(any())
    }

    "check your answers page" must {
      "show the check your answers page" in {

        val dataFromCache = Right((Seq("0", LocalDate.of(2017, 2, 1).toString, "No"), Seq(Some("EXT-TEST"))))

        when(
          endEmploymentJourneyCacheService.collectedJourneyValues(
            any(classOf[scala.collection.immutable.List[String]]),
            any(classOf[scala.collection.immutable.List[String]])
          )(any(), any())
        ).thenReturn(Future.successful(dataFromCache))

        val result = controller().endEmploymentCheckYourAnswers()(fakeGetRequest)
        val doc = Jsoup.parse(contentAsString(result))

        status(result) mustBe OK
        doc.title() must include(Messages("tai.endEmploymentConfirmAndSend.heading"))
      }

      "redirect to the summary page if a value is missing from the cache " in {

        when(
          endEmploymentJourneyCacheService.collectedJourneyValues(
            any(classOf[scala.collection.immutable.List[String]]),
            any(classOf[scala.collection.immutable.List[String]])
          )(any(), any())
        )
          .thenReturn(Future.successful(Left("An error has occurred")))

        val result = controller().endEmploymentCheckYourAnswers()(fakePostRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }

      "submit the details to backend" in {

        val empId = 0
        val dataFromCache =
          Right((Seq(empId.toString, LocalDate.of(2017, 2, 1).toString, "Yes"), Seq(Some("EXT-TEST"))))

        when(endEmploymentJourneyCacheService.collectedJourneyValues(any(), any())(any(), any()))
          .thenReturn(Future.successful(dataFromCache))
        when(employmentService.endEmployment(any(), any(), any())(any())).thenReturn(Future.successful("123-456-789"))
        when(
          trackSuccessJourneyCacheService
            .cache(meq(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId"), meq("true"))(any())
        )
          .thenReturn(
            Future.successful(Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId" -> "true"))
          )
        when(endEmploymentJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = controller().confirmAndSendEndEmployment()(fakeGetRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.EndEmploymentController.showConfirmationPage().url
        verify(endEmploymentJourneyCacheService, times(1)).flush()(any())
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
  }

  "add telephone number" must {
    "show the contact by telephone page" when {
      "the request has an authorised session and there is cached data" in {

        when(
          endEmploymentJourneyCacheService.mandatoryJourneyValueAsInt(meq(EndEmploymentConstants.EmploymentIdKey))(
            any()
          )
        ).thenReturn(Future.successful(Right(0)))
        when(endEmploymentJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(Some("yes"), Some("123456789"))))

        val result = controller().addTelephoneNumber()(fakeGetRequest)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

      "the request has an authorised session no cached data" in {

        when(
          endEmploymentJourneyCacheService.mandatoryJourneyValueAsInt(meq(EndEmploymentConstants.EmploymentIdKey))(
            any()
          )
        ).thenReturn(Future.successful(Right(0)))
        when(endEmploymentJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(None, None)))

        val result = controller().addTelephoneNumber()(fakeGetRequest)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

      "redirect to the tax summary page if a value is missing from the cache " in {

        when(
          endEmploymentJourneyCacheService.mandatoryJourneyValueAsInt(meq(EndEmploymentConstants.EmploymentIdKey))(
            any()
          )
        )
          .thenReturn(Future.successful(Left("Mandatory value missing from cache")))
        when(endEmploymentJourneyCacheService.optionalValues(any())(any(), any()))
          .thenReturn(Future.successful(Seq(None, None)))

        val result = controller().addTelephoneNumber()(fakeGetRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {

        val expectedCache =
          Map(
            EndEmploymentConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndEmploymentConstants.TelephoneNumberKey   -> "12345678"
          )
        when(endEmploymentJourneyCacheService.cache(meq(expectedCache))(any()))
          .thenReturn(Future.successful(expectedCache))
        val result = controller().submitTelephoneNumber()(
          fakePostRequest.withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "12345678"
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {

        val expectedCacheWithErasingNumber =
          Map(
            EndEmploymentConstants.TelephoneQuestionKey -> FormValuesConstants.NoValue,
            EndEmploymentConstants.TelephoneNumberKey   -> ""
          )
        when(endEmploymentJourneyCacheService.cache(meq(expectedCacheWithErasingNumber))(any()))
          .thenReturn(Future.successful(expectedCacheWithErasingNumber))
        val result = controller().submitTelephoneNumber()(
          fakePostRequest
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.NoValue,
              FormValuesConstants.YesNoTextEntry -> "this value must not be cached"
            )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {

        val empId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValueAsInt(any())(any()))
          .thenReturn(Future.successful(Right(empId)))

        val result = controller().submitTelephoneNumber()(
          fakePostRequest.withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> ""
          )
        )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

      "there is a form validation error (additional, controller() specific constraint)" in {

        val empId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValueAsInt(any())(any()))
          .thenReturn(Future.successful(Right(empId)))

        val tooFewCharsResult = controller().submitTelephoneNumber()(
          fakePostRequest.withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "1234"
          )
        )
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = controller().submitTelephoneNumber()(
          fakePostRequest
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

  "handleIrregularPay" must {
    "return bad request" when {
      "there are errors in form" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val result = controller().handleIrregularPaymentError(fakePostRequest.withFormUrlEncodedBody())

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to income summary view" when {
      "user selected an option to contact the employer" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val result = controller().handleIrregularPaymentError(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> IrregularPayConstants.ContactEmployer)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }

    "redirect to end employment journey" when {
      "user selected an option to update the details" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val result = controller().handleIrregularPaymentError(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(IrregularPayConstants.IrregularPayDecision -> IrregularPayConstants.UpdateDetails)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.EndEmploymentController
          .endEmploymentPage()
          .url
      }
    }
  }

  "onPageLoad" must {
    "redirect to employmentUpdateRemove when there is no end employment ID cache value present" in {
      val employmentId = 1

      val cacheMap = Map(
        EndEmploymentConstants.EmploymentIdKey -> employmentId.toString,
        EndEmploymentConstants.NameKey         -> employerName
      )
      when(endEmploymentJourneyCacheService.cache(meq(cacheMap))(any())).thenReturn(Future.successful(cacheMap))
      when(
        trackSuccessJourneyCacheService.currentValue(
          meq(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$employmentId")
        )(any())
      )
        .thenReturn(Future.successful(None))

      val result = controller().onPageLoad(employmentId)(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.EndEmploymentController.employmentUpdateRemoveDecision().url
    }

    "redirect to warning page when there is an end employment ID cache value present" in {
      val employmentId = 1

      val cacheMap = Map(
        EndEmploymentConstants.EmploymentIdKey -> employmentId.toString,
        EndEmploymentConstants.NameKey         -> employerName
      )
      when(endEmploymentJourneyCacheService.cache(meq(cacheMap))(any())).thenReturn(Future.successful(cacheMap))
      when(
        trackSuccessJourneyCacheService.currentValue(
          meq(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$employmentId")
        )(any())
      )
        .thenReturn(Future.successful(Some("true")))

      val result = controller().onPageLoad(employmentId)(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.EndEmploymentController.duplicateSubmissionWarning().url
    }
  }

  "duplicateSubmissionWarning" must {
    "show duplicateSubmissionWarning view" in {

      val employmentId = 1

      when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
        .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

      val result = controller().duplicateSubmissionWarning(fakeGetRequest)
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() must include(Messages("tai.employment.warning.customGaTitle"))
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
        .thenReturn(Future.successful(Left("Mandatory values missing from cache")))

      val result = controller().duplicateSubmissionWarning(fakeGetRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }

  "submitDuplicateSubmissionWarning" must {
    "redirect to the update remove employment decision page" when {
      "I want to update my employment is selected" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val result = controller().submitDuplicateSubmissionWarning(
          fakePostRequest
            .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision().url
      }
    }

    "redirect to the income source summary page" when {
      "I want to return to my employment details is selected" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val result = controller().submitDuplicateSubmissionWarning(
          fakePostRequest
            .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController
          .onPageLoad(employmentId)
          .url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {

        val employmentId = 1

        when(endEmploymentJourneyCacheService.mandatoryJourneyValues(any())(any(), any()))
          .thenReturn(Future.successful(Right(Seq(employerName, employmentId.toString))))

        val result = controller().submitDuplicateSubmissionWarning(
          fakePostRequest
            .withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> "")
        )

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
