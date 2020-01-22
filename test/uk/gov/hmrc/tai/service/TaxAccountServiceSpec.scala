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

package uk.gov.hmrc.tai.service

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.{income, _}
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

class TaxAccountServiceSpec extends PlaySpec with MockitoSugar {

  "taxCodeIncomes" must {
    "return seq of tax codes" in {
      val testService = createSut
      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))

      val result = testService.taxCodeIncomes(generateNino, TaxYear())
      Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(taxCodeIncomes)
    }
  }

  "taxCodeIncomeForSpecificEmployment" must {
    "return an income corresponding to a specific employment id" in {
      val testService = createSut

      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))

      val result = testService.taxCodeIncomeForEmployment(generateNino, TaxYear(), 1)

      val expected = Some(taxCodeIncome1)

      Await.result(result, 5 seconds) mustBe expected
    }

    "return None when an income id is not present" in {
      val testService = createSut

      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))

      val result = testService.taxCodeIncomeForEmployment(generateNino, TaxYear(), 99)

      Await.result(result, 5 seconds) mustBe None
    }

    "throw an exception when the TaxCodeIncome cannot be found" in {
      val testService = createSut

      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("error")))

      val result = testService.taxCodeIncomeForEmployment(generateNino, TaxYear(), 99)

      val expected = Some(taxCodeIncome1)

      a[RuntimeException] mustBe thrownBy(Await.result(result, 5 seconds))
    }
  }

  "taxAccountSummary" must {
    "return the tax account summary from the connector" in {
      val sut = createSut
      when(taxAccountConnector.taxAccountSummary(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxAccountSummary)))

      val result = sut.taxAccountSummary(generateNino, TaxYear())
      Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(taxAccountSummary)
    }
  }

  "nonTaxCodeIncomes" must {
    "return Non tax code income" in {
      val sut = createSut
      when(taxAccountConnector.nonTaxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(nonTaxCodeIncome)))

      val result = sut.nonTaxCodeIncomes(generateNino, TaxYear())
      Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(nonTaxCodeIncome)
    }
  }

  "updateEstimatedIncome" must {
    "return success update response" in {
      val sut = createSut
      when(taxAccountConnector.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponse))

      val result = sut.updateEstimatedIncome(generateNino, 100, TaxYear(), 1)
      Await.result(result, 5 seconds) mustBe TaiSuccessResponse
    }

    "return failure update response" in {
      val sut = createSut
      when(taxAccountConnector.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))

      val result = sut.updateEstimatedIncome(generateNino, 100, TaxYear(), 1)
      Await.result(result, 5 seconds) mustBe TaiTaxAccountFailureResponse("Failed")
    }
  }

  "totalTax" must {
    "return total tax" in {
      val sut = createSut
      val totalTax = TotalTax(1000, Nil, None, None, None)
      when(taxAccountConnector.totalTax(any(), any())(any()))
        .thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))

      val result = sut.totalTax(generateNino, TaxYear())
      Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(totalTax)
    }
  }

  "scottishBandRate" must {
    "return a map of tax bands with corresponding rates" when {
      "tai connector returns total tax value with tax bands" in {
        val sut = createSut
        val totalTax = TotalTax(
          1000,
          List(
            IncomeCategory(
              UkDividendsIncomeCategory,
              10,
              20,
              30,
              List(
                TaxBand("D0", "", 0, 0, None, None, 20),
                TaxBand("1150L", "1150L", 10000, 500, Some(5000), Some(20000), 10)))),
          None,
          None,
          None
        )

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))

        val result = sut.scottishBandRates(generateNino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map("D0" -> 20, "1150L" -> 10)
      }
    }

    "throw an empty map" when {
      "connector returns exception response" in {
        val sut = createSut

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(TaiTaxAccountFailureResponse("Error Message")))
        val result = sut.scottishBandRates(generateNino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map()
      }

      "none of the tax code is scottish" in {
        val sut = createSut

        val result = sut.scottishBandRates(generateNino, TaxYear(), taxCodeIncomes.map(_.taxCode))
        Await.result(result, 5 seconds) mustBe Map()
      }

      "tai connector returns total tax value without tax bands" in {
        val sut = createSut
        val totalTax =
          TotalTax(1000, List(IncomeCategory(UkDividendsIncomeCategory, 10, 20, 30, Nil)), None, None, None)

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))
        val result = sut.scottishBandRates(generateNino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map()
      }

      "tai connector returns total tax value without income category" in {
        val sut = createSut
        val totalTax = TotalTax(1000, Nil, None, None, None)

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))
        val result = sut.scottishBandRates(generateNino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map()
      }
    }
  }

  val taxAccountSummary = TaxAccountSummary(111, 222, 333.23, 444.44, 111.11)

  private val taxCodeIncome1 =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live)
  val taxCodeIncomes = Seq(
    taxCodeIncome1,
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live))

  val taxCodes = Seq("SD0", "1150L")

  private val nonTaxCodeIncome = NonTaxCodeIncome(
    Some(income.UntaxedInterest(UntaxedInterestIncome, None, 100, "Untaxed Interest", Seq.empty[BankAccount])),
    Seq(OtherNonTaxCodeIncome(Profit, None, 100, "Profit"))
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  private def createSut = new SUT

  val taxAccountConnector = mock[TaxAccountConnector]

  private class SUT
      extends TaxAccountService(
        taxAccountConnector
      )

}
