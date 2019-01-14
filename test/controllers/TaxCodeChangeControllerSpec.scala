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
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{GiftAidPayments, GiftsSharesCharity, TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
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
        val result = controller.whatHappensNext()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("taxCode.change.whatHappensNext.title"))
      }
    }
  }

  "yourTaxFreeAmount" must {
    "show 'Your tax-free amount' page" when {
      "the request has an authorised session" in {
        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))

        when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(Future.successful(codingComponents))
        when(companyCarService.companyCarOnCodingComponents(any(), any())(any())).thenReturn(Future.successful(Nil))
        when(employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(Map.empty[Int, String]))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))

        val result = controller.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }
  }

  "taxCodeComparison" must {
    "show 'Your tax code comparison' page" when {
      "the request has an authorised session" in {
        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))
        when(taxAccountService.scottishBandRates(any(), any(), any())(any())).thenReturn(Future.successful(Map[String, BigDecimal]()))

        val result = controller.taxCodeComparison()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }
  }

  private def controller = new SUT()

  def generateNino: Nino = new Generator(new Random).nextNino

  val giftAmount = 1000

  val codingComponents = Seq(CodingComponent(GiftAidPayments, None, giftAmount, "GiftAidPayments description"),
    CodingComponent(GiftsSharesCharity, None, giftAmount, "GiftsSharesCharity description"))

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("D0", startDate, startDate.plusDays(1), OtherBasisOfOperation, "Employer 1", false, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)

  val personService: PersonService = mock[PersonService]
  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val codingComponentService = mock[CodingComponentService]
  val companyCarService = mock[CompanyCarService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]

  private class SUT() extends TaxCodeChangeController(
    personService,
    codingComponentService,
    employmentService,
    companyCarService,
    taxCodeChangeService,
    taxAccountService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))
    when(taxCodeChangeService.latestTaxCodeChangeDate(generateNino)).thenReturn(Future.successful(new LocalDate(2018, 6, 11)))
  }

}
