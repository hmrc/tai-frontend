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
import org.mockito.Matchers
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
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.yourTaxFreeAmount.DescribedYourTaxFreeAmountService
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxFreeInfo
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import uk.gov.hmrc.time.TaxYearResolver

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
        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = controller.whatHappensNext()(request)

        status(result) mustBe OK

        result rendersTheSameViewAs views.html.taxCodeChange.whatHappensNext()
      }
    }
  }

  "yourTaxFreeAmount" must {
    "show 'Your tax-free amount' page with comparison" when {
      "taxFreeAmountComparison is enabled and the request has an authorised session" in {

        val expectedViewModel: YourTaxFreeAmountViewModel =
          YourTaxFreeAmountViewModel(
            Some(TaxFreeInfo("previousTaxDate", 0, 0)),
            TaxFreeInfo("currentTaxDate", 0, 0),
            Seq.empty,
            Seq.empty
          )

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val SUT = createSUT(true)

        when(describedYourTaxFreeAmountService.taxFreeAmountComparison(Matchers.eq(nino))(any(), any()))
          .thenReturn(Future.successful(expectedViewModel))

        val result = SUT.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        result rendersTheSameViewAs views.html.taxCodeChange.yourTaxFreeAmount(expectedViewModel)
      }
    }

    "show 'Your tax-free amount' page without comparison"when {
      "taxFreeAmountComparison is disabled and the request has a authorised session" in {
        val expectedViewModel = YourTaxFreeAmountViewModel(None, TaxFreeInfo("currentTaxDate", 0, 0), Seq.empty, Seq.empty)

        val SUT = createSUT(false)

        when(describedYourTaxFreeAmountService.taxFreeAmount(Matchers.eq(nino))(any(), any()))
          .thenReturn(Future.successful(expectedViewModel))

        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = SUT.yourTaxFreeAmount()(request)

        status(result) mustBe OK
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

  val nino: Nino = new Generator(new Random).nextNino

  val giftAmount = 1000

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("D0", startDate, startDate.plusDays(1), OtherBasisOfOperation, "Employer 1", false, Some("1234"), true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYearResolver.endOfCurrentTaxYear)

  val personService: PersonService = mock[PersonService]
  val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]
  val yourTaxFreeAmountService: YourTaxFreeAmountService = mock[YourTaxFreeAmountService]
  val describedYourTaxFreeAmountService: DescribedYourTaxFreeAmountService = mock[DescribedYourTaxFreeAmountService]

  private def createSUT(comparisonEnabled: Boolean = false) = new SUT(comparisonEnabled)

  private class SUT(comparisonEnabled: Boolean) extends TaxCodeChangeController(
    personService,
    taxCodeChangeService,
    taxAccountService,
    describedYourTaxFreeAmountService,
    yourTaxFreeAmountService,
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
