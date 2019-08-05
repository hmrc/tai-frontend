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

package controllers.benefits

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import controllers.{ControllerViewTestHelper, FakeAuthAction, FakeTaiPlayApplication}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.domain.{BenefitInKind, Employment}
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants, TaiConstants, UpdateOrRemoveCompanyBenefitDecisionConstants}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel
import views.html.benefits.updateOrRemoveCompanyBenefitDecision

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class CompanyBenefitControllerSpec
    extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport with FormValuesConstants
    with UpdateOrRemoveCompanyBenefitDecisionConstants with JourneyCacheConstants with JsoupMatchers
    with BeforeAndAfterEach with ControllerViewTestHelper {

  override def beforeEach: Unit =
    Mockito.reset(journeyCacheService)

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "redirectCompanyBenefitSelection" must {
    "redirect to decision page" in {

      val empId = 1

      val SUT = createSUT

      when(journeyCacheService.cache(Matchers.any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      val result =
        SUT.redirectCompanyBenefitSelection(empId, BenefitInKind)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.CompanyBenefitController.decision().url
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
          EndCompanyBenefit_EmploymentIdKey -> "1",
          EndCompanyBenefit_BenefitTypeKey  -> benefitType,
          EndCompanyBenefit_RefererKey      -> referer)

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.decision()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.benefits.updateOrRemove.decision.heading", benefitType, empName))

        verify(employmentService, times(1)).employment(any(), any())(any())
        verify(journeyCacheService, times(1)).currentCache(any())
        verify(journeyCacheService, times(1)).cache(
          mockEq(
            Map(
              EndCompanyBenefit_EmploymentNameKey -> empName,
              EndCompanyBenefit_BenefitNameKey    -> benefitType,
              EndCompanyBenefit_RefererKey        -> referer)))(any())
      }

      "prepopulate the decision selection" in {
        val empName = "company name"
        val benefitType = "Expenses"
        val referer = "/check-income-tax/income-summary"

        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefit_EmploymentIdKey -> "1",
          EndCompanyBenefit_BenefitTypeKey  -> benefitType,
          EndCompanyBenefit_RefererKey      -> referer,
          DecisionChoice                    -> YesIGetThisBenefit
        )

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val expectedForm: Form[Option[String]] =
          UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(Some(YesIGetThisBenefit))
        val expectedViewModel = CompanyBenefitDecisionViewModel(benefitType, empName, expectedForm)

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")
        val result = SUT.decision()(request)

        result rendersTheSameViewAs updateOrRemoveCompanyBenefitDecision(expectedViewModel)
      }
    }

    "throw exception" when {
      "employment not found" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefit_EmploymentIdKey -> "1",
          EndCompanyBenefit_BenefitTypeKey  -> "type",
          EndCompanyBenefit_RefererKey      -> "referrer")

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = SUT.decision()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit decision" must {

    "redirect to the 'When did you stop getting benefits from company?' page" when {
      "the form has the value noIDontGetThisBenefit" in {

        val SUT = createSUT

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit))

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url

      }
    }

    "redirect to the appropriate IFORM update page" when {
      "the form has the value yesIGetThisBenefit" in {

        val SUT = createSUT

        val result = SUT.submitDecision()(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit))

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.routes.ExternalServiceRedirectController
          .auditInvalidateCacheAndRedirectService(TaiConstants.CompanyBenefitsIform)
          .url

      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val SUT = createSUT
        val cache = Map(
          EndCompanyBenefit_EmploymentNameKey -> "Employer A",
          EndCompanyBenefit_BenefitTypeKey    -> "Expenses",
          EndCompanyBenefit_RefererKey        -> "/check-income-tax/income-summary"
        )

        when(journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        val result = SUT.submitDecision(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> ""))

        status(result) mustBe BAD_REQUEST

        verify(journeyCacheService, times(1)).currentCache(any())

      }
    }

    "cache the DecisionChoice value" when {
      "it is a NoIDontGetThisBenefit" in {
        val SUT = createSUT

        val result = SUT.submitDecision(
          RequestBuilder
            .buildFakeRequestWithAuth("POST")
            .withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit))

        Await.result(result, 5.seconds)

        verify(journeyCacheService, times(1))
          .cache(Matchers.eq(DecisionChoice), Matchers.eq(NoIDontGetThisBenefit))(any())
      }

      "it is a YesIGetThisBenefit" in {
        val SUT = createSUT

        val result = SUT.submitDecision(
          RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit))

        Await.result(result, 5.seconds)

        verify(journeyCacheService, times(1)).cache(Matchers.eq(DecisionChoice), Matchers.eq(YesIGetThisBenefit))(any())
      }
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  def createSUT = new SUT

  val employment = Employment(
    "company name",
    Some("123"),
    new LocalDate("2016-05-26"),
    Some(new LocalDate("2016-05-26")),
    Nil,
    "",
    "",
    2,
    None,
    false,
    false)

  val employmentService = mock[EmploymentService]
  val journeyCacheService = mock[JourneyCacheService]

  class SUT
      extends CompanyBenefitController(
        employmentService,
        journeyCacheService,
        FakeAuthAction,
        FakeValidatePerson,
        MockTemplateRenderer,
        mock[FormPartialRetriever]) {
    when(journeyCacheService.cache(any(), any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
  }
}
