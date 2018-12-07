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
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.util.YourTaxFreeAmount
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountSummaryViewModel
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link

import scala.concurrent.Future
import scala.util.Random


class TaxCodeChangeControllerSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with ControllerViewTestHelper {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "whatHappensNext" must {
    "show 'What happens next' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT(true)

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = SUT.whatHappensNext()(request)
        status(result) mustBe OK

        result rendersTheSameViewAs views.html.taxCodeChange.whatHappensNext()
      }
    }

    "don't show 'What happens next' page if 'tax code change journey' is toggled off" when {
      "the request has an authorised session" in {
        val SUT = createSUT()

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = SUT.whatHappensNext()(request)

        status(result) mustBe NOT_FOUND

        result rendersTheSameViewAs views.html.error_template_noauth(
          Messages("global.error.pageNotFound404.title"),
          Messages("tai.errorMessage.heading"),
          Messages("tai.errorMessage.frontend404", Link.toInternalPage(
            url = routes.TaxAccountSummaryController.onPageLoad().url,
            value = Some(Messages("tai.errorMessage.startAgain"))
          ).toHtml)
        )
      }
    }
  }

  "yourTaxFreeAmount" must {
    "show 'Your tax-free amount' page" when {
      "the request has an authorised session" in {
        val SUT = createSUT(true)

        val taxFreeAmountComparison = TaxFreeAmountComparison(Seq(codingComponent1), Seq(codingComponent2))
        val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
        val employmentMap = Map.empty[Int, String]
        val companyCar = Seq.empty[CompanyCarBenefit]

        when(SUT.codingComponentService.taxFreeAmountComparison(any())(any())).thenReturn(Future.successful(taxFreeAmountComparison))
        when(SUT.companyCarService.companyCarOnCodingComponents(any(), any())(any())).thenReturn(Future.successful(companyCar))
        when(SUT.employmentService.employmentNames(any(), any())(any())).thenReturn(Future.successful(employmentMap))
        when(SUT.taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val expectedViewModel: YourTaxFreeAmountViewModel =
          YourTaxFreeAmountViewModel("blah", "annualTaxFreeAmount", TaxFreeAmountSummaryViewModel(Seq.empty))

        val result = SUT.yourTaxFreeAmount()(request)

        status(result) mustBe OK
        result rendersTheSameViewAs views.html.taxCodeChange.yourTaxFreeAmount(expectedViewModel, expectedViewModel)
      }
    }

    "don't show 'Your tax-free amount' page if 'tax code change journey' is toggled off" when {
      "the request has an authorised session" in {
        val SUT = createSUT()
        val result = SUT.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe NOT_FOUND

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
        val scottishRates = Map.empty[String, BigDecimal]

        when(SUT.taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))
        when(SUT.taxAccountService.scottishBandRates(any(), any(), any())(any())).thenReturn(Future.successful(scottishRates))

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = SUT.taxCodeComparison()(request)

        val taxCodeChangeViewModel = TaxCodeChangeViewModel(taxCodeChange, scottishRates)

        status(result) mustBe OK
        result rendersTheSameViewAs views.html.taxCodeChange.taxCodeComparison(taxCodeChangeViewModel)
      }
    }

    "don't show 'Your tax code comparison' page if 'tax code change journey' is toggled off" when {
      "the request has an authorised session" in {
        val SUT = createSUT()
        val result = SUT.taxCodeComparison()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe NOT_FOUND

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(messagesApi("global.error.pageNotFound404.title"))
      }
    }
  }


  private def createSUT(taxCodeChangeJourneyEnabled: Boolean = false, comparisonEnabled: Boolean = false) = new SUT(taxCodeChangeJourneyEnabled, comparisonEnabled)

  def generateNino: Nino = new Generator(new Random).nextNino

  val giftAmount = 1000

  private val codingComponent1 = CodingComponent(GiftAidPayments, None, giftAmount, "GiftAidPayments description")
  private val codingComponent2 = CodingComponent(GiftsSharesCharity, None, giftAmount, "GiftsSharesCharity description")
  val codingComponents = Seq(codingComponent1, codingComponent2)

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("D0", startDate, startDate.plusDays(1), OtherBasisOfOperation, "Employer 1", false, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)

  private class SUT(taxCodeChangeJourneyEnabled: Boolean, comparisonEnabled: Boolean)
    extends TaxCodeChangeController
      with YourTaxFreeAmountMock {

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
    override val taxFreeAmountComparisonEnabled: Boolean = comparisonEnabled

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val ad: Future[Some[Authority]] = Future.successful(Some(AuthBuilder.createFakeAuthority(generateNino.toString())))
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)
    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(generateNino)))
    when(taxCodeChangeService.latestTaxCodeChangeDate(generateNino)).thenReturn(Future.successful(new LocalDate(2018, 6, 11)))
  }

  trait YourTaxFreeAmountMock {
    this: YourTaxFreeAmount =>
    override def buildTaxFreeAmount(recentTaxCodeChangeDate: LocalDate,
                                    codingComponents: Seq[CodingComponent],
                                    employmentNames: Map[Int, String],
                                    companyCarBenefits: Seq[CompanyCarBenefit])
                                   (implicit messages: Messages): YourTaxFreeAmountViewModel = {
      YourTaxFreeAmountViewModel("blah", "annualTaxFreeAmount", TaxFreeAmountSummaryViewModel(Seq.empty))
    }
  }

}
