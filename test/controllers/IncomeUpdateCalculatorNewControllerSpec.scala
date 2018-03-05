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

package controllers

import builders.{AuthBuilder, RequestBuilder, UserBuilder}
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Employment}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.JourneyCacheConstants

import scala.concurrent.Future
import scala.util.Random

class IncomeUpdateCalculatorNewControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar with JourneyCacheConstants {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "processChooseHowToUpdate" must {
    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val sut = createSut
        val result = sut.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.workingHoursPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val sut = createSut
        val result = sut.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "income"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeController.viewIncomeForEdit().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val sut = createSut
        val result = sut.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.howToUpdate.title")
      }
    }
  }

  "workingHoursPage" must {
    "display workingHours page" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        val result = sut.workingHoursPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.workingHours.title")
      }
    }
  }

  "handleWorkingHours" must {
    "redirect the user to workingHours page" when {
      "user selected income calculator" in {
        val sut = createSut
        val result = sut.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "same"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payPeriodPage().url)
      }
    }

    "redirect the user to viewIncomeForEdit page" when {
      "user selected anything apart from income calculator" in {
        val sut = createSut
        val result = sut.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "income"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.calcUnavailablePage().url)
      }
    }

    "redirect user back to how to update page" when {
      "user input has error" in {
        val sut = createSut
        val result = sut.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> ""))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.workingHours.title")
      }
    }
  }

  "payPeriodPage" must {
    "display payPeriod page" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        val result = sut.payPeriodPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payPeriod.title")
      }
    }
  }

  "handlePayPeriod" must {
    "redirect the user to payslipAmountPage page" when {
      "user selected monthly" in {
        val sut = createSut
        when(sut.journeyCacheService.cache(Matchers.eq(UpdateIncome_PayPeriod), Matchers.eq("monthly"))(any())).thenReturn(Future.successful(Map(""->"")))
        val result = sut.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "monthly"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.IncomeUpdateCalculatorController.payslipAmountPage().url)
      }
    }

    "redirect user back to how to payPeriod page" when {
      "user input has error" in {
        val sut = createSut
        val result = sut.handlePayPeriod()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("payPeriod" -> "otherInDays"))
        status(result) mustBe BAD_REQUEST

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payPeriod.title")
      }
    }
  }

  "payslipAmountPage" must {
    "display payslipAmount page" when {
      "journey cache returns employment name, id and payPeriod" in {
        val sut = createSut
        when(sut.journeyCacheService.mandatoryValue(Matchers.eq(UpdateIncome_PayPeriod))(any())).thenReturn(Future.successful(""))
        val result = sut.payslipAmountPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.payslip.title")
      }
    }
  }

  "calcUnavailablePage" must {
    "display calcUnavailable page" when {
      "journey cache returns employment name and id" in {
        val sut = createSut
        val result = sut.calcUnavailablePage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe Messages("tai.unableToCalculate.title")
      }
    }
  }

  private val SampleId = 1
  private val EmployerName = "sample employer"
  private val fakeTaiRoot = TaiRoot(fakeNino.nino, 0, "Mr", "Kkk", None, "Sss", "Kkk Sss", false, Some(false))
  private def fakeNino = new Generator(new Random).nextNino

  private def createSut = new SUT()

  private class SUT extends IncomeUpdateCalculatorNewController {
    override val taiService: TaiService = mock[TaiService]
    override val activityLoggerService: ActivityLoggerService = mock[ActivityLoggerService]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: PartialRetriever = MockPartialRetriever
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val incomeService: IncomeService = mock[IncomeService]

    val ad: Future[Some[Authority]] = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(AuthBuilder.createFakeSessionDataWithPY))
    when(taiService.personDetails(any())(any())).thenReturn(Future.successful(fakeTaiRoot))

    when(journeyCacheService.mandatoryValueAsInt(Matchers.eq(UpdateIncome_IdKey))(any())).thenReturn(Future.successful(SampleId))
    when(journeyCacheService.mandatoryValue(Matchers.eq(UpdateIncome_NameKey))(any())).thenReturn(Future.successful(EmployerName))
  }

}
