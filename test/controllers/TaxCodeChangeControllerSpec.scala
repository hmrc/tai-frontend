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

import builders.{AuthBuilder, RequestBuilder}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{GiftAidPayments, GiftsSharesCharity, TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future
import scala.util.Random


class TaxCodeChangeControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "whatHappensNext" must {
    "show 'What happens next' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT(true)
        val result = SUT.whatHappensNext()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("taxCode.change.whatHappensNext.title"))
      }
    }

    "don't show 'What happens next' page if 'tax code change journey' is toggled off" when {
      "the request has an authorised session" in {
        val SUT = createSUT()
        val result = SUT.whatHappensNext()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("global.error.pageNotFound404.title"))

      }
    }
  }

  "yourTaxFreeAmount" must {
    "show 'Your tax-free amount' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT(true)

        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))

        when(SUT.codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.successful(codingComponents))
        when(SUT.companyCarService.companyCarOnCodingComponents(any(), any())(any())).thenReturn(Future.successful(Nil))
        when(SUT.employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(Map.empty[Int, String]))
        when(SUT.taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))

        val result = SUT.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "don't show 'Your tax-free amount' page if 'tax code change journey' is toggled off" when {
      "the request has an authorised session" in {
        val SUT = createSUT()
        val result = SUT.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("global.error.pageNotFound404.title"))
      }
    }
  }

  "taxCodeComparison" must {
    "show 'Your tax code comparison' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT(true)

        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        when(SUT.taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))
        when(SUT.taxAccountService.scottishBandRates(any(), any(), any())(any())).thenReturn(Future.successful(Map[String, BigDecimal]()))

        val result = SUT.taxCodeComparison()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "don't show 'Your tax code comparison' page if 'tax code change journey' is toggled off" when {
      "the request has an authorised session" in {
        val SUT = createSUT()
        val result = SUT.taxCodeComparison()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("global.error.pageNotFound404.title"))
      }
    }
  }


  private def createSUT(taxCodeChangeJourneyEnabled: Boolean = false) = new SUT(taxCodeChangeJourneyEnabled)

  def generateNino: Nino = new Generator(new Random).nextNino

  val giftAmount = 1000

  val codingComponents = Seq(CodingComponent(GiftAidPayments, None, giftAmount, "GiftAidPayments description"),
    CodingComponent(GiftsSharesCharity, None, giftAmount, "GiftsSharesCharity description"))

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("D0", startDate, startDate.plusDays(1), OtherBasisOfOperation,"Employer 1", false, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)

  private class SUT(taxCodeChangeJourneyEnabled: Boolean) extends TaxCodeChangeController {

    override implicit val partialRetriever: FormPartialRetriever = mock[FormPartialRetriever]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override val personService: PersonService = mock[PersonService]
    override val codingComponentService: CodingComponentService = mock[CodingComponentService]
    override val employmentService: EmploymentService = mock[EmploymentService]
    override val companyCarService: CompanyCarService = mock[CompanyCarService]
    override val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val taxCodeChangeEnabled: Boolean = taxCodeChangeJourneyEnabled

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))
    when(taxCodeChangeService.latestTaxCodeChangeDate(generateNino)).thenReturn(Future.successful(new LocalDate(2018,6,11)))
  }

}
