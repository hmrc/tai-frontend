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

import builders.RequestBuilder
import controllers.ErrorPagesHandler
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import pages.TrackSuccessfulJourneyUpdatePensionPage
import pages.updatePensionProvider._
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, _}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, PensionIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, IncorrectPensionDecisionConstants}
import utils.BaseSpec
import views.html.CanWeContactByPhoneView
import views.html.pensions.DuplicateSubmissionWarningView
import views.html.pensions.update._

import scala.concurrent.Future

class UpdatePensionProviderControllerSpec extends BaseSpec {

  private def createController = new UpdatePensionProviderTestController
  private def fakeGetRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
  private def fakePostRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

  val pensionName = "Pension 1"
  val pensionId = 1

  val pensionTaxCodeIncome: TaxCodeIncome =
    TaxCodeIncome(PensionIncome, Some(pensionId), 100, "", "", pensionName, Week1Month1BasisOfOperation, Live)
  val empTaxCodeIncome: TaxCodeIncome =
    TaxCodeIncome(EmploymentIncome, Some(2), 100, "", "", "", Week1Month1BasisOfOperation, Live)

  val pensionProviderService: PensionProviderService = mock[PensionProviderService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  val baseUserAnswers: UserAnswers = UserAnswers("testSessionId", nino.nino)

  class UpdatePensionProviderTestController
      extends UpdatePensionProviderController(
        taxAccountService,
        pensionProviderService,
        mockAuthJourney,
        mcc,
        appConfig,
        inject[CanWeContactByPhoneView],
        inject[DoYouGetThisPensionIncomeView],
        inject[WhatDoYouWantToTellUsView],
        inject[UpdatePensionCheckYourAnswersView],
        inject[ConfirmationView],
        inject[DuplicateSubmissionWarningView],
        mockJourneyCacheNewRepository,
        inject[ErrorPagesHandler]
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(baseUserAnswers)
    Mockito.reset(mockJourneyCacheNewRepository)
  }

  "doYouGetThisPension" must {
    "show the doYouGetThisPension view" in {

      val PensionQuestionKey = "yes"
      val userAnswers = baseUserAnswers
        .setOrException(UpdatePensionProviderIdPage, pensionId)
        .setOrException(UpdatePensionProviderNamePage, pensionName)
        .setOrException(UpdatePensionProviderReceivePensionPage, PensionQuestionKey)

      setup(userAnswers)

      val result = createController.doYouGetThisPension()(fakeGetRequest)

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.updatePension.decision.pagetitle"))
    }

    "redirect to the tax summary page if a value is missing from the cache " in {

      val result = createController.doYouGetThisPension()(fakeGetRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }

  }

  "handleDoYouGetThisPension" must {
    "return bad request" when {
      "no options are selected" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        val result = createController.handleDoYouGetThisPension()(
          fakePostRequest.withFormUrlEncodedBody(IncorrectPensionDecisionConstants.IncorrectPensionDecision -> "")
        )

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to tes-1 iform" when {
      "option NO is selected" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        val result = createController.handleDoYouGetThisPension()(
          fakePostRequest.withFormUrlEncodedBody(
            IncorrectPensionDecisionConstants.IncorrectPensionDecision -> FormValuesConstants.NoValue
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe appConfig.incomeFromEmploymentPensionLinkUrl
      }
    }

    "redirect to whatDoYouWantToTellUs" when {
      "option YES is selected" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

        val result = createController.handleDoYouGetThisPension()(
          fakePostRequest.withFormUrlEncodedBody(
            IncorrectPensionDecisionConstants.IncorrectPensionDecision -> FormValuesConstants.YesValue
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs().url
      }
    }

  }

  "whatDoYouWantToTellUs" must {
    "show the whatDoYouWantToTellUs page" when {
      "an authorised user calls the page" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        val result = createController.whatDoYouWantToTellUs()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.whatDoYouWantToTellUs.pagetitle"))
      }
      "we have pension details in the cache" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)
          .setOrException(UpdatePensionProviderDetailsPage, "some details")

        setup(userAnswers)

        val result = createController.whatDoYouWantToTellUs()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.whatDoYouWantToTellUs.pagetitle"))
      }

      "redirect to the tax summary page if a value is missing from the cache " in {

        val result = createController.whatDoYouWantToTellUs()(fakeGetRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }
  }
  "submitUpdateEmploymentDetails" must {

    "redirect to the addTelephoneNumber page" when {
      "the form submission is valid" in {

        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

        val result = createController.submitWhatDoYouWantToTellUs(
          fakePostRequest
            .withFormUrlEncodedBody(("pensionDetails", "test details"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {

        val pensionDetailsFormData = ("pensionDetails", "")
        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        val result = createController.submitWhatDoYouWantToTellUs(
          fakePostRequest
            .withFormUrlEncodedBody(pensionDetailsFormData)
        )

        status(result) mustBe BAD_REQUEST
      }
    }

  }

  "addTelephoneNumber" must {
    "show the contact by telephone page" when {
      "an authorised request is received" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)

        setup(userAnswers)

        val result = createController.addTelephoneNumber()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
      "an authorised request is received and we have cached data" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderPhoneQuestionPage, "yes")
          .setOrException(UpdatePensionProviderPhoneNumberPage, "123456789")

        setup(userAnswers)

        val result = createController.addTelephoneNumber()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.toString must include("123456789")
      }

      "redirect to the tax summary page if a value is missing from the cache " in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderPhoneQuestionPage, "yes")
          .setOrException(UpdatePensionProviderPhoneNumberPage, "123456789")

        setup(userAnswers)
        val result = createController.addTelephoneNumber()(fakeGetRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {

        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

        val result = createController.submitTelephoneNumber()(
          fakePostRequest.withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "12345678"
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers().url
      }

    }
    "the request has an authorised session, and telephone number contact has not been approved" in {

      when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

      val result = createController.submitTelephoneNumber()(
        fakePostRequest
          .withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.NoValue,
            FormValuesConstants.YesNoTextEntry -> "this value must not be cached"
          )
      )

      status(result) mustBe SEE_OTHER
      redirectLocation(
        result
      ).get mustBe controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers().url
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {

        val userAnswers = baseUserAnswers.setOrException(UpdatePensionProviderIdPage, pensionId)
        setup(userAnswers)

        val result = createController.submitTelephoneNumber()(
          fakePostRequest.withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> ""
          )
        )
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

      "there is a form validation error (additional, controller specific constraint)" in {
        val controller = createController

        val userAnswers = baseUserAnswers.setOrException(UpdatePensionProviderIdPage, pensionId)
        setup(userAnswers)

        val tooFewCharsResult = controller.submitTelephoneNumber()(
          fakePostRequest.withFormUrlEncodedBody(
            FormValuesConstants.YesNoChoice    -> FormValuesConstants.YesValue,
            FormValuesConstants.YesNoTextEntry -> "1234"
          )
        )
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = controller.submitTelephoneNumber()(
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

  "check your answers" must {
    "show summary page" when {
      "valid details are present in userAnswers" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)
          .setOrException(UpdatePensionProviderReceivePensionPage, "Yes")
          .setOrException(UpdatePensionProviderDetailsPage, "some random info")
          .setOrException(UpdatePensionProviderPhoneQuestionPage, "Yes")
          .setOrException(UpdatePensionProviderPhoneNumberPage, "123456789")

        setup(userAnswers)

        val result = createController.checkYourAnswers()(fakeGetRequest)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }

    "redirect to the summary page if a value is missing from the cache " in {

      val userAnswers = baseUserAnswers
        .setOrException(UpdatePensionProviderIdPage, pensionId)

      setup(userAnswers)

      val result = createController.checkYourAnswers()(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }
  }

  "submit your answers" must {
    "invoke the back end 'incorrectEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderDetailsPage, "some random info")
          .setOrException(UpdatePensionProviderPhoneQuestionPage, "Yes")
          .setOrException(UpdatePensionProviderPhoneNumberPage, "123456789")

        setup(userAnswers)

        when(pensionProviderService.incorrectPensionProvider(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful("envelope_id_1"))
        when(mockJourneyCacheNewRepository.clear(any(), any())).thenReturn(Future.successful(true))
        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

        val result = createController.submitYourAnswers()(fakePostRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController
          .confirmation()
          .url
        verify(mockJourneyCacheNewRepository, times(1)).clear(any(), any())
        verify(mockJourneyCacheNewRepository, times(1)).set(any())
      }

      "the request has an authorised session and telephone number has not been provided" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderDetailsPage, "some random info")
          .setOrException(UpdatePensionProviderPhoneQuestionPage, "No")

        setup(userAnswers)

        when(pensionProviderService.incorrectPensionProvider(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful("envelope_id_1"))
        when(mockJourneyCacheNewRepository.clear(any(), any())).thenReturn(Future.successful(true))
        when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

        val result = createController.submitYourAnswers()(fakePostRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController
          .confirmation()
          .url
        verify(mockJourneyCacheNewRepository, times(1)).set(any())
        verify(mockJourneyCacheNewRepository, times(1)).clear(any(), any())
      }
    }
  }

  "confirmation" must {
    "show the update pension confirmation page" when {
      "the request has an authorised session" in {

        val result = createController.confirmation()(fakeGetRequest)
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.confirmation.heading"))
      }
    }
  }

  "redirectUpdatePension" must {

    def taxAccountServiceCall =
      when(taxAccountService.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))

    "redirect to the Do You Get This Pension page when there is no update pension ID cache value present" in {

      taxAccountServiceCall

      when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

      val result = createController.UpdatePension(pensionId.toInt)(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.UpdatePensionProviderController.doYouGetThisPension().url
    }

    "redirect to the Duplicate Submission Warning page when there is an Update pension ID cache value present" in {

      taxAccountServiceCall

      val userAnswers = baseUserAnswers.setOrException(TrackSuccessfulJourneyUpdatePensionPage(pensionId), true)
      setup(userAnswers)

      when(mockJourneyCacheNewRepository.set(any())).thenReturn(Future.successful(true))

      val result = createController.UpdatePension(pensionId)(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.UpdatePensionProviderController.duplicateSubmissionWarning().url
    }

    "return Internal Server error" when {
      "tax code income sources are not available" in {

        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("Failed")))

        val result = createController.UpdatePension(pensionId)(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "an invalid id has been passed" in {

        val invalidPensionId = 4
        when(taxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))

        val result = createController.UpdatePension(invalidPensionId)(fakeGetRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "duplicateSubmissionWarning" must {
    "show duplicateSubmissionWarning view" in {

      val userAnswers = baseUserAnswers
        .setOrException(UpdatePensionProviderIdPage, pensionId)
        .setOrException(UpdatePensionProviderNamePage, pensionName)

      setup(userAnswers)

      val result = createController.duplicateSubmissionWarning(fakeGetRequest)
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() must include(Messages("tai.pension.warning.customGaTitle"))
    }

    "redirect to the summary page if a value is missing from the cache " in {

      val result = createController.duplicateSubmissionWarning(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

    }
  }

  "submitDuplicateSubmissionWarning" must {

    "redirect to the update remove employment decision page" when {
      "I want to update my employment is selected" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        val result = createController.submitDuplicateSubmissionWarning(
          fakePostRequest.withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension().url
      }
    }

    "redirect to the income source summary page" when {
      "I want to return to my employment details is selected" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        val result = createController.submitDuplicateSubmissionWarning(
          fakePostRequest.withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController
          .onPageLoad(pensionId)
          .url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {

        val userAnswers = baseUserAnswers
          .setOrException(UpdatePensionProviderIdPage, pensionId)
          .setOrException(UpdatePensionProviderNamePage, pensionName)

        setup(userAnswers)

        val result =
          createController.submitDuplicateSubmissionWarning(
            fakePostRequest.withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> "")
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "return Internal Server Error" when {
      "mandatory values are missing from userAnswers" in {

        val result = createController.submitDuplicateSubmissionWarning(
          fakePostRequest.withFormUrlEncodedBody(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
        )

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
