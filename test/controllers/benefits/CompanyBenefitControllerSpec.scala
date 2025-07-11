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
import controllers.{ControllerViewTestHelper, ErrorPagesHandler}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import pages._
import pages.benefits._
import pages.testPages.EndCompanyBenefitsTypeTesterPage
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel
import utils.BaseSpec
import views.html.benefits.UpdateOrRemoveCompanyBenefitDecisionView

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

class CompanyBenefitControllerSpec extends BaseSpec with JsoupMatchers with ControllerViewTestHelper with Results {

  def createSUT          = new SUT
  def randomNino(): Nino = new Generator(new Random()).nextNino

  val empId       = 1
  val sessionId   = "testSessionId"
  val benefitType = "BenefitInKind"

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
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  val employmentService: EmploymentService               = mock[EmploymentService]
  val mockJourneyCacheRepository: JourneyCacheRepository = mock[JourneyCacheRepository]

  private val updateOrRemoveCompanyBenefitDecisionView = inject[UpdateOrRemoveCompanyBenefitDecisionView]

  class SUT
      extends CompanyBenefitController(
        employmentService = employmentService,
        authenticate = mockAuthJourney,
        mcc = mcc,
        updateOrRemoveCompanyBenefitDecision = updateOrRemoveCompanyBenefitDecisionView,
        journeyCacheRepository = mockJourneyCacheRepository,
        errorPagesHandler = inject[ErrorPagesHandler]
      ) {
    when(mockJourneyCacheRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheRepository)
  }

  "redirectCompanyBenefitSelection" must {
    "redirect to decision page" in {
      reset(mockJourneyCacheRepository)

      val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
        .setOrException(EndCompanyBenefitsIdPage, empId)
        .setOrException(EndCompanyBenefitsTypePage, benefitType)
      val SUT             = createSUT
      setup(mockUserAnswers)

      when(mockJourneyCacheRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      val result =
        SUT.redirectCompanyBenefitSelection(empId, BenefitInKind)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.CompanyBenefitController.decision().url
    }
  }

  "decision" must {
    "show 'Do you currently get benefitType from Company?' page" when {
      "the request has an authorised session" in {
        reset(mockJourneyCacheRepository)

        val empName     = "company name"
        val benefitType = "Expenses"
        val referer     = "referer"
        val SUT         = createSUT

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, empName)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
          .setOrException(EndCompanyBenefitsRefererPage, referer)
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val result = SUT.decision()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.benefits.updateOrRemove.decision.heading", benefitType, empName))

        verify(employmentService, times(1)).employment(any(), any())(any())
      }

      "prepopulate the decision selection" in {
        reset(mockJourneyCacheRepository)

        val empName = "company name"
        val referer = "referer"

        val SUT = createSUT

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
          .setOrException(EndCompanyBenefitsRefererPage, referer)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, empName)
          .setOrException(EndCompanyBenefitsNamePage, "tester")
          .setOrException(BenefitDecisionPage, YesIGetThisBenefit)
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val expectedForm: Form[Option[String]] =
          UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(Some(YesIGetThisBenefit))
        val expectedViewModel                  = CompanyBenefitDecisionViewModel(benefitType, empName, expectedForm, 2)

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")
        val result                                                    = SUT.decision(request)

        status(result) mustBe OK

        result rendersTheSameViewAs updateOrRemoveCompanyBenefitDecisionView(expectedViewModel)

        verify(mockJourneyCacheRepository).set(any())
        verify(employmentService, times(2)).employment(any(), any())(any())

      }
    }

    "throw exception" when {
      "employment not found" in {
        reset(mockJourneyCacheRepository)

        val SUT = createSUT

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
          .setOrException(EndCompanyBenefitsRefererPage, "referrer")
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = SUT.decision(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit decision" must {
    val benefitType = Telephone.name

    "cache the DecisionChoice value" when {
      "it is a NoIDontGetThisBenefit" in {
        reset(mockJourneyCacheRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
        setup(mockUserAnswers)

        val SUT = createSUT

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url

      }

      "it is a YesIGetThisBenefit" in {
        reset(mockJourneyCacheRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
        setup(mockUserAnswers)

        val SUT = createSUT

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.submitDecision(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe
          controllers.routes.ExternalServiceRedirectController
            .auditAndRedirectService(TaiConstants.CompanyBenefitsIform)
            .url
      }
    }

    "redirect to the 'When did you stop getting benefits from company?' page" when {
      "the form has the value noIDontGetThisBenefit and EndCompanyBenefitConstants.BenefitTypeKey is cached" in {
        reset(mockJourneyCacheRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
          .setOrException(EndCompanyBenefitsRefererPage, "referrer")
        setup(mockUserAnswers)

        val SUT = createSUT

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit)
        )

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url
      }
    }

    "redirect to the appropriate IFORM update page" when {
      "the form has the value yesIGetThisBenefit and EndCompanyBenefitConstants.BenefitTypeKey is cached" in {
        reset(mockJourneyCacheRepository)

        val SUT = createSUT

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "company name")
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        val result = SUT.submitDecision()(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit)
        )

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.routes.ExternalServiceRedirectController
          .auditAndRedirectService(TaiConstants.CompanyBenefitsIform)
          .url

      }
    }

    "redirect to the Tax Account Summary Page (start of journey)" when {
      "the form has the value noIDontGetThisBenefit and EndCompanyBenefitConstants.BenefitTypeKey is not cached" in {
        reset(mockJourneyCacheRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "company name")
          .setOrException(EndCompanyBenefitsTypeTesterPage, None)
        setup(mockUserAnswers)

        val SUT = createSUT

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit)
        )

        val redirectUrl = redirectLocation(result)

        redirectUrl mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)

      }

      "the form has the value YesIGetThisBenefit and EndCompanyBenefitConstants.BenefitTypeKey is not cached" in {
        reset(mockJourneyCacheRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "company name")
          .setOrException(EndCompanyBenefitsTypeTesterPage, None)
        setup(mockUserAnswers)

        val SUT = createSUT

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))
        when(mockJourneyCacheRepository.set(any())) thenReturn Future.successful(true)

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit)
        )

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url

      }

      "the form has no valid value" in {
        reset(mockJourneyCacheRepository)

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsTypePage, benefitType)
        setup(mockUserAnswers)

        val SUT = createSUT

        when(mockJourneyCacheRepository.set(any[UserAnswers])) thenReturn Future.successful(false)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> Random.alphanumeric.take(10).mkString)
        )

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val SUT = createSUT

        val mockUserAnswers = UserAnswers("testSessionId", randomNino().nino)
          .setOrException(EndCompanyBenefitsIdPage, 1)
          .setOrException(EndCompanyBenefitsTypePage, "Expenses")
          .setOrException(EndCompanyBenefitsEmploymentNamePage, "Employer A")
          .setOrException(EndCompanyBenefitsRefererPage, "referer")
        setup(mockUserAnswers)

        when(mockJourneyCacheRepository.get(any(), any()))
          .thenReturn(Future.successful(Some(mockUserAnswers)))

        val result = SUT.submitDecision(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> "")
        )

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}
