/*
 * Copyright 2019 HM Revenue & Customs
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

import builders.{RequestBuilder}
import controllers.actions.FakeValidatePerson
import controllers.{FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, IncorrectPensionProvider, PensionIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, IncorrectPensionDecisionConstants, JourneyCacheConstants}

import scala.concurrent.Future

class UpdatePensionProviderControllerSpec extends PlaySpec with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JourneyCacheConstants
  with FormValuesConstants
  with IncorrectPensionDecisionConstants
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(journeyCacheService)
  }

  val pensionName = "Pension 1"
  val pensionId = "1"

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  "doYouGetThisPension" must {
    "show the doYouGetThisPension view" in {

      val PensionQuestionKey = "yes"

      when(journeyCacheService.collectedValues(Seq(Matchers.anyVararg[String]), Seq(Matchers.anyVararg[String]))(any()))
        .thenReturn(Future.successful(Seq(pensionId.toString, pensionName), Seq(Some(PensionQuestionKey))))

      val result = createController.doYouGetThisPension()(fakeGetRequest)

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.updatePension.decision.pagetitle"))
    }
  }

  "handleDoYouGetThisPension" must {
    "return bad request" when {
      "no options are selected" in {

        when(journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq(pensionId, pensionName)))

        val result = createController.handleDoYouGetThisPension()(fakePostRequest.
          withFormUrlEncodedBody(IncorrectPensionDecision -> ""))

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to tes-1 iform" when {
      "option NO is selected" in {

        when(journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq(pensionId, pensionName)))

        val result = createController.handleDoYouGetThisPension()(fakePostRequest.
          withFormUrlEncodedBody(IncorrectPensionDecision -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe ApplicationConfig.incomeFromEmploymentPensionLinkUrl
      }
    }

    "redirect to whatDoYouWantToTellUs" when {
      "option YES is selected" in {

        when(journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq(pensionId, pensionName)))

        when(journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))

        val result = createController.handleDoYouGetThisPension()(fakePostRequest.
          withFormUrlEncodedBody(IncorrectPensionDecision -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs().url
      }
    }

  }

  "whatDoYouWantToTellUs" must {
    "show the whatDoYouWantToTellUs page" when {
      "an authorised user calls the page" in {

        when(journeyCacheService.collectedValues(Matchers.eq(Seq(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey)),
          Matchers.eq(Seq(UpdatePensionProvider_DetailsKey)))(any())).thenReturn(Future.successful(Seq(pensionName, pensionId), Seq(None)))

        val result = createController.whatDoYouWantToTellUs()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.whatDoYouWantToTellUs.pagetitle"))
      }
      "we have pension details in the cache" in {

        val cache = Seq(pensionName, pensionId)
        val optionalCache = Seq(Some("test1"))
        when(journeyCacheService.collectedValues(Matchers.eq(Seq(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey)),
          Matchers.eq(Seq(UpdatePensionProvider_DetailsKey)))(any())).thenReturn(Future.successful(cache, optionalCache))

        val result = createController.whatDoYouWantToTellUs()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.whatDoYouWantToTellUs.pagetitle"))
      }
    }
  }
  "submitUpdateEmploymentDetails" must {

    "redirect to the addTelephoneNumber page" when {
      "the form submission is valid" in {

        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = createController.submitWhatDoYouWantToTellUs(fakePostRequest
          .withFormUrlEncodedBody(("pensionDetails", "test details")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {

        val pensionDetailsFormData = ("pensionDetails", "")

        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String]())(any()))
          .thenReturn(Future.successful(Seq(pensionName, pensionId)))


        val result = createController.submitWhatDoYouWantToTellUs(fakePostRequest
          .withFormUrlEncodedBody(pensionDetailsFormData))

        status(result) mustBe BAD_REQUEST
      }
    }

  }

  "addTelephoneNumber" must {
    "show the contact by telephone page" when {
      "an authorised request is received" in {

        when(journeyCacheService.mandatoryValueAsInt(any())(any())).
          thenReturn(Future.successful(pensionId.toInt))
        when(journeyCacheService.optionalValues(any())(any()))
          .thenReturn(Future.successful(Seq(None, None)))
        val result = createController.addTelephoneNumber()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
      "an authorised request is received and we have cached data" in {

        when(journeyCacheService.mandatoryValueAsInt(any())(any())).
          thenReturn(Future.successful(pensionId.toInt))
        when(journeyCacheService.optionalValues(any())(any()))
          .thenReturn(Future.successful(Seq(Some("yes"), Some("123456789"))))
        val result = createController.addTelephoneNumber()(fakeGetRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.toString must include("123456789")
      }
    }
  }

  "submit telephone number" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {

        val expectedCache = Map(UpdatePensionProvider_TelephoneQuestionKey -> YesValue, UpdatePensionProvider_TelephoneNumberKey -> "12345678")
        when(journeyCacheService.cache(mockEq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))

        val result = createController.submitTelephoneNumber()(fakePostRequest.withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers().url
      }

    }
    "the request has an authorised session, and telephone number contact has not been approved" in {

      val expectedCacheWithErasingNumber = Map(UpdatePensionProvider_TelephoneQuestionKey -> NoValue, UpdatePensionProvider_TelephoneNumberKey -> "")
      when(journeyCacheService.cache(mockEq(expectedCacheWithErasingNumber))(any())).thenReturn(Future.successful(expectedCacheWithErasingNumber))

      val result = createController.submitTelephoneNumber()(fakePostRequest.withFormUrlEncodedBody(
        YesNoChoice -> NoValue, YesNoTextEntry -> "this value must not be cached"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers().url
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {

        val cache = Map(UpdatePensionProvider_IdKey -> pensionId)
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = createController.submitTelephoneNumber()(fakePostRequest.withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

      "there is a form validation error (additional, controller specific constraint)" in {
        val controller = createController

        val cache = Map(UpdatePensionProvider_IdKey -> pensionId)
        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val tooFewCharsResult = controller.submitTelephoneNumber()(fakePostRequest.withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234"))
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = controller.submitTelephoneNumber()(fakePostRequest.withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234123412341234123412341234123"))
        status(tooManyCharsResult) mustBe BAD_REQUEST
        val tooManyDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooManyDoc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
    }
  }

  "check your answers" must {
    "show summary page" when {
      "valid details are present in journey cache" in {

        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](pensionId, pensionName, "Yes", "some random info", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )

        val result = createController.checkYourAnswers()(fakeGetRequest)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }
  }

  "submit your answers" must {
    "invoke the back end 'incorrectEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {

        val incorrectPensionProvider = IncorrectPensionProvider("some random info", "Yes", Some("123456789"))
        val empId = 1
        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](empId.toString, "some random info", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )
        when(pensionProviderService.incorrectPensionProvider(any(), Matchers.eq(1), Matchers.eq(incorrectPensionProvider))(any()))
          .thenReturn(Future.successful("envelope_id_1"))
        when(successfulJourneyCacheService.cache(Matchers.eq(s"$TrackSuccessfulJourney_UpdatePensionKey-${empId}"), Matchers.eq("true"))(any()))
          .thenReturn(Future.successful(Map(s"$TrackSuccessfulJourney_UpdatePensionKey-$empId" -> "true")))
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = createController.submitYourAnswers()(fakePostRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.confirmation().url
        verify(journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has not been provided" in {

        val incorrectPensionProvider = IncorrectPensionProvider("some random info", "No", None)
        val empId = 1
        when(journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String](empId.toString, "some random info", "No"),
            Seq[Option[String]](None)
          ))
        )
        when(pensionProviderService.incorrectPensionProvider(any(), Matchers.eq(1), Matchers.eq(incorrectPensionProvider))(any()))
          .thenReturn(Future.successful("envelope_id_1"))
        when(successfulJourneyCacheService.cache(Matchers.eq(s"$TrackSuccessfulJourney_UpdatePensionKey-${empId}"), Matchers.eq("true"))(any()))
          .thenReturn(Future.successful(Map(s"$TrackSuccessfulJourney_UpdatePensionKey-${empId}" -> "true")))
        when(journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = createController.submitYourAnswers()(fakePostRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.confirmation().url
        verify(journeyCacheService, times(1)).flush()(any())
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

    def cacheMap = Map(UpdatePensionProvider_IdKey -> pensionId.toString, UpdatePensionProvider_NameKey -> pensionName)

    def taxAccountServiceCall = when(taxAccountService.taxCodeIncomes(any(), any())(any())).
      thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))

    def journeyCacheCall = when(journeyCacheService.cache(Matchers.eq(cacheMap))(any())).thenReturn(Future.successful(cacheMap))

    "redirect to the Do You Get This Pension page when there is no update pension ID cache value present" in {

      taxAccountServiceCall
      when(successfulJourneyCacheService.currentValue(Matchers.eq(s"$TrackSuccessfulJourney_UpdatePensionKey-$pensionId"))(any())).
      thenReturn(Future.successful(None))
      journeyCacheCall

      val result = createController.UpdatePension(pensionId.toInt)(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.UpdatePensionProviderController.doYouGetThisPension().url

    }

    "redirect to the Duplicate Submission Warning page when there is an Update pension ID cache value present" in {

      taxAccountServiceCall
      when(successfulJourneyCacheService.currentValue(Matchers.eq(s"$TrackSuccessfulJourney_UpdatePensionKey-$pensionId"))(any())).
      thenReturn(Future.successful(Some("true")))
      journeyCacheCall

      val result = createController.UpdatePension(pensionId.toInt)(fakeGetRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.UpdatePensionProviderController.duplicateSubmissionWarning.url
    }

    "return Internal Server error" when {
      "tax code income sources are not available" in {

        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))

        val result = createController.UpdatePension(pensionId.toInt)(fakeGetRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "an invalid id has been passed" in {

        when(taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))

        val result = createController.UpdatePension(4)(fakeGetRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "duplicateSubmissionWarning" must {
    "show duplicateSubmissionWarning view" in {

      when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
        .thenReturn(Future.successful(Seq(pensionName, pensionId.toString)))

      val result = createController.duplicateSubmissionWarning(fakeGetRequest)
      val doc = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK
      doc.title() must include(Messages("tai.pension.warning.customGaTitle"))
    }
  }

  "submitDuplicateSubmissionWarning" must {

    def journeyCacheCall = when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
      .thenReturn(Future.successful(Seq(pensionName, pensionId.toString)))

    "redirect to the update remove employment decision page" when {
      "I want to update my employment is selected" in {

        journeyCacheCall

        val result = createController.submitDuplicateSubmissionWarning(fakePostRequest.withFormUrlEncodedBody(YesNoChoice -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension().url
      }
    }

    "redirect to the income source summary page" when {
      "I want to return to my employment details is selected" in {

        journeyCacheCall

        val result = createController.submitDuplicateSubmissionWarning(fakePostRequest.withFormUrlEncodedBody(YesNoChoice -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.IncomeSourceSummaryController.onPageLoad(pensionId.toInt).url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {

        when(journeyCacheService.mandatoryValues(Matchers.anyVararg[String])(any()))
          .thenReturn(Future.successful(Seq(pensionName, pensionId.toString)))

        val result = createController.submitDuplicateSubmissionWarning(fakePostRequest.withFormUrlEncodedBody(YesNoChoice -> ""))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createController = new UpdatePensionProviderTestController

  private def fakeGetRequest = RequestBuilder.buildFakeRequestWithAuth("GET")
  private def fakePostRequest = RequestBuilder.buildFakeRequestWithAuth("POST")

  val generateNino: Nino = new Generator().nextNino
  val pensionTaxCodeIncome = TaxCodeIncome(PensionIncome, Some(pensionId.toInt), 100, "", "", pensionName, Week1Month1BasisOfOperation, Live)
  val empTaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(2), 100, "", "", "", Week1Month1BasisOfOperation, Live)


  val pensionProviderService = mock[PensionProviderService]
  val taxAccountService = mock[TaxAccountService]
  val journeyCacheService = mock[JourneyCacheService]
  val successfulJourneyCacheService = mock[JourneyCacheService]

  class UpdatePensionProviderTestController extends UpdatePensionProviderController(
    taxAccountService,
    pensionProviderService,
    mock[AuditService],
    FakeAuthAction,
    FakeValidatePerson,
    journeyCacheService,
    successfulJourneyCacheService,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  )
}
