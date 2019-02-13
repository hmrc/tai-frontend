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

package controllers

import builders.{AuthBuilder, RequestBuilder}
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{EstimatedTaxYouOweThisYear, MarriageAllowanceTransferred, TaxAccountSummary}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.AuditConstants

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PotentialUnderpaymentControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with AuditConstants
  with I18nSupport
  with BeforeAndAfterEach {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeEach: Unit = {
    Mockito.reset(auditService)
  }

  val nino = new Generator().nextNino

  "potentialUnderpaymentPage method" must {
    "return a clean response" when {
      "supplied with an authorised session" in {
        val res = new SUT().potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        status(res) mustBe OK
      }
    }
    "return the potentional underpayment page for current year only" when {
      "processing a TaxAccountSummary with no CY+1 amount" in {
        val sut = new SUT()
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](
            TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 0)
          ))
        )
        val res = sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        val doc = Jsoup.parse(contentAsString(res))
        doc.title() must include(Messages("tai.iya.tax.you.owe.title"))

      }
    }
    "return the general potentional underpayment page covering this and next year" when {
      "processing a TaxAccountSummary with a CY+1 amount" in {
        val sut = new SUT()
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](
            TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 55.55)
          ))
        )
        val res = sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        val doc = Jsoup.parse(contentAsString(res))
        doc.title() must include(Messages("tai.iya.tax.you.owe.title"))

      }
    }
    "raise an in year adjustment audit events" in {
      val sut = new SUT()
      Await.result(sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap)), 5 seconds)
      verify(auditService, times(1)).createAndSendAuditEvent(Matchers.eq(PotentialUnderpayment_InYearAdjustment), any())(any(), any())
    }
    "return the service unavailable error page in response to an internal error" in {
      val sut = new SUT()
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(Future.failed(new ForbiddenException("")))
      val res = sut.potentialUnderpaymentPage()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
      status(res) mustBe INTERNAL_SERVER_ERROR
      val doc = Jsoup.parse(contentAsString(res))
      doc.title() must include("Sorry, we are experiencing technical difficulties - 500")
    }
  }

  val personService: PersonService = mock[PersonService]
  val codingComponentService = mock[CodingComponentService]
  val auditService = mock[AuditService]
  val taxAccountService = mock[TaxAccountService]
  val referralMap = Map("Referer" ->"http://somelocation/somePageResource")

  private class SUT() extends PotentialUnderpaymentController(
    taxAccountService,
    codingComponentService,
    auditService,
    personService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData(nino))
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))

    when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
      Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](
        TaxAccountSummary(11.11, 22.22, 33.33, 44.44, 55.55)
      ))
    )

    when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(
      Future.successful(Seq(
        CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
        CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
      ))
    )
  }

}
