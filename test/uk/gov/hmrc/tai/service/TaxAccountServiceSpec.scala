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

package uk.gov.hmrc.tai.service

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.{income, _}
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Random

class TaxAccountServiceSpec extends PlaySpec with MockitoSugar {

  "taxCodeIncomes" must {
    "return seq of tax codes" in {
      val sut = createSut
      when(sut.taxAccountConnector.taxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeIncomes)))

      val result = sut.taxCodeIncomes(generateNino, TaxYear())
      Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(taxCodeIncomes)
    }
  }

  "taxAccountSummary" must {
    "return the tax account summary from the connector" in {
      val sut = createSut
      when(sut.taxAccountConnector.taxAccountSummary(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxAccountSummary)))

      val result = sut.taxAccountSummary(generateNino, TaxYear())
      Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(taxAccountSummary)
    }
  }

  "nonTaxCodeIncomes" must {
    "return Non tax code income" in {
      val sut = createSut
      when(sut.taxAccountConnector.nonTaxCodeIncomes(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(nonTaxCodeIncome)))

      val result = sut.nonTaxCodeIncomes(generateNino, TaxYear())
      Await.result(result, 5 seconds) mustBe TaiSuccessResponseWithPayload(nonTaxCodeIncome)
    }
  }

  "updateEstimatedIncome" must {
    "return success update response" in {
      val sut = createSut
      when(sut.taxAccountConnector.updateEstimatedIncome(any(), any(), any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponse))

      val result = sut.updateEstimatedIncome(generateNino, 100, TaxYear(), 1)
      Await.result(result, 5 seconds) mustBe TaiSuccessResponse
    }

    "return failure update response" in {
      val sut = createSut
      when(sut.taxAccountConnector.updateEstimatedIncome(any(), any(), any(), any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))

      val result = sut.updateEstimatedIncome(generateNino, 100, TaxYear(), 1)
      Await.result(result, 5 seconds) mustBe TaiTaxAccountFailureResponse("Failed")
    }
  }

  "scottishBandRate" must {
    "return a map of tax bands with corresponding rates" when {
      "tai connector returns total tax value with tax bands" in {
        val sut = createSut
        val totalTax = TotalTax(1000,
          List(IncomeCategory(UkDividendsIncomeCategory, 10, 20, 30, List(TaxBand("S", "D0", 0, 0, None, None, 20),
            TaxBand("B", "BR", 10000, 500, Some(5000), Some(20000), 10)))),
          None, None, None)

        when(sut.taxAccountConnector.totalTax(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))
        val result = sut.scottishBandRates(generateNino, TaxYear())
        Await.result(result, 5 seconds) mustBe Map("D0" -> 20, "BR" -> 10)
      }

      "tai connector returns total tax value without tax bands" in {
        val sut = createSut
        val totalTax = TotalTax(1000,
          List(IncomeCategory(UkDividendsIncomeCategory, 10, 20, 30, Nil)),
          None, None, None)

        when(sut.taxAccountConnector.totalTax(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))
        val result = sut.scottishBandRates(generateNino, TaxYear())
        Await.result(result, 5 seconds) mustBe Map()
      }

      "tai connector returns total tax value without income category" in {
        val sut = createSut
        val totalTax = TotalTax(1000, Nil, None, None, None)

        when(sut.taxAccountConnector.totalTax(any(), any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(totalTax)))
        val result = sut.scottishBandRates(generateNino, TaxYear())
        Await.result(result, 5 seconds) mustBe Map()
      }
    }

    "throw an exception" when {
      "when connector returns exception response" in {
        val sut = createSut

        when(sut.taxAccountConnector.totalTax(any(), any())(any())).thenReturn(Future.successful(TaiTaxAccountFailureResponse("Error Message")))
        val exception = the[RuntimeException] thrownBy Await.result(sut.scottishBandRates(generateNino, TaxYear()), 5 seconds)
        exception.getMessage mustBe "could not fetch scottish tax band rates"
      }
    }
  }

  val taxAccountSummary = TaxAccountSummary(111,222, 333.23)

  val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOperation, Live))

  private val nonTaxCodeIncome = NonTaxCodeIncome(Some(income.UntaxedInterest(
    UntaxedInterestIncome, None, 100, "Untaxed Interest", Seq.empty[BankAccount])),
    Seq(OtherNonTaxCodeIncome(Profit, None, 100, "Profit")))

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  def generateNino: Nino = new Generator(new Random).nextNino

  private def createSut = new SUT

  private class SUT extends TaxAccountService {
    override val taxAccountConnector: TaxAccountConnector = mock[TaxAccountConnector]
  }

}
