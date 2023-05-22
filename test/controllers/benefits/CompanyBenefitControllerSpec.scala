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

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{ControllerViewTestHelper, ErrorPagesHandler, FakeAuthAction}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.tai.DecisionCacheWrapper
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{BenefitInKind, Employment, Telephone}
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants.{DecisionChoice, NoIDontGetThisBenefit, YesIGetThisBenefit}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel
import utils.BaseSpec
import views.html.benefits.UpdateOrRemoveCompanyBenefitDecisionView

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class CompanyBenefitControllerSpec
    extends BaseSpec with JsoupMatchers with BeforeAndAfterEach with ControllerViewTestHelper {

  override def beforeEach: Unit =
    Mockito.reset(journeyCacheService)

  "redirectCompanyBenefitSelection" must {
    "redirect to decision page" in {

      val empId = 1

      val SUT = createSUT

      when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      val result =
        SUT.redirectCompanyBenefitSelection(empId, BenefitInKind)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.CompanyBenefitController.decision.url
    }
  }

  "decision" must {
    "show 'Do you currently get benefitType from Company?' page" when {
      "the request has an authorised session" in {

        val empName = "company name"
        val benefitType = "Expenses"
        val referer = "/check-income-tax/income-summary"

        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefitConstants.EmploymentIdKey -> "1",
          EndCompanyBenefitConstants.BenefitTypeKey  -> benefitType,
          EndCompanyBenefitConstants.RefererKey      -> referer
        )

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        when(journeyCacheService.mandatoryJourneyValue(any())(any())).thenReturn(Future.successful(Left("")))

        val result = SUT.decision()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.benefits.updateOrRemove.decision.heading", benefitType, empName))

        verify(employmentService, times(1)).employment(any(), any())(any())
        verify(journeyCacheService, times(1)).currentCache(any())
        verify(journeyCacheService, times(1)).cache(
          eqTo(
            Map(
              EndCompanyBenefitConstants.EmploymentNameKey -> empName,
              EndCompanyBenefitConstants.BenefitNameKey    -> benefitType,
              EndCompanyBenefitConstants.RefererKey        -> referer
            )
          )
        )(any())
      }

      "prepopulate the decision selection" in {
        val empName = "company name"
        val benefitType = "Expenses"
        val referer = "/check-income-tax/income-summary"

        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefitConstants.EmploymentIdKey -> "1",
          EndCompanyBenefitConstants.BenefitTypeKey  -> benefitType,
          EndCompanyBenefitConstants.RefererKey      -> referer,
          s"$benefitType $DecisionChoice"            -> YesIGetThisBenefit
        )

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))
        when(journeyCacheService.mandatoryJourneyValue(any())(any())).thenReturn(Future.successful(Right(benefitType)))
        when(journeyCacheService.currentValue(any())(any()))
          .thenReturn(Future.successful(Some(YesIGetThisBenefit)))

        val expectedForm: Form[Option[String]] =
          UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(Some(YesIGetThisBenefit))
        val expectedViewModel = CompanyBenefitDecisionViewModel(benefitType, empName, expectedForm, 2)

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeRequestWithAuth("GET")
        val result = SUT.decision()(request)

        result rendersTheSameViewAs updateOrRemoveCompanyBenefitDecisionView(expectedViewModel)
      }
    }

    "throw exception" when {
      "employment not found" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefitConstants.EmploymentIdKey -> "1",
          EndCompanyBenefitConstants.BenefitTypeKey  -> "type",
          EndCompanyBenefitConstants.RefererKey      -> "referrer"
        )

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = SUT.decision()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit decision" must {

    def ensureBenefitTypeInCache(): String = {
      val benefitType = Telephone.name
      when(journeyCacheService.mandatoryJourneyValue(eqTo(EndCompanyBenefitConstants.BenefitTypeKey))(any()))
        .thenReturn(Future.successful(Right(benefitType)))
      benefitType
    }

    def ensureBenefitTypeOutOfCache(): Unit =
      when(journeyCacheService.mandatoryJourneyValue(eqTo(EndCompanyBenefitConstants.BenefitTypeKey))(any()))
        .thenReturn(Future.successful(Left("")))

    "redirect to the 'When did you stop getting benefits from company?' page" when {
      "the form has the value noIDontGetThisBenefit and EndCompanyBenefitConstants.BenefitTypeKey is cached" in {

        val SUT = createSUT
        ensureBenefitTypeInCache()

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit)
        )

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.stopDate.url

      }
    }

    "redirect to the appropriate IFORM update page" when {
      "the form has the value yesIGetThisBenefit and EndCompanyBenefitConstants.BenefitTypeKey is cached" in {

        val SUT = createSUT
        ensureBenefitTypeInCache()

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
        val SUT = createSUT
        ensureBenefitTypeOutOfCache()

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit)
        )

        val redirectUrl = redirectLocation(result)

        redirectUrl mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad.url)

      }

      "the form has the value YesIGetThisBenefit and EndCompanyBenefitConstants.BenefitTypeKey is not cached" in {
        val SUT = createSUT
        ensureBenefitTypeOutOfCache()

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit)
        )

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.routes.TaxAccountSummaryController.onPageLoad.url

      }

      "the form has no valid value" in {
        val SUT = createSUT
        ensureBenefitTypeInCache()

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> Random.alphanumeric.take(10).mkString)
        )

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.routes.TaxAccountSummaryController.onPageLoad.url
      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefitConstants.EmploymentNameKey -> "Employer A",
          EndCompanyBenefitConstants.BenefitTypeKey    -> "Expenses",
          EndCompanyBenefitConstants.RefererKey        -> "/check-income-tax/income-summary",
          EndCompanyBenefitConstants.EmploymentIdKey   -> "1"
        )

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        val result = SUT.submitDecision(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> "")
        )

        status(result) mustBe BAD_REQUEST

        verify(journeyCacheService, times(1)).currentCache(any())

      }
    }

    "cache the DecisionChoice value" when {
      "it is a NoIDontGetThisBenefit" in {
        val SUT = createSUT

        val benefitType = ensureBenefitTypeInCache()
        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit)
        )

        Await.result(result, 5.seconds)

        verify(journeyCacheService, times(1))
          .cache(eqTo(s"$benefitType $DecisionChoice"), eqTo(NoIDontGetThisBenefit))(any())
      }

      "it is a YesIGetThisBenefit" in {
        val SUT = createSUT

        val benefitType = ensureBenefitTypeInCache()

        val result = SUT.submitDecision(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit)
        )

        Await.result(result, 5.seconds)
        verify(journeyCacheService, times(1))
          .cache(eqTo(s"$benefitType $DecisionChoice"), eqTo(YesIGetThisBenefit))(any())
      }
    }
  }

  def createSUT = new SUT

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

  val employmentService: EmploymentService = mock[EmploymentService]
  val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
  val decisionCacheWrapper: DecisionCacheWrapper = mock[DecisionCacheWrapper]

  private val updateOrRemoveCompanyBenefitDecisionView = inject[UpdateOrRemoveCompanyBenefitDecisionView]

  class SUT
      extends CompanyBenefitController(
        employmentService,
        new DecisionCacheWrapper(journeyCacheService),
        journeyCacheService,
        FakeAuthAction,
        FakeValidatePerson,
        mcc,
        updateOrRemoveCompanyBenefitDecisionView,
        inject[ErrorPagesHandler]
      ) {
    when(journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
  }
}
