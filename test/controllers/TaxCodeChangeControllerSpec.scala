/*
 * Copyright 2020 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.yourTaxFreeAmount.{DescribedYourTaxFreeAmountService, TaxCodeChangeReasonsService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxFreeInfo
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodeChangeViewModel, YourTaxFreeAmountViewModel}
import utils.BaseSpec

import scala.concurrent.Future

class TaxCodeChangeControllerSpec extends BaseSpec with ControllerViewTestHelper {

  "whatHappensNext" must {
    "show 'What happens next' page" when {
      "the request has an authorised session" in {
        implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

        val result = createController.whatHappensNext()(request)

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

        when(describedYourTaxFreeAmountService.taxFreeAmountComparison(Matchers.eq(FakeAuthAction.nino))(any(), any()))
          .thenReturn(Future.successful(expectedViewModel))

        val result = createController.yourTaxFreeAmount()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        result rendersTheSameViewAs views.html.taxCodeChange.yourTaxFreeAmount(expectedViewModel)
      }
    }
  }

  "taxCodeComparison" must {
    "show 'Your tax code comparison' page" in {
      implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

      val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))
      val scottishRates = Map.empty[String, BigDecimal]

      when(taxAccountService.scottishBandRates(any(), any(), any())(any()))
        .thenReturn(Future.successful(Map[String, BigDecimal]()))
      when(taxAccountService.totalTax(Matchers.eq(FakeAuthAction.nino), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(TotalTax(0, Seq.empty, None, None, None))))
      when(taxCodeChangeService.taxCodeChange(any())(any())).thenReturn(Future.successful(taxCodeChange))
      when(yourTaxFreeAmountService.taxFreeAmountComparison(any())(any(), any()))
        .thenReturn(Future.successful(mock[YourTaxFreeAmountComparison]))

      val reasons = Seq("a reason")
      when(taxCodeChangeReasonsService.combineTaxCodeChangeReasons(any(), any(), Matchers.eq(taxCodeChange))(any()))
        .thenReturn(reasons)
      when(taxCodeChangeReasonsService.isAGenericReason(Matchers.eq(reasons))(any())).thenReturn(false)

      val result = createController.taxCodeComparison()(request)

      val expectedViewModel = TaxCodeChangeViewModel(taxCodeChange, scottishRates, reasons, false)

      status(result) mustBe OK
      result rendersTheSameViewAs views.html.taxCodeChange.taxCodeComparison(expectedViewModel, appConfig)
    }
  }

  val giftAmount = 1000

  val startDate = TaxYear().start

  val taxCodeRecord1 = TaxCodeRecord(
    "D0",
    startDate,
    startDate.plusDays(1),
    OtherBasisOfOperation,
    "Employer 1",
    false,
    Some("1234"),
    true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYear().end)

  val personService = mock[PersonService]
  val taxCodeChangeService = mock[TaxCodeChangeService]
  val taxAccountService = mock[TaxAccountService]
  val describedYourTaxFreeAmountService = mock[DescribedYourTaxFreeAmountService]
  val yourTaxFreeAmountService = mock[YourTaxFreeAmountService]
  val taxCodeChangeReasonsService = mock[TaxCodeChangeReasonsService]

  private def createController() = new TaxCodeChangeTestController

  private class TaxCodeChangeTestController
      extends TaxCodeChangeController(
        taxCodeChangeService,
        taxAccountService,
        describedYourTaxFreeAmountService,
        FakeAuthAction,
        FakeValidatePerson,
        yourTaxFreeAmountService,
        taxCodeChangeReasonsService,
        appConfig,
        mcc,
        partialRetriever,
        templateRenderer
      ) {

    when(taxCodeChangeService.latestTaxCodeChangeDate(meq(nino))(any()))
      .thenReturn(Future.successful(new LocalDate(2018, 6, 11)))
  }

}
