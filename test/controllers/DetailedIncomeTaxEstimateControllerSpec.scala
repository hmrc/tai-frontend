/*
 * Copyright 2025 HM Revenue & Customs
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
import cats.data.EitherT
import cats.instances.future.*
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{BadRequestException, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.domain.{IabdDetails, TaxAccountSummary}
import uk.gov.hmrc.tai.model.domain.income.{NonTaxCodeIncome, TaxCodeIncome}
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, TaxAdjustment, TotalTax}
import uk.gov.hmrc.tai.service.{CodingComponentService, IabdService, PersonService, TaxAccountService}
import utils.BaseSpec
import views.html.estimatedIncomeTax.DetailedIncomeTaxEstimateView

import java.time.LocalDate
import scala.concurrent.Future

class DetailedIncomeTaxEstimateControllerSpec extends BaseSpec {

  val mockPersonService: PersonService                   = mock[PersonService]
  val mockCodingComponentService: CodingComponentService = mock[CodingComponentService]
  val mockTaxAccountService: TaxAccountService           = mock[TaxAccountService]
  val mockIabdService: IabdService                       = mock[IabdService]

  def sut =
    new DetailedIncomeTaxEstimateController(
      mockTaxAccountService,
      mockCodingComponentService,
      mockIabdService,
      mockAuthJourney,
      mcc,
      inject[DetailedIncomeTaxEstimateView],
      inject[ErrorPagesHandler]
    )

  def taxAccountSummary(
    totalEstimatedTax: BigDecimal = BigDecimal(0),
    taxFreeAmount: BigDecimal = BigDecimal(0),
    totalInYearAdjustmentIntoCY: BigDecimal = BigDecimal(0),
    totalInYearAdjustment: BigDecimal = BigDecimal(0),
    totalInYearAdjustmentIntoCYPlusOne: BigDecimal = BigDecimal(0),
    totalEstimatedIncome: BigDecimal = BigDecimal(0),
    taxFreeAllowance: BigDecimal = BigDecimal(0),
    date: Option[LocalDate] = None
  ) =
    TaxAccountSummary(
      totalEstimatedTax,
      taxFreeAmount,
      totalInYearAdjustmentIntoCY,
      totalInYearAdjustment,
      totalInYearAdjustmentIntoCYPlusOne,
      totalEstimatedIncome,
      taxFreeAllowance,
      date
    )

  def totalTax(
    amount: BigDecimal = BigDecimal(0),
    incomeCategories: Seq[IncomeCategory] = Seq.empty,
    reliefsGivingBackTax: Option[TaxAdjustment] = None,
    otherTaxDue: Option[TaxAdjustment] = None,
    alreadyTaxedAtSource: Option[TaxAdjustment] = None,
    taxOnOtherIncome: Option[BigDecimal] = None,
    taxReliefComponent: Option[TaxAdjustment] = None
  ): TotalTax =
    TotalTax(
      amount,
      incomeCategories,
      reliefsGivingBackTax,
      otherTaxDue,
      alreadyTaxedAtSource,
      taxOnOtherIncome,
      taxReliefComponent
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockTaxAccountService)
  }

  "Detailed Income Tax Estimate Controller" must {
    "return OK when responses are " when {
      "there are bands present and no iabd estimate" in {
        when(mockTaxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(TotalTax(0, Seq.empty, None, None, None)))
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(TaxAccountSummary(0, 0, 0, 0, 0)))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(NonTaxCodeIncome(None, Seq.empty)))
        when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
          .thenReturn(Future.successful(Seq.empty))
        when(mockIabdService.getIabds(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty))

        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val html   = Jsoup.parse(contentAsString(result))
        Option(html.getElementById("new-income-estimate")).map(_.text()) mustBe None
      }

      "iabd estimate income is used instead of tax code income" in {
        when(mockTaxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(TotalTax(0, Seq.empty, None, None, None)))
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty[TaxCodeIncome])))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(TaxAccountSummary(0, 0, 0, 0, 0)))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(NonTaxCodeIncome(None, Seq.empty)))
        when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
          .thenReturn(Future.successful(Seq.empty))
        when(mockIabdService.getIabds(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              Seq(
                IabdDetails(Some(233), None, Some(27), None, Some(LocalDate.now), Some(BigDecimal(1234.56)))
              )
            )
          )

        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe OK
        val html   = Jsoup.parse(contentAsString(result))
        Option(html.getElementById("new-income-estimate")).map(_.text()) mustBe Some(
          "We’re updating your income tax estimate. It will be ready tomorrow."
        )
      }
    }

    "return Internal server error" when {
      "fetch total tax details fails" in {
        when(mockTaxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("testFailure")))
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(NonTaxCodeIncome(None, Seq.empty)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
        )
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch tax code incomes fails" in {
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Left("testFailure")))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(NonTaxCodeIncome(None, Seq.empty)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockTaxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(totalTax()))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
        )

        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch tax account summary fails" in {
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.failed(new BadRequestException("testFailure")))
        when(mockTaxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(totalTax()))
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
        )

        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch of non-tax code incomes fails" in {
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.failed(new BadRequestException("testFailure")))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockTaxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(totalTax()))
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
        )

        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "fetch of tax free amount components" in {
        when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
          .thenReturn(Future.failed(new Error))
        when(mockTaxAccountService.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(totalTax()))
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(Right(Seq.empty)))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any()))
          .thenReturn(Future.successful(NonTaxCodeIncome(None, Seq.empty)))
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              taxAccountSummary()
            )
          )
        when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
        )
        val result = sut.taxExplanationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
