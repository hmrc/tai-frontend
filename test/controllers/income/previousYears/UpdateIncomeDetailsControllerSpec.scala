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

package controllers.income.previousYears

import builders.RequestBuilder
import controllers.ErrorPagesHandler
import controllers.auth.{AuthedUser, DataRequest}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.stubbing.ScalaOngoingStubbing
import pages.TrackSuccessfulJourneyConstantsUpdatePreviousYearPage
import pages.income._
import play.api.i18n.Messages
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result}
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, UpdateHistoricIncomeChoiceConstants}
import utils.BaseSpec
import views.html.CanWeContactByPhoneView
import views.html.incomes.previousYears._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class UpdateIncomeDetailsControllerSpec extends BaseSpec {

  private val previousTaxYear = TaxYear().prev
  val sessionId = "testSessionId"

  private def createSUT = new SUT
  def randomNino(): Nino = new Generator(new Random()).nextNino

  val previousYearsIncomeService: PreviousYearsIncomeService = mock[PreviousYearsIncomeService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  private class SUT
      extends UpdateIncomeDetailsController(
        previousYearsIncomeService,
        mockAuthJourney,
        mcc,
        inject[CanWeContactByPhoneView],
        inject[CheckYourAnswersView],
        inject[UpdateIncomeDetailsDecisionView],
        inject[UpdateIncomeDetailsView],
        inject[UpdateIncomeDetailsConfirmationView],
        mockJourneyCacheNewRepository,
        inject[ErrorPagesHandler]
      ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  private def setup(ua: UserAnswers): ScalaOngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
    when(mockAuthJourney.authWithDataRetrieval) thenReturn new ActionBuilder[DataRequest, AnyContent] {
      override def invokeBlock[A](
        request: Request[A],
        block: DataRequest[A] => Future[Result]
      ): Future[Result] =
        block(
          DataRequest(
            request,
            taiUser = AuthedUser(
              Nino(nino.toString()),
              Some("saUtr"),
              None
            ),
            fullName = "",
            userAnswers = ua
          )
        )

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheNewRepository)
  }

  "decision" must {
    "return ok" in {
      val SUT = createSUT

      when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      val result = SUT.decision(previousTaxYear)(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(
        Messages(
          "tai.income.previousYears.decision.title",
          TaxPeriodLabelService.taxPeriodLabel(previousTaxYear.year).replaceAll("\u00A0", " ")
        )
      )
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
              UpdateHistoricIncomeChoiceConstants.UpdateIncomeChoice -> FormValuesConstants.YesValue
            )
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
              UpdateHistoricIncomeChoiceConstants.UpdateIncomeChoice -> FormValuesConstants.NoValue
            )
        val result = SUT.submitDecision()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.previousYears.routes.UpdateIncomeDetailsController.details().url
        )
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {
        val sut = createSUT
        val result = sut.submitDecision()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(UpdateHistoricIncomeChoiceConstants.UpdateIncomeChoice -> "")
        )
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "details" must {
    "show 'What Do You Want To Tell Us' Page" when {
      "the request has an authorised session with Tax Year" in {
        val taxYear = TaxYear().prev.year.toString

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomePage, "123")
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, taxYear)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

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
        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val SUT = createSUT

        val result = SUT.submitDetails()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("employmentDetails", "test details"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber().url
      }
    }

    "add income details to the journey cache" when {
      "the form submission is valid" in {
        val SUT = createSUT

        val incomeDetailsFormData = ("employmentDetails", "test details")

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.submitDetails()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(incomeDetailsFormData)
        )

        status(result) mustBe SEE_OTHER
        verify(mockJourneyCacheNewRepository).set(any())
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomePage, "123")
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2016")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val employmentDetailsFormData = ("employmentDetails", "")

        val result = SUT.submitDetails()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(employmentDetailsFormData)
        )

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "telephoneNumber" must {
    "show the contact by telephone page" when {
      "valid details have been passed" in {
        val taxYear = TaxYear().prev.year.toString

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, taxYear)
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "12345678")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
  }

  "submitTelephoneNumber" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "12345678")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.submitTelephoneNumber()(
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
        ).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.checkYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.NoValue)
          .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "$$$$")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.submitTelephoneNumber()(
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
        ).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController.checkYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2016")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.submitTelephoneNumber()(
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

      "there is a form validation error (too few characters)" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "1234")
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2016")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val tooFewCharsResult = SUT.submitTelephoneNumber()(
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
      }

      "there is a form validation error (too many characters)" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "1234123412341234123412341234123")
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2016")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val tooManyCharsResult = SUT.submitTelephoneNumber()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
              FormValuesConstants.YesNoTextEntry -> "1234123412341234123412341234123"
            )
        )
        status(tooManyCharsResult) mustBe BAD_REQUEST
        val tooManyDoc = Jsoup.parse(contentAsString(tooManyCharsResult))
        tooManyDoc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

    }
  }

  "checkYourAnswers" must {
    "display check your answers containing populated values from the journey cache" in {
      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
        .setOrException(UpdatePreviousYearsIncomePage, "whatYouToldUs")
        .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "123456789")
        .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2016")

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result = SUT.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.checkYourAnswers.title"))
    }

    "redirect to the summary page if a value is missing from the cache " in {
      val SUT = createSUT

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(None))

      val result = SUT.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }

  }

  "submit your answers" must {
    "invoke the back end 'previous years income details' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2020")
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(UpdatePreviousYearsIncomePage, "whatYouToldUs")
          .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "123456789")
          .setOrException(TrackSuccessfulJourneyConstantsUpdatePreviousYearPage, "true")

        val incorrectIncome = IncorrectIncome("whatYouToldUs", "Yes", Some("123456789"))

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(mockJourneyCacheNewRepository.clear(any(), any()))
          .thenReturn(Future.successful(true))

        when(previousYearsIncomeService.incorrectIncome(any(), meq(2020), meq(incorrectIncome))(any(), any()))
          .thenReturn(Future.successful("1"))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController
          .confirmation()
          .url
        verify(mockJourneyCacheNewRepository).set(any())
      }

      "the request has an authorised session and telephone number has not been provided" in {
        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.NoValue)
          .setOrException(UpdatePreviousYearsIncomePage, "whatYouToldUs")
          .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2020")
          .setOrException(TrackSuccessfulJourneyConstantsUpdatePreviousYearPage, "true")

        val incorrectEmployment = IncorrectIncome("whatYouToldUs", "No", None)

        val sut = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        when(previousYearsIncomeService.incorrectIncome(any(), meq(2020), meq(incorrectEmployment))(any(), any()))
          .thenReturn(Future.successful("1"))

        val result = sut.submitYourAnswers()(
          RequestBuilder.buildFakeRequestWithAuth("POST")
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.income.previousYears.routes.UpdateIncomeDetailsController
          .confirmation()
          .url
        verify(mockJourneyCacheNewRepository).set(any())
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

}
