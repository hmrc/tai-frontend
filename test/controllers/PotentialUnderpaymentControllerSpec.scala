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

package controllers

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import play.api.i18n.{I18nSupport, Messages}
import play.api.test.Helpers._
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, MarriageAllowanceTransferred, TaxAccountSummary}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.AuditConstants
import utils.BaseSpec
import views.html.PotentialUnderpaymentView

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class PotentialUnderpaymentControllerSpec extends BaseSpec with I18nSupport {

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(auditService)
  }

  "potentialUnderpaymentPage method" must {
    "return a clean response" when {
      "supplied with an authorised session" in {
        val res = new SUT().potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        status(res) mustBe OK
      }
      "supplied with an authorised session with no referral headers" in {
        val res = new SUT().potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(res) mustBe OK
      }
    }
    "return the potential underpayment page for current year only" when {
      "processing a TaxAccountSummary with no CY+1 amount" in {
        val sut = new SUT()
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 0)))
        val res = sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        val doc = Jsoup.parse(contentAsString(res))
        doc.title() must include(Messages("tai.iya.tax.you.owe.title"))

      }
    }
    "return the general potential underpayment page covering this and next year" when {
      "processing a TaxAccountSummary with a CY+1 amount" in {
        val sut = new SUT()
        when(taxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(Future.successful(TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 55.55)))
        val res = sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        val doc = Jsoup.parse(contentAsString(res))
        doc.title() must include(Messages("tai.iya.tax.you.owe.title"))

      }
    }
    "raise an in year adjustment audit events" in {
      val sut = new SUT()
      Await
        .result(sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap)), 5 seconds)
      verify(auditService, times(1))
        .createAndSendAuditEvent(meq(AuditConstants.PotentialUnderpaymentInYearAdjustment), any())(any(), any())
    }
    "return the service unavailable error page in response to an internal error" in {
      val sut = new SUT()
      when(taxAccountService.taxAccountSummary(any(), any())(any()))
        .thenReturn(Future.failed(new ForbiddenException("")))
      val res = sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
      status(res) mustBe INTERNAL_SERVER_ERROR
      val doc = Jsoup.parse(contentAsString(res))
      doc.title() must include("Sorry, there is a problem with the service")
    }
  }

  val codingComponentService = mock[CodingComponentService]
  val auditService = mock[AuditService]
  val taxAccountService = mock[TaxAccountService]
  val referralMap = "Referer" -> "http://somelocation/somePageResource"

  private class SUT()
      extends PotentialUnderpaymentController(
        taxAccountService,
        codingComponentService,
        auditService,
        mockAuthJourney,
        FakeValidatePerson,
        mcc,
        inject[PotentialUnderpaymentView],
        inject[ErrorPagesHandler]
      ) {
    when(taxAccountService.taxAccountSummary(any(), any())(any()))
      .thenReturn(Future.successful(TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 55.55)))

    when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(
      Future.successful(
        Seq(
          CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
          CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
        )
      )
    )
  }

}
