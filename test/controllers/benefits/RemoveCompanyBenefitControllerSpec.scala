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

package controllers.benefits

import builders.RequestBuilder
import controllers.ControllerViewTestHelper
import controllers.auth.{AuthedUser, DataRequest}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.stubbing.OngoingStubbing
import pages.benefits._
import pages.{EndCompanyBenefitsTelephoneTesterNumberPage, EndCompanyBenefitsValueTesterPage}
import play.api.i18n.Messages
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.forms.benefits.{CompanyBenefitTotalValueForm, RemoveCompanyBenefitStopDateForm}
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{ThreeWeeks, TrackingService}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.TaiConstants.TaxDateWordMonthFormat
import uk.gov.hmrc.tai.util.constants.journeyCache.EndCompanyBenefitConstants
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.viewModels.benefit.{BenefitViewModel, RemoveCompanyBenefitsCheckYourAnswersViewModel}
import utils.BaseSpec
import views.html.CanWeContactByPhoneView
import views.html.benefits._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class RemoveCompanyBenefitControllerSpec extends BaseSpec with JsoupMatchers with ControllerViewTestHelper {

  val employment: Employment = Employment(
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

  val sessionId = "testSessionId"
  val startOfTaxYear: String = Dates.formatDate(TaxYear().start)
  val stopDate: String = LocalDate.now().toString
  val stopDateFormatted: String =
    LocalDate.parse(stopDate).toString.format(DateTimeFormatter.ofPattern(TaxDateWordMonthFormat))

  def createSUT = new SUT
  def randomNino(): Nino = new Generator(new Random()).nextNino

  val benefitsService: BenefitsService = mock[BenefitsService]
  private val trackingService = mock[TrackingService]
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  private val removeCompanyBenefitCheckYourAnswersView = inject[RemoveCompanyBenefitCheckYourAnswersView]
  private val removeBenefitTotalValueView = inject[RemoveBenefitTotalValueView]
  private val removeCompanyBenefitStopDateView = inject[RemoveCompanyBenefitStopDateView]

  class SUT
      extends RemoveCompanyBenefitController(
        benefitsService,
        trackingService,
        mockAuthJourney,
        mcc,
        removeCompanyBenefitCheckYourAnswersView,
        removeCompanyBenefitStopDateView,
        removeBenefitTotalValueView,
        inject[CanWeContactByPhoneView],
        inject[RemoveCompanyBenefitConfirmationView],
        mockJourneyCacheNewRepository
      ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  private def setup(ua: UserAnswers): OngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
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
    reset(benefitsService)
  }

  "stopDate" must {
    "show 'When did you stop getting benefits from company?' page" in {
      reset(mockJourneyCacheNewRepository)

      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(EndCompanyBenefitsEmploymentNamePage, "Test")
        .setOrException(EndCompanyBenefitsNamePage, "Test")
        .setOrException(EndCompanyBenefitsRefererPage, "Test")
      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result = SUT.stopDate(fakeRequest)

      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.benefits.ended.stopDate.heading", "Test", "Test"))

    }

    "show the prepopulated fields when an EndCompanyBenefitsStopDatePage benefitStopDateKey has been cached" in {
      reset(mockJourneyCacheNewRepository)

      val stopDate = LocalDate.now()
      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(EndCompanyBenefitsEmploymentNamePage, "EmploymentName")
        .setOrException(EndCompanyBenefitsNamePage, "BenefitName")
        .setOrException(EndCompanyBenefitsRefererPage, "Referer")
        .setOrException(EndCompanyBenefitsStopDatePage, stopDate.toString)
      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")
      val result = SUT.stopDate()(request)

      val expectedView = {
        val form = RemoveCompanyBenefitStopDateForm("BenefitName", "EmploymentName").form
          .fill(
            LocalDate.parse(
              mockUserAnswers
                .get(EndCompanyBenefitsStopDatePage)
                .get
            )
          )
        removeCompanyBenefitStopDateView(
          form,
          mockUserAnswers.get(EndCompanyBenefitsNamePage).get,
          mockUserAnswers.get(EndCompanyBenefitsEmploymentNamePage).get
        )
      }

      result rendersTheSameViewAs expectedView
    }
  }

  "submit stop date" must {
    "redirect to the 'Can we call you if we need more information?' page" when {
      "the date is before this tax year" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")
          .setOrException(EndCompanyBenefitsNamePage, "benefit")
        val SUT = createSUT
        setup(mockUserAnswers)

        val year = TaxYear().year.toString

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.submitStopDate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "01",
              RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "01",
              RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> year
            )
        )
        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url

        verify(mockJourneyCacheNewRepository).set(any())

      }
    }

    "redirect to the 'What was the total value of your benefit' page" when {
      "the date is during this tax year" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsNamePage, "benefit")
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")

        val SUT = createSUT
        setup(mockUserAnswers)

        val taxYear = TaxYear()
        val year = taxYear.year.toString

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.submitStopDate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "06",
              RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "04",
              RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> year
            )
        )
        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url

        val updatedUserAnswers = mockUserAnswers.setOrException(EndCompanyBenefitsStopDatePage, s"$year-04-06")
        verify(mockJourneyCacheNewRepository, times(1)).set(argThat(new ArgumentMatcher[UserAnswers] {
          override def matches(argument: UserAnswers): Boolean =
            argument.sessionId == updatedUserAnswers.sessionId &&
              argument.nino == updatedUserAnswers.nino &&
              argument.data == updatedUserAnswers.data
        }))
      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsNamePage, "benefit")
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.submitStopDate(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(
              RemoveCompanyBenefitStopDateForm.BenefitFormDay   -> "",
              RemoveCompanyBenefitStopDateForm.BenefitFormMonth -> "",
              RemoveCompanyBenefitStopDateForm.BenefitFormYear  -> ""
            )
        )

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "totalValueOfBenefit" must {

    val employmentName = "Employment Name"
    val benefitName = "Employer Provided Services"
    val referer = "Url"

    "show what was the total value page" when {
      "the request has an authorised session with employment name and benefit name" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employmentName)
          .setOrException(EndCompanyBenefitsNamePage, benefitName)
          .setOrException(EndCompanyBenefitsRefererPage, referer)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.totalValueOfBenefit()(fakeRequest)

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))

        doc.title() must include(
          Messages("tai.remove.company.benefit.total.value.heading", benefitName, employmentName)
        )
      }
    }

    "the value of benefit is prepopulated with the cached amount" in {
      reset(mockJourneyCacheNewRepository)

      val valueOfBenefit = "9876543"

      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(EndCompanyBenefitsEmploymentNamePage, employmentName)
        .setOrException(EndCompanyBenefitsNamePage, benefitName)
        .setOrException(EndCompanyBenefitsValuePage, valueOfBenefit)

      val SUT = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      implicit val request: FakeRequest[AnyContent] = fakeRequest

      val result = SUT.totalValueOfBenefit()(fakeRequest)

      val expectedForm = CompanyBenefitTotalValueForm.form.fill(valueOfBenefit)
      val expectedViewModel = BenefitViewModel(employmentName, benefitName)
      result rendersTheSameViewAs removeBenefitTotalValueView(expectedViewModel, expectedForm)
    }
  }

  "submitBenefitValue" must {
    "redirect to the 'Can we contact you' page" when {
      "the form submission is valid" in {
        reset(mockJourneyCacheNewRepository)

        val valueOfBenefit = "1000"

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsValuePage, valueOfBenefit)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.submitBenefitValue()(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(("totalValue", "1000"))
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(
          result
        ).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url
      }
    }

    "add total value input to the journey cache with decimal values" when {
      "the form submission is valid" in {
        reset(mockJourneyCacheNewRepository)

        val valueOfBenefit = "1000"

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsValuePage, valueOfBenefit)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val removeCompanyBenefitFormData = ("totalValue", "1000.00")

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
        reset(mockJourneyCacheNewRepository)

        val valueOfBenefit = "123000"

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsValuePage, valueOfBenefit)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val removeCompanyBenefitFormData = ("totalValue", "123,000.00")

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
        reset(mockJourneyCacheNewRepository)

        val valueOfBenefit = ""
        val employmentName = "Employment Name"
        val benefitName = "Employer Provided Services"
        val referer = "Url"

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employmentName)
          .setOrException(EndCompanyBenefitsNamePage, benefitName)
          .setOrException(EndCompanyBenefitsRefererPage, referer)
          .setOrException(EndCompanyBenefitsValuePage, valueOfBenefit)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val removeCompanyBenefitFormData = ("totalValue", "")

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
        reset(mockJourneyCacheNewRepository)

        val employmentName = "Employment Name"
        val benefitName = "Employer Provided Services"
        val referer = "Url"
        val valueOfBenefit = "1234Â£$%@"

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employmentName)
          .setOrException(EndCompanyBenefitsNamePage, benefitName)
          .setOrException(EndCompanyBenefitsRefererPage, referer)
          .setOrException(EndCompanyBenefitsValuePage, valueOfBenefit)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val removeCompanyBenefitFormData = ("totalValue", "1234Â£$%@")

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
        reset(mockJourneyCacheNewRepository)

        val telephoneNumber = "85256651"

        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          randomNino().nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> telephoneNumber
          )
        ).setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employment.name)
          .setOrException(EndCompanyBenefitsTypePage, "amazing")
          .setOrException(EndCompanyBenefitsStopDatePage, "before6April2017")
          .setOrException(EndCompanyBenefitsRefererPage, "Test")
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(EndCompanyBenefitsTelephoneNumberPage, telephoneNumber)

        val SUT = createSUT
        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.getElementsByClass("govuk-caption-xl hmrc-caption-xl").text() must endWith(
          Messages("tai.benefits.ended.journey.preHeader")
        )
        doc must haveBackLink
        doc
          .getElementById("cancelLink")
          .attr("href") mustBe controllers.benefits.routes.RemoveCompanyBenefitController.cancel().url
      }

      "has the yes field and telephone number prepopulated from the cache" in {
        reset(mockJourneyCacheNewRepository)

        val telephoneNumber = "85256651"

        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          randomNino().nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> telephoneNumber
          )
        ).setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employment.name)
          .setOrException(EndCompanyBenefitsTypePage, "amazing")
          .setOrException(EndCompanyBenefitsStopDatePage, "before6April2017")
          .setOrException(EndCompanyBenefitsRefererPage, "Test")
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(EndCompanyBenefitsTelephoneNumberPage, telephoneNumber)

        setup(mockUserAnswers)
        val SUT = createSUT

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

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
        reset(mockJourneyCacheNewRepository)

        val telephoneNumber = "85256651"

        val mockUserAnswers = UserAnswers(sessionId = "testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employment.name)
          .setOrException(EndCompanyBenefitsTypePage, "amazing")
          .setOrException(EndCompanyBenefitsStopDatePage, "before6April2017")
          .setOrException(EndCompanyBenefitsValuePage, "12345")
          .setOrException(EndCompanyBenefitsRefererPage, "Test")
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(EndCompanyBenefitsTelephoneNumberPage, telephoneNumber)

        setup(mockUserAnswers)
        val SUT = createSUT

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = SUT.telephoneNumber()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        val doc = Jsoup.parse(contentAsString(result))

        doc.title() must include(Messages("tai.canWeContactByPhone.title"))
        doc.getElementsByClass("govuk-caption-xl hmrc-caption-xl").text() must endWith(
          Messages("tai.benefits.ended.journey.preHeader")
        )
        doc must haveBackLink
        doc
          .getElementById("cancelLink")
          .attr("href") mustBe controllers.benefits.routes.RemoveCompanyBenefitController.cancel().url
      }
    }
  }

  "submitTelephoneNumber" must {
    "redirect to the check your answers page" when {
      "the request has an authorised session, and a telephone number has been provided" in {
        reset(mockJourneyCacheNewRepository)

        val telephoneNumber = "12345678"

        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          randomNino().nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> telephoneNumber
          )
        ).setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employment.name)
          .setOrException(EndCompanyBenefitsTypePage, "amazing")
          .setOrException(EndCompanyBenefitsStopDatePage, "before6April2017")
          .setOrException(EndCompanyBenefitsValuePage, "12345")
          .setOrException(EndCompanyBenefitsRefererPage, "Test")
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(EndCompanyBenefitsTelephoneNumberPage, telephoneNumber)

        setup(mockUserAnswers)
        val SUT = createSUT

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
        ).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers().url
      }

      "the request has an authorised session, and telephone number contact has not been approved" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          randomNino().nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.NoValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> ""
          )
        ).setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, employment.name)
          .setOrException(EndCompanyBenefitsTypePage, "amazing")
          .setOrException(EndCompanyBenefitsStopDatePage, "before6April2017")
          .setOrException(EndCompanyBenefitsValuePage, "12345")
          .setOrException(EndCompanyBenefitsRefererPage, "Test")
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, FormValuesConstants.YesValue)
          .setOrException(EndCompanyBenefitsTelephoneNumberPage, "")

        setup(mockUserAnswers)
        val SUT = createSUT

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
        ).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers().url
      }
    }

    "return BadRequest" when {
      "there is a form validation error (standard form validation)" in {
        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          nino.nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> "0123456789",
            EndCompanyBenefitConstants.EmploymentIdKey      -> "1234"
          )
        )
          .setOrException(EndCompanyBenefitsIdPage, 1234)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")
          .setOrException(EndCompanyBenefitsTypePage, "Accommodation")
          .setOrException(EndCompanyBenefitsStopDatePage, stopDateFormatted)
          .setOrException(EndCompanyBenefitsValuePage, "10000")
          .setOrException(EndCompanyBenefitsRefererPage, "Test")
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "Yes")
          .setOrException(EndCompanyBenefitsTelephoneNumberPage, "0123456789")
          .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, "true")

        setup(mockUserAnswers)

        val SUT = createSUT

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
        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          nino.nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> "0123456789"
          )
        )
          .setOrException(EndCompanyBenefitsIdPage, 1234)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")
          .setOrException(EndCompanyBenefitsTypePage, "Accommodation")
          .setOrException(EndCompanyBenefitsStopDatePage, stopDateFormatted)
          .setOrException(EndCompanyBenefitsValuePage, "10000")
          .setOrException(EndCompanyBenefitsRefererPage, "Test")
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "Yes")
          .setOrException(EndCompanyBenefitsTelephoneNumberPage, "0123456789")
          .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, "true")

        setup(mockUserAnswers)

        val SUT = createSUT

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
      reset(mockJourneyCacheNewRepository)
      reset(benefitsService)

      val mockUserAnswers = UserAnswers(
        sessionId = "testSessionId",
        nino.nino,
        data = Json.obj(
          EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
          EndCompanyBenefitConstants.TelephoneNumberKey   -> "123456789"
        )
      )
        .setOrException(EndCompanyBenefitsIdPage, 1234)
        .setOrException(EndCompanyBenefitsEmploymentNamePage, "AwesomeType")
        .setOrException(EndCompanyBenefitsNamePage, "TestCompany")
        .setOrException(EndCompanyBenefitsTypePage, "AwesomeType")
        .setOrException(EndCompanyBenefitsStopDatePage, stopDateFormatted)
        .setOrException(EndCompanyBenefitsValueTesterPage, Some("10000"))
        .setOrException(EndCompanyBenefitsRefererPage, "Url")
        .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "Yes")
        .setOrException(EndCompanyBenefitsTelephoneTesterNumberPage, Some("123456789"))
        .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, "true")

      setup(mockUserAnswers)

      val SUT = createSUT

      val stopDate = LocalDate.now()

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

      val result = SUT.checkYourAnswers()(request)

      val expectedViewModel = RemoveCompanyBenefitsCheckYourAnswersViewModel(
        benefitType = "AwesomeType",
        employerName = "TestCompany",
        stopDate = stopDate,
        valueOfBenefit = Some("10000"),
        contactByPhone = "Yes",
        phoneNumber = Some("123456789")
      )

      result rendersTheSameViewAs removeCompanyBenefitCheckYourAnswersView(expectedViewModel)
    }

    "redirect to the summary page if a value is missing from the cache " in {
      reset(mockJourneyCacheNewRepository)

      val mockUserAnswers = UserAnswers(
        sessionId = "testSessionId",
        nino.nino,
        data = Json.obj(
          EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
          EndCompanyBenefitConstants.TelephoneNumberKey   -> "0123456789"
        )
      )
        .setOrException(EndCompanyBenefitsIdPage, 1234)
        .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")
        .setOrException(EndCompanyBenefitsTypePage, "Accommodation")
        .setOrException(EndCompanyBenefitsStopDatePage, stopDateFormatted)
        .setOrException(EndCompanyBenefitsValueTesterPage, None)
        .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "Yes")
        .setOrException(EndCompanyBenefitsTelephoneNumberPage, "0123456789")
        .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, "true")

      setup(mockUserAnswers)
      val sut = createSUT

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(None))

      val result = sut.checkYourAnswers()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }

  }

  "submit your answers" must {
    "invoke the back end 'end employment benefit' service and redirect to the confirmation page" when {
      "the request has an authorised session and a telephone number and benefit value have been provided" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          nino.nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> "0123456789"
          )
        )
          .setOrException(EndCompanyBenefitsIdPage, 1234)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")
          .setOrException(EndCompanyBenefitsTypePage, "Accommodation")
          .setOrException(EndCompanyBenefitsStopDatePage, stopDateFormatted)
          .setOrException(EndCompanyBenefitsValueTesterPage, Some("1000000"))
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "Yes")
          .setOrException(EndCompanyBenefitsTelephoneTesterNumberPage, Some("0123456789"))
          .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, "true")

        setup(mockUserAnswers)
        val SUT = createSUT

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        when(
          benefitsService
            .endedCompanyBenefit(any(), any(), any())(any(), any())
        )
          .thenReturn(Future.successful("1"))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController
          .confirmation()
          .url
      }

      "the request has an authorised session and neither telephone number nor benefit value have been provided" in {
        reset(mockJourneyCacheNewRepository)

        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          nino.nino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> "0123456789"
          )
        )
          .setOrException(EndCompanyBenefitsIdPage, 1234)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")
          .setOrException(EndCompanyBenefitsTypePage, "Accommodation")
          .setOrException(EndCompanyBenefitsStopDatePage, stopDateFormatted)
          .setOrException(EndCompanyBenefitsValueTesterPage, None)
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "No")
          .setOrException(EndCompanyBenefitsTelephoneTesterNumberPage, None)
          .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, "true")

        setup(mockUserAnswers)
        val SUT = createSUT

        when(
          benefitsService
            .endedCompanyBenefit(any(), any(), any())(any(), any())
        )
          .thenReturn(Future.successful("1"))

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController
          .confirmation()
          .url
      }

      "the request has an authorised session and telephone number has not been provided but benefit value has been provided" in {
        reset(mockJourneyCacheNewRepository)
        reset(benefitsService)

        val testNino = "KH139703B"
        val mockUserAnswers = UserAnswers(
          sessionId = "testSessionId",
          testNino,
          data = Json.obj(
            EndCompanyBenefitConstants.TelephoneQuestionKey -> FormValuesConstants.YesValue,
            EndCompanyBenefitConstants.TelephoneNumberKey   -> "0123456789"
          )
        )
          .setOrException(EndCompanyBenefitsIdPage, 1234)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "employment")
          .setOrException(EndCompanyBenefitsTypePage, "Accommodation")
          .setOrException(EndCompanyBenefitsStopDatePage, stopDateFormatted)
          .setOrException(EndCompanyBenefitsValueTesterPage, Some("1000000"))
          .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "No")
          .setOrException(EndCompanyBenefitsTelephoneTesterNumberPage, None)
          .setOrException(EndCompanyBenefitsEndEmploymentBenefitsPage, "true")

        setup(mockUserAnswers)
        val SUT = createSUT

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any())) thenReturn Future.successful(true)

        when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

        when(
          benefitsService
            .endedCompanyBenefit(any(), any(), any())(any(), any())
        )
          .thenReturn(Future.successful("1"))

        val result = SUT.submitYourAnswers()(RequestBuilder.buildFakeRequestWithAuth("POST"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController
          .confirmation()
          .url

      }
    }
  }

  "cancel" must {
    "clear the cache and redirect to start of journey" in {
      reset(mockJourneyCacheNewRepository)

      val testNino = "KH139703B"
      val mockUserAnswers = UserAnswers(sessionId = "testSessionId", testNino)
        .setOrException(EndCompanyBenefitsRefererPage, "Test")

      setup(mockUserAnswers)
      val SUT = createSUT

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      when(mockJourneyCacheNewRepository.clear(any(), any())) thenReturn Future.successful(true)

      val result = SUT.cancel(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe SEE_OTHER

      redirectLocation(result).get mustBe "Test"
      verify(mockJourneyCacheNewRepository, times(1)).clear(sessionId, testNino)
    }
  }

  "confirmation" must {
    "show the update income details confirmation page" when {
      "the request has an authorised session" in {
        val sut = createSUT

        when(trackingService.isAnyIFormInProgress(any())(any(), any()))
          .thenReturn(Future.successful(ThreeWeeks))

        val result = sut.confirmation()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.income.previousYears.confirmation.heading"))
      }
    }
  }

}
