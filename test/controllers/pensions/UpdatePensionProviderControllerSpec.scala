/*
 * Copyright 2018 HM Revenue & Customs
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

import builders.{AuthBuilder, RequestBuilder}
import controllers.FakeTaiPlayApplication
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, IncorrectPensionProvider, PensionIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, IncorrectPensionDecisionConstants, JourneyCacheConstants}

import scala.concurrent.Future

class UpdatePensionProviderControllerSpec extends PlaySpec with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport
  with JourneyCacheConstants
  with FormValuesConstants
  with IncorrectPensionDecisionConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  "decision" must {
    "show the decision view" when {
      "a valid pension id has been passed" in {
        val sut = createSUT
        val pensionTaxCodeIncome = TaxCodeIncome(PensionIncome, Some(1), 100, "", "",
          "TEST", Week1Month1BasisOfOperation, Live)
        val empTaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(2), 100, "", "",
          "", Week1Month1BasisOfOperation, Live)
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.doYouGetThisPension(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.decision.heading", "TEST"))
      }
      "a valid pension id has been passed and we have some cached data" in {
        val sut = createSUT
        val pensionTaxCodeIncome = TaxCodeIncome(PensionIncome, Some(1), 100, "", "",
          "TEST", Week1Month1BasisOfOperation, Live)
        val empTaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(2), 100, "", "",
          "", Week1Month1BasisOfOperation, Live)
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.doYouGetThisPension(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.decision.heading", "TEST"))
      }
    }


    "return Internal Server error" when {
      "tax code income sources are not available" in {
        val sut = createSUT
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))

        val result = sut.doYouGetThisPension(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "an invalid id has been passed" in {
        val sut = createSUT
        val pensionTaxCodeIncome = TaxCodeIncome(PensionIncome, Some(1), 100, "", "",
          "", Week1Month1BasisOfOperation, Live)
        val empTaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(2), 100, "", "",
          "", Week1Month1BasisOfOperation, Live)
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))

        val result = sut.doYouGetThisPension(4)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }


      "an invalid pension id has been passed" in {
        val sut = createSUT
        val pensionTaxCodeIncome = TaxCodeIncome(PensionIncome, Some(1), 100, "", "",
          "", Week1Month1BasisOfOperation, Live)
        val empTaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(2), 100, "", "",
          "", Week1Month1BasisOfOperation, Live)
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(Seq(pensionTaxCodeIncome, empTaxCodeIncome))))

        val result = sut.doYouGetThisPension(2)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "handle decision" must {
    "return bad request" when {
      "no options are selected" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "TEST")))

        val result = sut.handleDoYouGetThisPension()(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(IncorrectPensionDecision -> ""))

        status(result) mustBe BAD_REQUEST
      }
    }

    "redirect to tes-1 iform" when {
      "option NO is selected" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "TEST")))

        val result = sut.handleDoYouGetThisPension()(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(IncorrectPensionDecision -> NoValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe ApplicationConfig.incomeFromEmploymentPensionLinkUrl
      }
    }

    "redirect to whatDoYouWantToTellUs" when {
      "option YES is selected" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValues(any())(any())).
          thenReturn(Future.successful(Seq("1", "TEST")))
        when(sut.journeyCacheService.cache(any(), any())(any())).
          thenReturn(Future.successful(Map.empty[String, String]))

        val result = sut.handleDoYouGetThisPension()(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(IncorrectPensionDecision -> YesValue))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs().url
      }
    }

  }

  "whatDoYouWantToTellUs" must {
    "show the whatDoYouWantToTellUs page" when {
      "an authorised user calls the page" in {
        val sut = createSUT
        val cache = Seq("TEST")
        val optionalCache = Seq(None)
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(cache, optionalCache))


        val result = sut.whatDoYouWantToTellUs()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.whatDoYouWantToTellUs.heading", "TEST"))
      }
      "we have pension details in the cache" in {
        val sut = createSUT
        val cache = Seq("TEST")
        val optionalCache = Seq(Some("test1"))
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).
          thenReturn(Future.successful(cache, optionalCache))

        val result = sut.whatDoYouWantToTellUs()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.whatDoYouWantToTellUs.heading", "TEST"))
      }
    }
  }
  "submitUpdateEmploymentDetails" must {

    "redirect to the addTelephoneNumber page" when {
      "the form submission is valid" in {

        val sut = createSUT

        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitWhatDoYouWantToTellUs(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(("pensionDetails", "test details")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {

        val sut = createSUT

        val pensionDetailsFormData = ("pensionDetails", "")

        when(sut.journeyCacheService.mandatoryValue(any())(any())).thenReturn(Future.successful("Test"))
        when(sut.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = sut.submitWhatDoYouWantToTellUs(RequestBuilder.buildFakeRequestWithAuth("POST")
          .withFormUrlEncodedBody(pensionDetailsFormData))

        status(result) mustBe BAD_REQUEST
      }
    }

  }

  "addTelephoneNumber" must {
    "show the contact by telephone page" when {
      "an authorised request is received" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValueAsInt(any())(any())).
          thenReturn(Future.successful(1))
        when(sut.journeyCacheService.optionalValues(any())(any()))
          .thenReturn(Future.successful(Seq(None, None)))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }
      "an authorised request is received and we have cached data" in {
        val sut = createSUT
        when(sut.journeyCacheService.mandatoryValueAsInt(any())(any())).
          thenReturn(Future.successful(1))
        when(sut.journeyCacheService.optionalValues(any())(any()))
          .thenReturn(Future.successful(Seq(Some("yes"), Some("123456789"))))
        val result = sut.addTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

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
        val sut = createSUT
        val expectedCache = Map(UpdatePensionProvider_TelephoneQuestionKey -> YesValue, UpdatePensionProvider_TelephoneNumberKey -> "12345678")
        when(sut.journeyCacheService.cache(mockEq(expectedCache))(any())).thenReturn(Future.successful(expectedCache))

        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "12345678"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers().url
      }

    }
    "the request has an authorised session, and telephone number contact has not been approved" in {
      val sut = createSUT

      val expectedCacheWithErasingNumber = Map(UpdatePensionProvider_TelephoneQuestionKey -> NoValue, UpdatePensionProvider_TelephoneNumberKey -> "")
      when(sut.journeyCacheService.cache(mockEq(expectedCacheWithErasingNumber))(any())).thenReturn(Future.successful(expectedCacheWithErasingNumber))

      val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
        YesNoChoice -> NoValue, YesNoTextEntry -> "this value must not be cached"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers().url
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val sut = createSUT
        val cache = Map(UpdatePensionProvider_IdKey -> "1")
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
      }

      "there is a form validation error (additional, controller specific constraint)" in {
        val sut = createSUT
        val cache = Map(UpdatePensionProvider_IdKey -> "1")
        when(sut.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val tooFewCharsResult = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
          YesNoChoice -> YesValue, YesNoTextEntry -> "1234"))
        status(tooFewCharsResult) mustBe BAD_REQUEST
        val tooFewDoc = Jsoup.parse(contentAsString(tooFewCharsResult))
        tooFewDoc.title() must include(Messages("tai.canWeContactByPhone.title"))

        val tooManyCharsResult = sut.submitTelephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(
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
        val sut = createSUT
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("1", "Pension1", "Yes", "some random info", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )

        val result = sut.checkYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.checkYourAnswers.title"))
      }
    }
  }

  "submit your answers" must {
    "invoke the back end 'incorrectEmployment' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number has been provided" in {
        val sut = createSUT
        val incorrectPensionProvider = IncorrectPensionProvider("some random info", "Yes", Some("123456789"))
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("1", "some random info", "Yes"),
            Seq[Option[String]](Some("123456789"))
          ))
        )
        when(pensionProviderService.incorrectPensionProvider(any(), Matchers.eq(1), Matchers.eq(incorrectPensionProvider))(any()))
          .thenReturn(Future.successful("envelope_id_1"))
        when(sut.successfulJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_UpdatePensionKey), Matchers.eq("true"))(any()))
          .thenReturn(Future.successful(Map(TrackSuccessfulJourney_UpdateEmploymentKey -> "true")))
        when(sut.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.confirmation().url
        verify(sut.journeyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has not been provided" in {
        val sut = createSUT
        val incorrectPensionProvider = IncorrectPensionProvider("some random info", "No", None)
        when(sut.journeyCacheService.collectedValues(any(), any())(any())).thenReturn(
          Future.successful((
            Seq[String]("1", "some random info", "No"),
            Seq[Option[String]](None)
          ))
        )
        when(pensionProviderService.incorrectPensionProvider(any(), Matchers.eq(1), Matchers.eq(incorrectPensionProvider))(any()))
          .thenReturn(Future.successful("envelope_id_1"))
        when(sut.successfulJourneyCacheService.cache(Matchers.eq(TrackSuccessfulJourney_UpdatePensionKey), Matchers.eq("true"))(any()))
          .thenReturn(Future.successful(Map(TrackSuccessfulJourney_UpdateEmploymentKey -> "true")))
        when(sut.journeyCacheService.flush()(any())).thenReturn(Future.successful(TaiSuccessResponse))

        val result = sut.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.pensions.routes.UpdatePensionProviderController.confirmation().url
        verify(sut.journeyCacheService, times(1)).flush()(any())
      }
    }
  }

  "confirmation" must {
    "show the update pension confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT
        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.updatePension.confirmation.heading"))
      }
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createSUT = new SUT

  val generateNino: Nino = new Generator().nextNino
  val pensionName = "TEST"

  val pensionProviderService = mock[PensionProviderService]

  class SUT extends UpdatePensionProviderController(
    mock[TaxAccountService],
    pensionProviderService,
    mock[AuditService],
    mock[PersonService],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[JourneyCacheService],
    mock[JourneyCacheService],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))
  }

}
