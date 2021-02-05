/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service.yourTaxFreeAmount

import builders.RequestBuilder
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.model.{CodingComponentPair, CodingComponentPairModel, TaxYear}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{EmploymentService, TaxAccountService, YourTaxFreeAmountComparison, YourTaxFreeAmountService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount._
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class DescribedYourTaxFreeAmountServiceSpec extends BaseSpec {

  "taxFreeAmountComparison" must {
    "returns a YourTaxFreeAmountViewModel with the described comparison for previous and current" in {
      val yourTaxFreeAmountComparison = YourTaxFreeAmountComparison(
        previousTaxFreeInfo,
        currentTaxFreeInfo,
        AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
      )

      when(yourTaxFreeAmountService.taxFreeAmountComparison(Matchers.eq(nino))(any(), any()))
        .thenReturn(Future.successful(yourTaxFreeAmountComparison))
      when(employmentService.employmentNames(Matchers.eq(nino), Matchers.eq(TaxYear()))(any()))
        .thenReturn(Future.successful(Map.empty[Int, String]))
      when(companyCarService.companyCars(Matchers.eq(nino))(any()))
        .thenReturn(Future.successful(Seq.empty))
      when(taxAccountService.totalTax(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))

      val expectedModel: YourTaxFreeAmountViewModel =
        YourTaxFreeAmountViewModel(
          previousTaxFreeInfo,
          currentTaxFreeInfo,
          Seq.empty,
          Seq.empty
        )

      val service = createTestService
      implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")
      val result = service.taxFreeAmountComparison(nino)

      Await.result(result, 5.seconds) mustBe expectedModel
    }

    "returns a translates the coding component pair to a described coding component pair for the view model" in {
      val yourTaxFreeAmountComparison = YourTaxFreeAmountComparison(
        None,
        currentTaxFreeInfo,
        AllowancesAndDeductionPairs(Seq(allowancePair), Seq(deductionPair))
      )

      when(yourTaxFreeAmountService.taxFreeAmountComparison(Matchers.eq(nino))(any(), any()))
        .thenReturn(Future.successful(yourTaxFreeAmountComparison))
      when(employmentService.employmentNames(Matchers.eq(nino), Matchers.eq(TaxYear()))(any()))
        .thenReturn(Future.successful(Map.empty[Int, String]))
      when(companyCarService.companyCars(Matchers.eq(nino))(any()))
        .thenReturn(Future.successful(Seq.empty))
      when(taxAccountService.totalTax(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))

      val expectedModel: YourTaxFreeAmountViewModel =
        YourTaxFreeAmountViewModel(
          None,
          currentTaxFreeInfo,
          Seq(describedAllowancePair),
          Seq(describedDeductionPair)
        )

      val service = createTestService
      implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")
      val result = service.taxFreeAmountComparison(nino)

      Await.result(result, 5.seconds) mustBe expectedModel
    }

    "throw an exception when unable to retrieve total tax details" in {
      val yourTaxFreeAmountComparison = YourTaxFreeAmountComparison(
        None,
        currentTaxFreeInfo,
        AllowancesAndDeductionPairs(Seq(allowancePair), Seq(deductionPair))
      )

      when(yourTaxFreeAmountService.taxFreeAmountComparison(Matchers.eq(nino))(any(), any()))
        .thenReturn(Future.successful(yourTaxFreeAmountComparison))
      when(employmentService.employmentNames(Matchers.eq(nino), Matchers.eq(TaxYear()))(any()))
        .thenReturn(Future.successful(Map.empty[Int, String]))
      when(companyCarService.companyCars(Matchers.eq(nino))(any()))
        .thenReturn(Future.successful(Seq.empty))
      when(taxAccountService.totalTax(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("error")))

      val service = createTestService
      implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")
      val result = service.taxFreeAmountComparison(nino)

      the[RuntimeException] thrownBy Await
        .result(result, 5.seconds) must have message "Failed to fetch total tax details"
    }
  }

  private val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
  private val incomeCatergories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
  private val totalTax: TotalTax = TotalTax(1000, Seq(incomeCatergories), None, None, None)

  private def createTestService = new TestService

  private val yourTaxFreeAmountService: YourTaxFreeAmountService = mock[YourTaxFreeAmountService]
  private val companyCarService: CompanyCarService = mock[CompanyCarService]
  private val employmentService: EmploymentService = mock[EmploymentService]
  private val taxAccountService = mock[TaxAccountService]

  private val deductionPair = CodingComponentPair(CarBenefit, Some(1), Some(1000), Some(1000), None)
  private val describedDeductionPair = CodingComponentPairModel("Car benefit", 1000, 1000)
  private val allowancePair = CodingComponentPair(GiftAidPayments, None, None, Some(3000), None)
  private val describedAllowancePair = CodingComponentPairModel("Gift Aid Payments", 0, 3000)

  private val previousTaxFreeInfo = Some(TaxFreeInfo("Previous", 1000, 1000))
  private val currentTaxFreeInfo = TaxFreeInfo("Current", 100, 100)

  private class TestService
      extends DescribedYourTaxFreeAmountService(
        yourTaxFreeAmountService: YourTaxFreeAmountService,
        companyCarService: CompanyCarService,
        employmentService: EmploymentService,
        taxAccountService: TaxAccountService
      )
}
