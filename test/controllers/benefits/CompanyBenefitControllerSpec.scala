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

package controllers.benefits

import builders.{AuthBuilder, RequestBuilder}
import controllers.FakeTaiPlayApplication
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => mockEq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.TaiRoot
import uk.gov.hmrc.tai.model.domain.{BenefitInKind, Employment}
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, JourneyCacheService, TaiService}
import uk.gov.hmrc.tai.util._
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers

import scala.concurrent.Future
import scala.util.Random


class CompanyBenefitControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with FormValuesConstants
  with UpdateOrRemoveCompanyBenefitDecisionConstants
  with JourneyCacheConstants
  with DateFormatConstants
  with JsoupMatchers{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "redirectCompanyBenefitSelection" must {
    "redirect to decision page" in {

      val empId = 1

      val SUT = createSUT

      when(SUT.journeyCacheService.cache(Matchers.any())(any())).thenReturn(Future.successful(Map.empty[String, String]))

      val result = SUT.redirectCompanyBenefitSelection(empId, BenefitInKind)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.CompanyBenefitController.decision().url
    }
  }

  "decision" must {
    "show 'Do you currently get benefitType from Company?' page" when {
      "the request has an authorised session" in {

        val empName = "company name"
        val benefitType = "Expenses"

        val SUT = createSUT
        val cache = Map(EndCompanyBenefit_EmploymentIdKey -> "1",EndCompanyBenefit_BenefitTypeKey -> benefitType)

        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(SUT.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.decision(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.benefits.updateOrRemove.decision.title")

        verify(SUT.employmentService, times(1)).employment(any(),any())(any())
        verify(SUT.journeyCacheService, times(1)).currentCache(any())
        verify(SUT.journeyCacheService, times(1)).cache(
          mockEq(Map(EndCompanyBenefit_EmploymentNameKey -> empName, EndCompanyBenefit_BenefitNameKey -> benefitType)))(any())
      }


      "show the word benefit once when benefit is part of the benefit name " in {

        val empName = "company name"
        val benefitType = "NonCashBenefit"
        val formattedBenefitName = "Non-cash"

        val SUT = createSUT
        val cache = Map(EndCompanyBenefit_EmploymentIdKey -> "1",EndCompanyBenefit_BenefitTypeKey -> benefitType)

        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(SUT.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.decision(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.benefits.updateOrRemove.decision.title")

        doc must haveHeadingWithText(Messages("tai.benefits.updateOrRemove.decision.heading",formattedBenefitName, empName))

        verify(SUT.journeyCacheService, times(1)).cache(
          mockEq(Map(EndCompanyBenefit_EmploymentNameKey -> empName,EndCompanyBenefit_BenefitNameKey -> formattedBenefitName)))(any())

      }

      "show the word benefit once when benefits is part of the benefit name " in {

        val empName = "company name"
        val benefitType = "ServiceBenefit"
        val formattedBenefitName = "Service"

        val SUT = createSUT
        val cache = Map(EndCompanyBenefit_EmploymentIdKey -> "1",EndCompanyBenefit_BenefitTypeKey -> benefitType)

        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        when(SUT.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))
        when(SUT.journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map("" -> "")))

        val result = SUT.decision(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.benefits.updateOrRemove.decision.title")

        doc must haveHeadingWithText(Messages("tai.benefits.updateOrRemove.decision.heading",formattedBenefitName, empName))

        verify(SUT.journeyCacheService, times(1)).cache(
          mockEq(Map(EndCompanyBenefit_EmploymentNameKey -> empName,EndCompanyBenefit_BenefitNameKey -> formattedBenefitName)))(any())

      }

    }

    "throw exception" when {
      "employment not found" in {
        val SUT = createSUT
        when(SUT.employmentService.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val result = SUT.decision()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submit decision" must {

    "redirect to the 'When did you stop getting benefits from company?' page" when {
      "the form has the value noIDontGetThisBenefit" in {

        val SUT = createSUT

        val result = SUT.submitDecision(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(DecisionChoice -> NoIDontGetThisBenefit))

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url

      }
    }

    "redirect to the appropriate IFORM update page" when {
      "the form has the value yesIGetThisBenefit" in {

        val SUT = createSUT

        val result = SUT.submitDecision()(RequestBuilder.buildFakeRequestWithAuth("POST").
          withFormUrlEncodedBody(DecisionChoice -> YesIGetThisBenefit))

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).getOrElse("")

        redirectUrl mustBe controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.CompanyBenefitsIform).url

      }
    }

    "return Bad Request" when {
      "the form submission is having blank value" in {
        val SUT = createSUT
        val cache = Map(EndCompanyBenefit_EmploymentNameKey -> "Employer A",EndCompanyBenefit_BenefitNameKey -> "Expenses")

        when(SUT.journeyCacheService.currentCache(any())).thenReturn(Future.successful(cache))
        val result = SUT.submitDecision(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(DecisionChoice -> ""))

        status(result) mustBe BAD_REQUEST

        verify(SUT.journeyCacheService, times(1)).currentCache(any())

      }
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  def generateNino: Nino = new Generator(new Random).nextNino

  private def createSUT = new SUT

  private val employment = Employment("company name", Some("123"), new LocalDate("2016-05-26"),
    Some(new LocalDate("2016-05-26")), Nil, "", "", 2)

  private class SUT extends CompanyBenefitController {

    override val taiService: TaiService = mock[TaiService]
    override val auditService: AuditService = mock[AuditService]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: PartialRetriever = mock[PartialRetriever]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val trackingJourneyCacheService: JourneyCacheService = mock[JourneyCacheService]

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))

    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(TaiRoot("", 1, "", "", None, "", "", false, None)))
  }
}