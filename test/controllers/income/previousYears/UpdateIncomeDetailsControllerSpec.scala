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

package controllers.income.previousYears

import akka.Done
import builders.RequestBuilder
import controllers.{ErrorPagesHandler, FakeAuthAction}
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, UpdateHistoricIncomeChoiceConstants}
import uk.gov.hmrc.tai.util.constants.journeyCache._
import utils.BaseSpec
import views.html.CanWeContactByPhoneView
import views.html.incomes.previousYears.{CheckYourAnswersView, UpdateIncomeDetailsConfirmationView, UpdateIncomeDetailsDecisionView, UpdateIncomeDetailsView}

import scala.concurrent.Future

class UpdateIncomeDetailsControllerSpec extends BaseSpec with BeforeAndAfterEach {

  override def beforeEach: Unit =
    Mockito.reset(journeyCacheService, trackingjourneyCacheService)

  "decision" must {
    "return ok" in {
      val SUT = createSUT
      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

      val result = SUT.decision(previousTaxYear)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(
        Messages("tai.income.previousYears.decision.title", TaxPeriodLabelService.taxPeriodLabel(previousTaxYear.year)))
    }
  }

  "submitDecision" must {
    "redirect to the details page" when {
      "the form has the value Yes in UpdateIncomeDecision" in {
        val SUT = createSUT
        val request =
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              UpdateHistoricIncomeChoiceConstants.UpdateIncomeChoice -> FormValuesConstants.YesValue)
        val result = SUT.submitDecision()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.PayeControllerHistoric.payePage(previousTaxYear).url)
      }
    }

    "redirect to the Historic Paye page" when {
      "the form has the value No in UpdateIncomeDecision" in {
        val SUT = createSUT
        val request =
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              UpdateHistoricIncomeChoiceConstants.UpdateIncomeChoice -> FormValuesConstants.NoValue)
        val result = SUT.submitDecision()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.previousYears.routes.UpdateIncomeDetailsController.details.url)
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {
        val sut = createSUT
        val result = sut.submitDecision()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(UpdateHistoricIncomeChoiceConstants.UpdateIncomeChoice -> ""))
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "details" must {
    "show 'What Do You Want To Tell Us' Page" when {
      "the request has an authorised session with Tax Year" in {
        val SUT = createSUT
        val taxYear = TaxYear().prev.year.toString
        val cache = Map(UpdatePreviousYearsIncomeConstants.TaxYearKey -> taxYear)
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))
        val result = SUT.details()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.income.previousYears.details.title"))
      }
    }
  }

  "submitDetails" must {
    "redirect to the 'Add Telephone Number' page" when {
      "the form submission is valid" in {
        val SUT = createSUT
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitDetails()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("employmentDetails", "test details")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber.url
      }
    }

    "add income details to the journey cache" when {
      "the form submission is valid" in {
        val SUT = createSUT

        val incomeDetailsFormData = ("employmentDetails", "test details")
        val incomeDetails = Map("incomeDetails" -> "test details")

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitDetails()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(incomeDetailsFormData))

        status(result) mustBe SEE_OTHER
        verify(journeyCacheService, times(1)).cache(mockEq(incomeDetails))(any())
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {
        val SUT = createSUT
        val employmentDetailsFormData = ("employmentDetails", "")

        when(journeyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(UpdatePreviousYearsIncomeConstants.TaxYearKey -> "2016")))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitDetails()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(employmentDetailsFormData))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "telephoneNumber" must {
    "show the contact by telephone page" when {
      "valid details have been passed" in {
        val sut = createSUT

        val taxYear = TaxYear().prev.year.toString
        val cache = Map(UpdatePreviousYearsIncomeConstants.TaxYearKey -> taxYear)
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(journeyCacheService.currentValue(any())(any())).thenReturn(Future.successful(None))
        val result = sut.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
  }

  "submitTelephoneNumber" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val sut = createSUT
        val expectedCache = Map(
          UpdatePreviousYearsIncomeConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
          UpdatePreviousYearsIncomeConstants.TelephoneNumberKey   -> "12345678"
        )
        when(journeyCacheService.cache(mockEq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))

        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.checkYourAnswers.url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val sut = createSUT

        val expectedCacheWithErasingNumber = Map(
          UpdatePreviousYearsIncomeConstants.TelephoneQuestionKey -> FormValuesConstants.NoValue,
          UpdatePreviousYearsIncomeConstants.TelephoneNumberKey   -> "")
        when(journeyCacheService.cache(mockEq(expectedCacheWithErasingNumber))(any()))
          .thenReturn(Future.successful(expectedCacheWithErasingNumber))

        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.NoValue,
              FormValuesConstants.YesNoTextEntry -> "this value must not be cached"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.checkYourAnswers.url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT
        val cache = Map(UpdatePreviousYearsIncomeConstants.TaxYearKey -> "2016")
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
      "there is a form validation error (additional, controller specific constraint)" in {
        val sut = createSUT
        val cache = Map(UpdatePreviousYearsIncomeConstants.TaxYearKey -> "2016")
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val tooFewCharsResult = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234"))
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = sut.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234123412341234123412341234123"))
        status(tooManyCharsResult) mustBe BAD_REQUEST
        val tooManyDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooManyDoc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
  }

  "checkYourAnswers" must {
    "display check your answers containing populated values from the journey cache" in {
      val SUT = createSUT
      when(journeyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
        Future.successful(Right(Seq[String]("2016", "whatYouToldUs", "Yes"), Seq[Option[String]](Some("123456789"))))
      )
      val result = SUT.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.checkYourAnswers.title"))
    }

    "redirect to the summary page if a value is missing from the cache " in {

      val SUT = createSUT

      when(
        journeyCacheService.collectedJourneyValues(
          any(classOf[scala.collection.immutable.List[String]]),
          any(classOf[scala.collection.immutable.List[String]]))(any())).thenReturn(
        Future.successful(Left("An error has occurred"))
      )

      val result = SUT.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad.url

    }

  }

  "submit your answers" must {
    "invoke the back end 'previous years income details' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {

        val sut = createSUT
        val incorrectIncome = IncorrectIncome("whatYouToldUs", "Yes", Some("123456789"))
        when(journeyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq[String]("1", "whatYouToldUs", "Yes"),
              Seq[Option[String]](Some("123456789"))
            ))
        )
        when(previousYearsIncomeService.incorrectIncome(any(), Matchers.eq(1), Matchers.eq(incorrectIncome))(any()))
          .thenReturn(Future.successful("1"))
        when(trackingjourneyCacheService
          .cache(Matchers.eq(TrackSuccessfulJourneyConstants.UpdatePreviousYearsIncomeKey), Matchers.eq("true"))(any()))
          .thenReturn(Future.successful(Map(TrackSuccessfulJourneyConstants.UpdatePreviousYearsIncomeKey -> "true")))
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.confirmation.url
        verify(journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has not been provided" in {

        val sut = createSUT
        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "No", None)
        when(journeyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq[String]("1", "whatYouToldUs", "No"),
              Seq[Option[String]](None)
            ))
        )
        when(previousYearsIncomeService.incorrectIncome(any(), Matchers.eq(1), Matchers.eq(incorrectEmployment))(any()))
          .thenReturn(Future.successful("1"))
        when(trackingjourneyCacheService
          .cache(Matchers.eq(TrackSuccessfulJourneyConstants.UpdatePreviousYearsIncomeKey), Matchers.eq("true"))(any()))
          .thenReturn(Future.successful(Map(TrackSuccessfulJourneyConstants.UpdatePreviousYearsIncomeKey -> "true")))
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.confirmation.url
        verify(journeyCacheService, times(1)).flush()(any())
      }
    }
  }

  "confirmation" must {
    "show the update income details confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT

        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.income.previousYears.confirmation.heading"))
      }
    }
  }

  private val previousTaxYear = TaxYear().prev

  private def createSUT = new SUT

  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val trackingjourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val previousYearsIncomeService: PreviousYearsIncomeService = mock[PreviousYearsIncomeService]

  private class SUT
      extends UpdateIncomeDetailsController(
        previousYearsIncomeService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        inject[CanWeContactByPhoneView],
        inject[CheckYourAnswersView],
        inject[UpdateIncomeDetailsDecisionView],
        inject[UpdateIncomeDetailsView],
        inject[UpdateIncomeDetailsConfirmationView],
        trackingjourneyCacheService,
        journeyCacheService,
        MockTemplateRenderer,
        inject[ErrorPagesHandler]
      )
}
