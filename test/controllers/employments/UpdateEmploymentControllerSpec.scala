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
import pages.updateEmployment._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.Results.NotFound
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome, IncorrectIncome}
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import utils.{BaseSpec, FakeAuthJourney}
import views.html.CanWeContactByPhoneView
import views.html.employments.ConfirmationView
import views.html.employments.update.{UpdateEmploymentCheckYourAnswersView, WhatDoYouWantToTellUsView}

import java.time.LocalDate
import scala.concurrent.Future

class UpdateEmploymentControllerSpec extends BaseSpec {

  private val employment = Employment(
    "company name",
    Live,
    Some("123"),
    Some(LocalDate.parse("2016-05-26")),
    Some(LocalDate.parse("2016-05-26")),
    "",
    "",
    2,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  val personService: PersonService                = mock[PersonService]
  val employmentService: EmploymentService        = mock[EmploymentService]
  lazy val mockRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  override val userAnswers: UserAnswers = UserAnswers(
    RequestBuilder.uuid,
    nino.nino,
    Json.obj(
      "end-employment-employmentId" -> 1
    )
  )

  def controller(
    userAnswersAsArg: Option[UserAnswers] = None
  ) = new UpdateEmploymentController(
    employmentService,
    mock[AuditConnector],
    new FakeAuthJourney(userAnswersAsArg.getOrElse(userAnswers)),
    mcc,
    inject[WhatDoYouWantToTellUsView],
    inject[CanWeContactByPhoneView],
    inject[UpdateEmploymentCheckYourAnswersView],
    inject[ConfirmationView],
    inject[ErrorPagesHandler],
    mockRepository,
    mockEmpIdCheck
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(personService, mockRepository, employmentService)
    when(mockRepository.set(any)).thenReturn(Future.successful(true))
    when(mockRepository.clear(any(), any())).thenReturn(Future.successful(true))
    when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswers)))
  }

  "employmentDetailsUpdate" must {
    "show the 'What Do You Want To Tell Us' Page" when {
      "the request has an authorised session" in {
        when(employmentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        val userAnswersUpdated =
          userAnswers.copy(data =
            userAnswers.data ++ Json
              .obj(UpdateEmploymentIdPage.toString -> 1, UpdateEmploymentNamePage.toString -> employment.name)
          )

        when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswersUpdated)))

        val result = controller(Some(userAnswersUpdated)).updateEmploymentDetails(1)(
          RequestBuilder.buildFakeRequestWithAuth("GET")
        )

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updateEmployment.whatDoYouWantToTellUs.pagetitle"))
      }
    }

    "retrieve the employer name from the cache" when {
      "the request has an authorised session" in {
        when(employmentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        val userAnswersUpdated =
          userAnswers.copy(data =
            userAnswers.data ++ Json
              .obj(UpdateEmploymentIdPage.toString -> 1, UpdateEmploymentNamePage.toString -> employment.name)
          )

        when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswersUpdated)))

        val result = controller(Some(userAnswersUpdated)).updateEmploymentDetails(1)(
          RequestBuilder.buildFakeRequestWithAuth("GET")
        )

        status(result) mustBe OK

        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }
    "retrieve the employment update details from the cache" when {
      "the request has an authorised session" in {
        when(employmentService.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        val userAnswersUpdated =
          userAnswers.copy(data =
            userAnswers.data ++ Json
              .obj(
                UpdateEmploymentIdPage.toString      -> 1,
                UpdateEmploymentNamePage.toString    -> employment.name,
                UpdateEmploymentDetailsPage.toString -> "updateDetails"
              )
          )

        when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswersUpdated)))

        val result = controller(Some(userAnswersUpdated)).updateEmploymentDetails(1)(
          RequestBuilder.buildFakeRequestWithAuth("GET")
        )

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title()  must include(Messages("tai.updateEmployment.whatDoYouWantToTellUs.pagetitle"))
        doc.toString must include("updateDetails")
        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }

    "throw exception" when {
      "employment not found" in {
        when(employmentService.employmentOnly(any(), any(), any())(any())).thenReturn(Future.successful(None))
        val userAnswersUpdated =
          userAnswers.copy(data =
            userAnswers.data ++ Json
              .obj(
                UpdateEmploymentDetailsPage.toString -> "updateDetails"
              )
          )
        val result             = controller(Some(userAnswersUpdated)).updateEmploymentDetails(1)(
          RequestBuilder.buildFakeRequestWithAuth("GET")
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
    "load a not found page from empIdCheck" when {
      "the empId doesn't match the provided one" in {
        when(mockEmpIdCheck.checkValidId(any(), any())(any()))
          .thenReturn(Future.successful(Some(NotFound("EmpId not found"))))

        val result = controller(None).updateEmploymentDetails(1)(
          RequestBuilder.buildFakeRequestWithAuth("GET")
        )

        status(result) mustBe NOT_FOUND
        verify(employmentService, times(0)).employmentOnly(any(), any(), any())(any())
      }
    }
  }

  "submitUpdateEmploymentDetails" must {

    "redirect to the 'TODO' page" when {
      "the form submission is valid" in {

        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentDetailsPage.toString -> ""
              )
          )

        val result = controller(Some(userAnswersUpdated)).submitUpdateEmploymentDetails(0)(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("employmentDetails", "test details"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
      }
    }

    "add employment details to the journey cache" when {
      "the form submission is valid" in {

        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentDetailsPage.toString -> "test details"
              )
          )

        when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswersUpdated)))

        val result = controller(Some(userAnswersUpdated)).submitUpdateEmploymentDetails(0)(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("employmentDetails", "test details"))
        )

        status(result) mustBe SEE_OTHER
        verify(mockRepository, times(1)).set(any[UserAnswers])
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {

        val employmentDetailsFormData = ("employmentDetails", "")

        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentNamePage.toString -> "Test"
              )
          )
        val result             = controller(Some(userAnswersUpdated)).submitUpdateEmploymentDetails(0)(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(employmentDetailsFormData)
        )

        status(result) mustBe BAD_REQUEST
      }
    }

    "not add employment details to the journey cache" when {
      "the form submission is invalid" in {

        val employmentDetailsFormData = ("employmentDetails", "")
        val userAnswersUpdated        =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentNamePage.toString -> "Test"
              )
          )

        when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswersUpdated)))

        val result = controller(Some(userAnswersUpdated)).submitUpdateEmploymentDetails(0)(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(employmentDetailsFormData)
        )

        status(result) mustBe BAD_REQUEST
        verify(mockRepository, times(0)).set(any[UserAnswers])
      }
    }
  }

  "addTelephoneNumber" must {
    "show the contact by telephone page" when {
      "valid details has been passed" in {
        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentIdPage.toString -> 1
              )
          )

        when(mockRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswersUpdated)))
        val result =
          controller(Some(userAnswersUpdated)).addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""
      }
      "we fetch telephone details form cache" in {
        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentIdPage.toString                -> 1,
                UpdateEmploymentTelephoneQuestionPage.toString -> FormValuesConstants.YesValue,
                UpdateEmploymentTelephoneNumberPage.toString   -> "01215485965"
              )
          )

        val result =
          controller(Some(userAnswersUpdated)).addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-2][checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice][checked]").size() mustBe 1
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe "01215485965"
      }
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      val result = controller().addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentTelephoneQuestionPage.toString -> FormValuesConstants.YesValue,
                UpdateEmploymentTelephoneNumberPage.toString   -> "12345678"
              )
          )

        val result = controller(Some(userAnswersUpdated)).submitTelephoneNumber()(
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
        ).get mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {

        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentTelephoneQuestionPage.toString -> FormValuesConstants.NoValue,
                UpdateEmploymentTelephoneNumberPage.toString   -> ""
              )
          )

        val result = controller(Some(userAnswersUpdated)).submitTelephoneNumber()(
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
        ).get mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentIdPage.toString -> 1
              )
          )

        val result = controller(Some(userAnswersUpdated)).submitTelephoneNumber()(
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
        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentIdPage.toString -> 1
              )
          )

        val tooFewCharsResult = controller(Some(userAnswersUpdated)).submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234"
            )
        )
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc         = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = controller(Some(userAnswersUpdated)).submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234123412341234123412341234123"
            )
        )
        status(tooManyCharsResult) mustBe BAD_REQUEST
        val tooManyDoc         = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooManyDoc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
  }

  "check your answers" must {
    "show summary page" when {
      "valid details has been passed" in {
        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentIdPage.toString                -> 1,
                UpdateEmploymentNamePage.toString              -> "emp-name",
                UpdateEmploymentDetailsPage.toString           -> "whatYouToldUs",
                UpdateEmploymentTelephoneQuestionPage.toString -> "Yes",
                UpdateEmploymentTelephoneNumberPage.toString   -> "123456789"
              )
          )

        val result = controller(Some(userAnswersUpdated)).updateEmploymentCheckYourAnswers()(
          RequestBuilder.buildFakeRequestWithAuth("GET")
        )
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }

    "redirect to the tax summary page if a value is missing from the cache " in {
      val result = controller().updateEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submit your answers" must {
    "invoke the back end 'incorrectEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {
        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "Yes", Some("123456789"))

        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentIdPage.toString                -> 1,
                UpdateEmploymentDetailsPage.toString           -> "whatYouToldUs",
                UpdateEmploymentTelephoneQuestionPage.toString -> "Yes",
                UpdateEmploymentTelephoneNumberPage.toString   -> "123456789"
              )
          )

        when(employmentService.incorrectEmployment(any(), meq(1), meq(incorrectEmployment))(any()))
          .thenReturn(Future.successful("1"))

        val result =
          controller(Some(userAnswersUpdated)).submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.confirmation().url
        verify(mockRepository, times(1)).clear(any(), any())

      }

      "the request has an authorised session and telephone number has not been provided" in {
        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "No", None)

        val userAnswersUpdated =
          userAnswers.copy(data =
            Json
              .obj(
                UpdateEmploymentIdPage.toString                -> 1,
                UpdateEmploymentDetailsPage.toString           -> "whatYouToldUs",
                UpdateEmploymentTelephoneQuestionPage.toString -> "No"
              )
          )

        when(employmentService.incorrectEmployment(any(), meq(1), meq(incorrectEmployment))(any()))
          .thenReturn(Future.successful("1"))

        val result =
          controller(Some(userAnswersUpdated)).submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.confirmation().url
        verify(mockRepository, times(1)).clear(any(), any())
      }
    }
  }

  "confirmation" must {
    "show the update employment confirmation page" when {
      "the request has an authorised session" in {
        val result = controller().confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc    = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.employmentConfirmation.heading"))
      }
    }
  }

  "cancel" must {
    "redirect to the the IncomeSourceSummaryController" in {
      val employmentId = 1

      val result = controller().cancel(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url
    }
  }
}
