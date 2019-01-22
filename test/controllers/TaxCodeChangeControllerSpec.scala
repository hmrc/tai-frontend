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
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future
import scala.util.Random
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{CodingComponentsWithCarBenefits, TaxFreeInfo, YourTaxFreeAmount}
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountSummaryViewModel
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import uk.gov.hmrc.urls.Link


class TaxCodeChangeControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with ControllerViewTestHelper {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "whatHappensNext" must {
    "show 'What happens next' page" when {
      "the request has an authorised session" in {
        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = controller.whatHappensNext()(request)

        status(result) mustBe OK

        result rendersTheSameViewAs views.html.taxCodeChange.whatHappensNext()
      }
    }
  }

  "yourTaxFreeAmount" must {
    "show 'Your tax-free amount' page with previous benefits" when {
      "taxFreeAmountComparison is enabled and the request has an authorised session" in {
        val SUT = createSUT(true, companyCarService)

        val previousCodingComponents = Seq(codingComponent1)
        val currentCodingComponents = Seq(codingComponent2)
        val taxFreeAmountComparison = TaxFreeAmountComparison(previousCodingComponents, currentCodingComponents)
        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        val employmentMap = Map.empty[Int, String]
        val companyCar = Seq.empty[CompanyCarBenefit]

        when(codingComponentService.taxFreeAmountComparison(Matchers.eq(nino))(any())).thenReturn(Future.successful(taxFreeAmountComparison))
        when(companyCarService.companyCarOnCodingComponents(Matchers.eq(nino), Matchers.eq(previousCodingComponents))(any()))
          .thenReturn(Future.successful(companyCar))
        when(companyCarService.companyCarOnCodingComponents(Matchers.eq(nino), Matchers.eq(currentCodingComponents))(any()))
          .thenReturn(Future.successful(companyCar))

        when(employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(employmentMap))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))

        val result = SUT.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        verify(companyCarService, times(1)).companyCarOnCodingComponents(Matchers.eq(nino), Matchers.eq(currentCodingComponents))(any())
        verify(companyCarService, times(1)).companyCarOnCodingComponents(Matchers.eq(nino), Matchers.eq(previousCodingComponents))(any())
      }
    }

    "show 'Your tax-free amount' page without previous benefits"when {
      "taxFreeAmountComparison is disabled and the request has a authorised session" in {
        val companyCarService = mock[CompanyCarService]

        val SUT = createSUT(false, companyCarService)

        val currentCodingComponents = Seq(codingComponent2)

        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        val employmentMap = Map.empty[Int, String]
        val companyCar = Seq.empty[CompanyCarBenefit]

        when(codingComponentService.taxFreeAmountComponents(Matchers.eq(nino), Matchers.eq(TaxYear()))(any()))
          .thenReturn(Future.successful(currentCodingComponents))

        when(companyCarService.companyCarOnCodingComponents(Matchers.eq(nino), Matchers.eq(currentCodingComponents))(any()))
          .thenReturn(Future.successful(companyCar))

        when(employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(employmentMap))
        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = SUT.yourTaxFreeAmount()(request)

        status(result) mustBe OK

        verify(companyCarService, times(1)).companyCarOnCodingComponents(Matchers.eq(nino), Matchers.eq(currentCodingComponents))(any())
      }
    }
  }

  "taxCodeComparison" must {
    "show 'Your tax code comparison' page" when {
      "the request has an authorised session" in {
        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        val scottishRates = Map.empty[String, BigDecimal]

        when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))
        when(taxAccountService.scottishBandRates(any(), any(), any())(any())).thenReturn(Future.successful(Map[String, BigDecimal]()))

        val result = controller.taxCodeComparison()(request)

        val taxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, scottishRates)

        status(result) mustBe OK
        result rendersTheSameViewAs views.html.taxCodeChange.taxCodeComparison(taxCodeChangeViewModel)
      }
    }
  }

  private def controller = createSUT()

  trait YourTaxFreeAmountMock {
    this: YourTaxFreeAmount =>
    override def buildTaxFreeAmount(unused1: Option[CodingComponentsWithCarBenefits],
                                    unused2: CodingComponentsWithCarBenefits,
                                    unused3: Map[Int, String])
                                   (implicit messages: Messages): YourTaxFreeAmountViewModel = {
      expectedViewModel
    }
  }

  val expectedViewModel: YourTaxFreeAmountViewModel =
    YourTaxFreeAmountViewModel(
      Some(TaxFreeInfo("previousTaxDate", 0, 0)),
      TaxFreeInfo("currentTaxDate", 0, 0),
      Seq.empty,
      Seq.empty)

  val nino: Nino = new Generator(new Random).nextNino

  val giftAmount = 1000

  private val codingComponent1 = CodingComponent(GiftAidPayments, None, giftAmount, "GiftAidPayments description")
  private val codingComponent2 = CodingComponent(GiftsSharesCharity, None, giftAmount, "GiftsSharesCharity description")
  val codingComponents = Seq(codingComponent1, codingComponent2)

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("D0", startDate, startDate.plusDays(1), OtherBasisOfOperation, "Employer 1", false, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)

  val personService: PersonService = mock[PersonService]
  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val codingComponentService = mock[CodingComponentService]
  val companyCarService = mock[CompanyCarService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]

  private def createSUT(comparisonEnabled: Boolean = false, companyCarMockService: CompanyCarService = companyCarService) = new SUT(comparisonEnabled, companyCarMockService)

  private class SUT(comparisonEnabled: Boolean, companyCarMockService: CompanyCarService) extends TaxCodeChangeController(
    personService,
    codingComponentService,
    employmentService,
    companyCarMockService,
    taxCodeChangeService,
    taxAccountService,
    mock[AuditConnector],
    mock[DelegationConnector],
    mock[AuthConnector],
    mock[FormPartialRetriever],
    MockTemplateRenderer
  ) {

    override val taxFreeAmountComparisonEnabled: Boolean = comparisonEnabled

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(nino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
    when(taxCodeChangeService.latestTaxCodeChangeDate(nino)).thenReturn(Future.successful(new LocalDate(2018, 6, 11)))
  }
}
