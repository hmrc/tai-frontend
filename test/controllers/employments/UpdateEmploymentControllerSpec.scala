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

import akka.Done
import builders.RequestBuilder
import controllers.ErrorPagesHandler
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{Employment, IncorrectIncome}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec
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
    Nil,
    "",
    "",
    2,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false
  )

  private def createSUT = new SUT

  val personService: PersonService = mock[PersonService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val successfulJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val employmentService: EmploymentService = mock[EmploymentService]

  private class SUT
      extends UpdateEmploymentController(
        employmentService,
        mock[AuditConnector],
        mockAuthJourney,
        mcc,
        inject[WhatDoYouWantToTellUsView],
        inject[CanWeContactByPhoneView],
        inject[UpdateEmploymentCheckYourAnswersView],
        inject[ConfirmationView],
        journeyCacheService,
        successfulJourneyCacheService,
        inject[ErrorPagesHandler]
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(journeyCacheService, successfulJourneyCacheService, personService)
  }

  "employmentDetailsUpdate" must {
    "show the 'What Do You Want To Tell Us' Page" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        val cache =
          Map(UpdateEmploymentConstants.EmploymentIdKey -> "1", UpdateEmploymentConstants.NameKey -> employment.name)
        when(journeyCacheService.cache(meq(cache))(any())).thenReturn(Future.successful(cache))
        when(journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updateEmployment.whatDoYouWantToTellUs.pagetitle"))
      }
    }

    "retrieve the employer name from the cache" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        val cache =
          Map(UpdateEmploymentConstants.EmploymentIdKey -> "1", UpdateEmploymentConstants.NameKey -> employment.name)
        when(journeyCacheService.cache(meq(cache))(any())).thenReturn(Future.successful(cache))
        when(journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))

        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        verify(journeyCacheService, times(1)).cache(meq(cache))(any())
      }
    }
    "retrieve the employment update details from the cache" when {
      "the request has an authorised session" in {
        val sut = createSUT
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        val cacheDetails = Some("updateDetails")
        when(journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(cacheDetails))
        val cache =
          Map(UpdateEmploymentConstants.EmploymentIdKey -> "1", UpdateEmploymentConstants.NameKey -> employment.name)
        when(journeyCacheService.cache(meq(cache))(any())).thenReturn(Future.successful(cache))

        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updateEmployment.whatDoYouWantToTellUs.pagetitle"))
        doc.toString must include("updateDetails")
        verify(journeyCacheService, times(1)).currentValue(any())(any())
      }
    }

    "throw exception" when {
      "employment not found" in {
        val sut = createSUT
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))
        when(journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(Some("updateDetails")))
        val result = sut.updateEmploymentDetails(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submitUpdateEmploymentDetails" must {

    "redirect to the 'TODO' page" when {
      "the form submission is valid" in {

        val sut = createSUT

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(
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

        val sut = createSUT

        val employmentDetailsFormData = ("employmentDetails", "test details")
        val employmentDetails = Map("employmentDetails" -> "test details")

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(employmentDetailsFormData)
        )

        status(result) mustBe SEE_OTHER

        verify(journeyCacheService, times(1)).cache(meq(employmentDetails))(any())
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {

        val sut = createSUT

        val employmentDetailsFormData = ("employmentDetails", "")

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddEmploymentConstants.NameKey -> "Test")))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(employmentDetailsFormData)
        )

        status(result) mustBe BAD_REQUEST
      }
    }

    "not add employment details to the journey cache" when {
      "the form submission is invalid" in {

        val sut = createSUT

        val employmentDetailsFormData = ("employmentDetails", "")
        val employmentDetails = Map("employmentDetails" -> "")

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(AddEmploymentConstants.NameKey -> "Test")))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitUpdateEmploymentDetails(0)(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(employmentDetailsFormData)
        )

        status(result) mustBe BAD_REQUEST

        verify(journeyCacheService, never).cache(meq(employmentDetails))(any())
      }
    }
  }

  "addTelephoneNumber" must {
    "show the contact by telephone page" when {
      "valid details has been passed" in {
        val sut = createSUT
        when(journeyCacheService.mandatoryJourneyValueAsInt(any())(any())).thenReturn(Future.successful(Right(1)))
        when(journeyCacheService.optionalValues(any())(any(), any())).thenReturn(Future.successful(Seq(None, None)))

        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.select("input[id=yesNoChoice-no][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoChoice-yes][checked=checked]").size() mustBe 0
        doc.select("input[id=yesNoTextEntry]").get(0).attributes().get("value") mustBe ""
      }
      "we fetch telephone details form cache" in {
        val sut = createSUT
        when(journeyCacheService.mandatoryJourneyValueAsInt(any())(any())).thenReturn(Future.successful(Right(1)))
        when(journeyCacheService.optionalValues(any())(any(), any()))
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

    "redirect to the tax summary page if a value is missing from the cache " in {

      val sut = createSUT

      when(journeyCacheService.mandatoryJourneyValueAsInt(any())(any()))
        .thenReturn(Future.successful(Left("Mandatory value missing from cache")))
      when(journeyCacheService.optionalValues(any())(any(), any())).thenReturn(Future.successful(Seq(None, None)))

      val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT
        val expectedCache =
          Map(
            UpdateEmploymentConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            UpdateEmploymentConstants.TelephoneNumberKey   -> "12345678"
          )
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(expectedCache))

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
        ).get mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber =
          Map(
            UpdateEmploymentConstants.TelephoneQuestionKey -> FormValuesConstants.NoValue,
            UpdateEmploymentConstants.TelephoneNumberKey   -> ""
          )
        when(journeyCacheService.cache(any())(any()))
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
        ).get mustBe controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT
        val cache = Map(UpdateEmploymentConstants.EmploymentIdKey -> "1")
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

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
        val cache = Map(UpdateEmploymentConstants.EmploymentIdKey -> "1")
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

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
    "show summary page" when {
      "valid details has been passed" in {
        val sut = createSUT

        when(
          journeyCacheService.collectedJourneyValues(
            any(classOf[scala.collection.immutable.List[String]]),
            any(classOf[scala.collection.immutable.List[String]])
          )(any(), any())
        ).thenReturn(
          Future.successful(
            Right(
              (
                Seq[String]("1", "emp-name", "whatYouToldUs", "Yes"),
                Seq[Option[String]](Some("123456789"))
              )
            )
          )
        )

        val result = sut.updateEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      val sut = createSUT

      when(
        journeyCacheService.collectedJourneyValues(
          any(classOf[scala.collection.immutable.List[String]]),
          any(classOf[scala.collection.immutable.List[String]])
        )(any(), any())
      )
        .thenReturn(Future.successful(Left("An error has occurred")))

      val result = sut.updateEmploymentCheckYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submit your answers" must {
    "invoke the back end 'incorrectEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {
        val sut = createSUT
        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "Yes", Some("123456789"))
        val empId = 1
        when(journeyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
          Future.successful(
            Right(
              (
                Seq[String](empId.toString, "whatYouToldUs", "Yes"),
                Seq[Option[String]](Some("123456789"))
              )
            )
          )
        )
        when(employmentService.incorrectEmployment(any(), meq(1), meq(incorrectEmployment))(any(), any()))
          .thenReturn(Future.successful("1"))
        when(
          successfulJourneyCacheService
            .cache(meq(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId"), meq("true"))(any())
        )
          .thenReturn(
            Future.successful(Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId" -> "true"))
          )
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.confirmation().url
        verify(journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has not been provided" in {
        val sut = createSUT
        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "No", None)
        val empId = 1
        when(journeyCacheService.collectedJourneyValues(any(), any())(any(), any())).thenReturn(
          Future.successful(
            Right(
              (
                Seq[String](empId.toString, "whatYouToldUs", "No"),
                Seq[Option[String]](None)
              )
            )
          )
        )
        when(employmentService.incorrectEmployment(any(), meq(1), meq(incorrectEmployment))(any(), any()))
          .thenReturn(Future.successful("1"))
        when(
          successfulJourneyCacheService
            .cache(meq(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId"), meq("true"))(any())
        )
          .thenReturn(
            Future.successful(Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId" -> "true"))
          )
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.employments.routes.UpdateEmploymentController.confirmation().url
        verify(journeyCacheService, times(1)).flush()(any())
      }
    }
  }

  "confirmation" must {
    "show the update employment confirmation page" when {
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
    "redirect to the the IncomeSourceSummaryController" in {
      val employmentId = 1
      when(journeyCacheService.flush()(any())).thenReturn(Future.successful(Done))

      val result = createSUT.cancel(employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(employmentId).url
    }
  }

}
