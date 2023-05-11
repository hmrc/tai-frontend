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

package controllers.benefits

import akka.Done
import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{ControllerViewTestHelper, FakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.forms.benefits.{CompanyBenefitTotalValueForm, RemoveCompanyBenefitStopDateForm}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.benefits.EndedCompanyBenefit
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.service.{ThreeWeeks, TrackingService}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.TaiConstants.TaxDateWordMonthFormat
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.viewModels.benefit.{BenefitViewModel, RemoveCompanyBenefitsCheckYourAnswersViewModel}
import utils.BaseSpec
import views.html.CanWeContactByPhoneView
import views.html.benefits.{RemoveBenefitTotalValueView, RemoveCompanyBenefitCheckYourAnswersView, RemoveCompanyBenefitConfirmationView, RemoveCompanyBenefitStopDateView}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class RemoveCompanyBenefitControllerSpec
    extends BaseSpec with JsoupMatchers with BeforeAndAfterEach with ControllerViewTestHelper {

  override def beforeEach: Unit =
    Mockito.reset(removeCompanyBenefitJourneyCacheService)

  "stopDate" must {
    "show 'When did you stop getting benefits from company?' page" in {
      val SUT = createSUT
      val cache = Map(
        EndCompanyBenefitConstants.EmploymentNameKey -> "Test",
        EndCompanyBenefitConstants.BenefitNameKey    -> "Test",
        EndCompanyBenefitConstants.RefererKey        -> "Test"
      )

      when(removeCompanyBenefitJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

      val result = SUT.stopDate()(fakeRequest)
      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.benefits.ended.stopDate.heading", "Test", "Test"))

      verify(removeCompanyBenefitJourneyCacheService, times(1)).currentCache(any())
    }

    "show the prepopulated fields when an EndCompanyBenefitConstants.BenefitStopDateKey has been cached" in {
      val stopDate = LocalDate.now()
      val SUT = createSUT
      val cache = Map(
        EndCompanyBenefitConstants.EmploymentNameKey  -> "EmploymentName",
        EndCompanyBenefitConstants.BenefitNameKey     -> "BenefitName",
        EndCompanyBenefitConstants.RefererKey         -> "Referer",
        EndCompanyBenefitConstants.BenefitStopDateKey -> stopDate.toString
      )

      when(removeCompanyBenefitJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")
      val result = SUT.stopDate()(request)

      val expectedView = {
        val form = RemoveCompanyBenefitStopDateForm("BenefitName", "EmploymentName").form
          .fill(LocalDate.parse(cache(EndCompanyBenefitConstants.BenefitStopDateKey)))
        removeCompanyBenefitStopDateView(
          form,
          cache(EndCompanyBenefitConstants.BenefitNameKey),
          cache(EndCompanyBenefitConstants.EmploymentNameKey)
        )
      }

      result rendersTheSameViewAs expectedView
    }
  }

  "submit stop date" must {
    "redirect to the 'Can we call you if we need more information?' page" when {
      "the date is before this tax year" in {

        val SUT = createSUT
        val year = TaxYear().year.toString
        val formData = Json.obj(
          RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "01",
          RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "01",
          RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> year
        )

        when(removeCompanyBenefitJourneyCacheService.currentCache(any()))
          .thenReturn(
            Future.successful(
              Map(
                EndCompanyBenefitConstants.BenefitNameKey    -> "benefit",
                EndCompanyBenefitConstants.EmploymentNameKey -> "employment"
              )
            )
          )
        when(removeCompanyBenefitJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))
        when(removeCompanyBenefitJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitStopDate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withJsonBody(formData)
        )
        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber.url

        verify(removeCompanyBenefitJourneyCacheService, times(1))
          .cache(
            eq(
              Map(
                EndCompanyBenefitConstants.BenefitNameKey     -> "benefit",
                EndCompanyBenefitConstants.EmploymentNameKey  -> "employment",
                EndCompanyBenefitConstants.BenefitStopDateKey -> s"$year-01-01"
              )
            )
          )(any())
      }
    }

    "redirect to the 'What was the total value of your benefit' page" when {
      "the date is during this tax year" in {

        val taxYear = TaxYear()
        val year = taxYear.year.toString
        val formData = Json.obj(
          RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "06",
          RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "04",
          RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> year
        )

        val SUT = createSUT
        when(removeCompanyBenefitJourneyCacheService.currentCache(any()))
          .thenReturn(
            Future.successful(
              Map(
                EndCompanyBenefitConstants.BenefitNameKey    -> "benefit",
                EndCompanyBenefitConstants.EmploymentNameKey -> "employment"
              )
            )
          )
        when(removeCompanyBenefitJourneyCacheService.cache(any(), any())(any()))
          .thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitStopDate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withJsonBody(formData)
        )
        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit.url

        verify(removeCompanyBenefitJourneyCacheService, times(1))
          .cache(eq(EndCompanyBenefitConstants.BenefitStopDateKey), eq(s"$year-04-06"))(any())
      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val formData = Json.obj(
          RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "",
          RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "",
          RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> ""
        )

        val SUT = createSUT
        when(removeCompanyBenefitJourneyCacheService.currentCache(any()))
          .thenReturn(
            Future.successful(
              Map(
                EndCompanyBenefitConstants.BenefitNameKey    -> "benefit",
                EndCompanyBenefitConstants.EmploymentNameKey -> "employment"
              )
            )
          )
        when(removeCompanyBenefitJourneyCacheService.mandatoryJourneyValues(any())(any()))
          .thenReturn(Future.successful(Right(Seq("EmployerA", "Expenses", "Url"))))
        val result = SUT.submitStopDate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withJsonBody(formData)
        )

        status(result) mustBe BAD_REQUEST

        verify(removeCompanyBenefitJourneyCacheService, times(1)).mandatoryJourneyValues(Matchers.anyVararg())(any())
      }
    }
  }

  "totalValueOfBenefit" must {

    val employmentName = "Employment Name"
    val benefitName = "Employer Provided Services"
    val referer = "Url"

    "show what was the total value page" when {
      "the request has an authorised session with employment name and benefit name" in {
        val SUT = createSUT

        when(removeCompanyBenefitJourneyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq(employmentName, benefitName, referer),
              Seq[Option[String]](None)
            )
          )
        )

        val result = SUT.totalValueOfBenefit()(fakeRequest)
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(
          Messages("tai.remove.company.benefit.total.value.heading", benefitName, employmentName)
        )
      }
    }

    "the value of benefit is prepopulated with the cached amount" in {
      val SUT = createSUT

      val valueOfBenefit = Some("9876543")

      when(removeCompanyBenefitJourneyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
        Future.successful(
          Right(
            Seq(employmentName, benefitName, referer),
            Seq[Option[String]](valueOfBenefit)
          )
        )
      )

      implicit val request: FakeRequest[AnyContent] = fakeRequest

      val result = SUT.totalValueOfBenefit()(fakeRequest)

      val expectedForm = CompanyBenefitTotalValueForm.form.fill(valueOfBenefit.get)
      val expectedViewModel = BenefitViewModel(employmentName, benefitName)
      result rendersTheSameViewAs removeBenefitTotalValueView(expectedViewModel, expectedForm)
    }
  }

  "submitBenefitValue" must {
    "redirect to the 'Can we contact you' page" when {
      "the form submission is valid" in {
        val SUT = createSUT
        when(removeCompanyBenefitJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("totalValue", "1000"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber.url
      }
    }

    "add total value input to the journey cache with decimal values" when {
      "the form submission is valid" in {
        val SUT = createSUT
        val removeCompanyBenefitFormData = ("totalValue", "1000.00")
        val totalValue = Map("benefitValue" -> "1000")

        when(removeCompanyBenefitJourneyCacheService.cache(eq(totalValue))(any()))
          .thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(removeCompanyBenefitFormData)
        )

        status(result) mustBe SEE_OTHER

      }
    }

    "add total value input to the journey cache with comma separated values removed" when {
      "the form submission is valid" in {
        val SUT = createSUT
        val removeCompanyBenefitFormData = ("totalValue", "123,000.00")
        val totalValue = Map("benefitValue" -> "123000")

        when(removeCompanyBenefitJourneyCacheService.cache(eq(totalValue))(any()))
          .thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(removeCompanyBenefitFormData)
        )

        status(result) mustBe SEE_OTHER

      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val SUT = createSUT
        val employmentName = "Employment Name"
        val benefitName = "Employer Provided Services"
        val referer = "Url"

        val removeCompanyBenefitFormData = ("totalValue", "")

        when(removeCompanyBenefitJourneyCacheService.mandatoryJourneyValues(any())(any()))
          .thenReturn(Future.successful(Right(Seq(employmentName, benefitName, referer))))
        when(removeCompanyBenefitJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(removeCompanyBenefitFormData)
        )

        status(result) mustBe BAD_REQUEST
      }
    }

    "return Bad Request" when {
      "the form submission is invalid" in {
        val SUT = createSUT
        val employmentName = "Employment Name"
        val benefitName = "Employer Provided Services"
        val referer = "Url"

        val removeCompanyBenefitFormData = ("totalValue", "1234Â£$%@")

        when(removeCompanyBenefitJourneyCacheService.mandatoryJourneyValues(any())(any()))
          .thenReturn(Future.successful(Right(Seq(employmentName, benefitName, referer))))
        when(removeCompanyBenefitJourneyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.submitBenefitValue()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(removeCompanyBenefitFormData)
        )

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "telephoneNumber" must {

    "show the contact by telephone page" when {
      "navigating from 'when did you stop getting benefits' page" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefitConstants.EmploymentIdKey    -> "1",
          EndCompanyBenefitConstants.EmploymentNameKey  -> employment.name,
          EndCompanyBenefitConstants.BenefitTypeKey     -> "amazing",
          EndCompanyBenefitConstants.BenefitStopDateKey -> "before6April2017",
          EndCompanyBenefitConstants.RefererKey         -> "Test"
        )
        when(removeCompanyBenefitJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        val result = SUT.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.getElementsByClass("hmrc-caption").text() must endWith(Messages("tai.benefits.ended.journey.preHeader"))
        doc must haveBackLinkNew
        doc
          .getElementById("cancelLink")
          .attr("href") mustBe controllers.benefits.routes.RemoveCompanyBenefitController.cancel.url
      }

      "has the yes field and telephone number prepopulated from the cache" in {
        val SUT = createSUT

        val telephoneNumber = "85256651"

        val cache = Map(
          EndCompanyBenefitConstants.EmploymentIdKey      -> "1",
          EndCompanyBenefitConstants.EmploymentNameKey    -> employment.name,
          EndCompanyBenefitConstants.BenefitTypeKey       -> "amazing",
          EndCompanyBenefitConstants.BenefitStopDateKey   -> "before6April2017",
          EndCompanyBenefitConstants.RefererKey           -> "Test",
          EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
          EndCompanyBenefitConstants.TelephoneNumberKey   -> telephoneNumber
        )

        when(removeCompanyBenefitJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))

        val result = SUT.telephoneNumber()(fakeRequest)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.getElementById("conditional-yesNoChoice").getElementsByAttribute("value").toString must include(
          telephoneNumber
        )
      }
    }

    "show the contact by telephone page" when {
      "navigating from 'total value of benefit' page" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefitConstants.EmploymentIdKey    -> "1",
          EndCompanyBenefitConstants.EmploymentNameKey  -> employment.name,
          EndCompanyBenefitConstants.BenefitTypeKey     -> "amazing",
          EndCompanyBenefitConstants.BenefitStopDateKey -> "before6April2017",
          EndCompanyBenefitConstants.BenefitValueKey    -> "12345",
          EndCompanyBenefitConstants.RefererKey         -> "Test"
        )
        when(removeCompanyBenefitJourneyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        val result = SUT.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.getElementsByClass("hmrc-caption").text() must endWith(Messages("tai.benefits.ended.journey.preHeader"))
        doc must haveBackLinkNew
        doc
          .getElementById("cancelLink")
          .attr("href") mustBe controllers.benefits.routes.RemoveCompanyBenefitController.cancel.url
      }
    }
  }

  "submitTelephoneNumber" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        val SUT = createSUT
        val expectedCache =
          Map(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> "12345678"
          )
        when(removeCompanyBenefitJourneyCacheService.cache(eq(expectedCache))(any()))
          .thenReturn(Future.successful(expectedCache))
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
        ).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers.url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        val SUT = createSUT
        val expectedCacheWithErasingNumber =
          Map(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.NoValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> ""
          )
        when(removeCompanyBenefitJourneyCacheService.cache(eq(expectedCacheWithErasingNumber))(any()))
          .thenReturn(Future.successful(expectedCacheWithErasingNumber))
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
        ).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers.url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val SUT = createSUT
        when(removeCompanyBenefitJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(EndCompanyBenefitConstants.RefererKey -> "Test")))
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

      "there is a form validation error (additional, controller specific constraint)" in {
        val SUT = createSUT
        when(removeCompanyBenefitJourneyCacheService.currentCache(any()))
          .thenReturn(Future.successful(Map(EndCompanyBenefitConstants.RefererKey -> "Test")))

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

        val tooManyCharsResult = SUT.submitTelephoneNumber()(
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

  "checkYourAnswers" must {
    "display check your answers containing populated values from the journey cache" in {
      val SUT = createSUT

      val stopDate = LocalDate.now()

      when(
        removeCompanyBenefitJourneyCacheService.collectedJourneyValues(
          any(classOf[scala.collection.immutable.List[String]]),
          any(classOf[scala.collection.immutable.List[String]])
        )(any())
      ).thenReturn(
        Future.successful(
          Right(
            Seq[String]("AwesomeType", "TestCompany", stopDate.toString, "Yes", "Url"),
            Seq[Option[String]](Some("10000"), Some("123456789"))
          )
        )
      )

      implicit val request = FakeRequest()

      val result = SUT.checkYourAnswers()(request)

      val expectedViewModel = RemoveCompanyBenefitsCheckYourAnswersViewModel(
        "AwesomeType",
        "TestCompany",
        stopDate,
        Some("10000"),
        "Yes",
        Some("123456789")
      )

      result rendersTheSameViewAs removeCompanyBenefitCheckYourAnswersView(expectedViewModel)
    }

    "redirect to the summary page if a value is missing from the cache " in {

      val sut = createSUT

      when(
        removeCompanyBenefitJourneyCacheService.collectedJourneyValues(
          any(classOf[scala.collection.immutable.List[String]]),
          any(classOf[scala.collection.immutable.List[String]])
        )(any())
      )
        .thenReturn(Future.successful(Left("An error has occurred")))

      val result = sut.checkYourAnswers()(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad.url

    }

  }

  "submit your answers" must {
    "invoke the back end 'end employment benefit' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number and benefit value have been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit =
          EndedCompanyBenefit("Accommodation", stopDateFormatted, Some("1000000"), "Yes", Some("0123456789"))

        when(removeCompanyBenefitJourneyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq[String](employmentId, "TestCompany", "Accommodation", stopDate, "Yes"),
              Seq[Option[String]](Some("1000000"), Some("0123456789"))
            )
          )
        )
        when(benefitsService.endedCompanyBenefit(any(), eq(employmentId.toInt), eq(endedCompanyBenefit))(any()))
          .thenReturn(Future.successful("1"))
        when(
          trackSuccessJourneyCacheService
            .cache(eq(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey), eq("true"))(any())
        )
          .thenReturn(Future.successful(Map(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey -> "true")))
        when(removeCompanyBenefitJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation.url
        verify(removeCompanyBenefitJourneyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and neither telephone number nor benefit value have been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit =
          EndedCompanyBenefit("Accommodation", stopDateFormatted, None, "No", None)

        when(removeCompanyBenefitJourneyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq[String](employmentId, "TestCompany", "Accommodation", stopDate, "No"),
              Seq[Option[String]](None, None)
            )
          )
        )
        when(benefitsService.endedCompanyBenefit(any(), eq(employmentId.toInt), eq(endedCompanyBenefit))(any()))
          .thenReturn(Future.successful("1"))
        when(
          trackSuccessJourneyCacheService
            .cache(eq(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey), eq("true"))(any())
        )
          .thenReturn(Future.successful(Map(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey -> "true")))
        when(removeCompanyBenefitJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation.url
        verify(removeCompanyBenefitJourneyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has not been provided but benefit value has been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit = EndedCompanyBenefit("Accommodation", stopDateFormatted, Some("1000000"), "No", None)

        when(removeCompanyBenefitJourneyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq[String](employmentId, "TestCompany", "Accommodation", stopDate, "No"),
              Seq[Option[String]](Some("1000000"), None)
            )
          )
        )
        when(benefitsService.endedCompanyBenefit(any(), eq(employmentId.toInt), eq(endedCompanyBenefit))(any()))
          .thenReturn(Future.successful("1"))
        when(
          trackSuccessJourneyCacheService
            .cache(eq(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey), eq("true"))(any())
        )
          .thenReturn(Future.successful(Map(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey -> "true")))
        when(removeCompanyBenefitJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation.url
        verify(removeCompanyBenefitJourneyCacheService, times(1)).flush()(any())
      }

      "the request has an authorised session and telephone number has been provided but benefit value has not been provided" in {
        val SUT = createSUT
        val employmentId: String = "1234"
        val endedCompanyBenefit =
          EndedCompanyBenefit("Accommodation", stopDateFormatted, None, "Yes", Some("0123456789"))

        when(removeCompanyBenefitJourneyCacheService.collectedJourneyValues(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq[String](employmentId, "TestCompany", "Accommodation", stopDate, "Yes"),
              Seq[Option[String]](None, Some("0123456789"))
            )
          )
        )
        when(benefitsService.endedCompanyBenefit(any(), eq(employmentId.toInt), eq(endedCompanyBenefit))(any()))
          .thenReturn(Future.successful("1"))
        when(
          trackSuccessJourneyCacheService
            .cache(eq(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey), eq("true"))(any())
        )
          .thenReturn(Future.successful(Map(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey -> "true")))
        when(removeCompanyBenefitJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.confirmation.url
        verify(removeCompanyBenefitJourneyCacheService, times(1)).flush()(any())
      }
    }
  }

  "cancel" must {
    "flush the cache and redirect to start of journey" in {
      val SUT = createSUT

      when(removeCompanyBenefitJourneyCacheService.mandatoryJourneyValues(any())(any()))
        .thenReturn(Future.successful(Right(Seq("Url"))))
      when(removeCompanyBenefitJourneyCacheService.flush()(any())).thenReturn(Future.successful(Done))

      val result = SUT.cancel(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe "Url"
      verify(removeCompanyBenefitJourneyCacheService, times(1)).flush()(any())
      verify(removeCompanyBenefitJourneyCacheService, times(1)).mandatoryJourneyValues(any())(any())
    }
  }

  "confirmation" must {
    "show the update income details confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT

        when(trackingService.isAnyIFormInProgress(any())(any()))
          .thenReturn(Future.successful(ThreeWeeks))

        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.income.previousYears.confirmation.heading"))
      }
    }
  }

  val employment: Employment = Employment(
    "company name",
    Live,
    Some("123"),
    LocalDate.parse("2016-05-26"),
    Some(LocalDate.parse("2016-05-26")),
    Nil,
    "",
    "",
    2,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false
  )

  val startOfTaxYear: String = Dates.formatDate(TaxYear().start)
  val stopDate = LocalDate.now().toString
  val stopDateFormatted = LocalDate.parse(stopDate).format(DateTimeFormatter.ofPattern(TaxDateWordMonthFormat))

  def createSUT = new SUT

  val benefitsService: BenefitsService = mock[BenefitsService]
  val removeCompanyBenefitJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val trackSuccessJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

  private val removeCompanyBenefitCheckYourAnswersView = inject[RemoveCompanyBenefitCheckYourAnswersView]

  private val removeBenefitTotalValueView = inject[RemoveBenefitTotalValueView]

  private val removeCompanyBenefitStopDateView = inject[RemoveCompanyBenefitStopDateView]

  private val trackingService = mock[TrackingService]

  class SUT
      extends RemoveCompanyBenefitController(
        removeCompanyBenefitJourneyCacheService,
        trackSuccessJourneyCacheService,
        benefitsService,
        trackingService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        langUtils,
        removeCompanyBenefitCheckYourAnswersView,
        removeCompanyBenefitStopDateView,
        removeBenefitTotalValueView,
        inject[CanWeContactByPhoneView],
        inject[RemoveCompanyBenefitConfirmationView]
      )

}
