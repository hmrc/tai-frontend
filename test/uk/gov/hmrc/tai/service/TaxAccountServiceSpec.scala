/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.EitherT
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{InternalServerException, UnauthorizedException}
import uk.gov.hmrc.tai.connectors.TaxAccountConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax._
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class TaxAccountServiceSpec extends BaseSpec {

  "taxCodeIncomes" must {
    "return seq of tax codes" in {
      val testService = createSut
      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))

      val result = testService.taxCodeIncomes(nino, TaxYear())
      Await.result(result, 5 seconds) mustBe Right(taxCodeIncomes)
    }
  }

  "taxCodeIncomeForSpecificEmployment" must {
    "return an income corresponding to a specific employment id" in {
      val testService = createSut

      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))

      val result = testService.taxCodeIncomeForEmployment(nino, TaxYear(), 1)

      val expected = Right(Some(taxCodeIncome1))

      Await.result(result, 5 seconds) mustBe expected
    }

    "return None when an income id is not present" in {
      val testService = createSut

      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Right(taxCodeIncomes)))

      val result = testService.taxCodeIncomeForEmployment(nino, TaxYear(), 99)

      Await.result(result, 5 seconds) mustBe Right(Option.empty[TaxCodeIncome])
    }

    "return the error when the TaxCodeIncome cannot be found" in {
      val testService = createSut

      when(taxAccountConnector.taxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(Left("error")))

      val result = testService.taxCodeIncomeForEmployment(nino, TaxYear(), 99)

      Await.result(result, 5 seconds) mustBe Left("error")
    }
  }

  "taxAccountSummary" must {
    "return the tax account summary from the connector" in {
      val sut = createSut
      when(taxAccountConnector.taxAccountSummary(any(), any())(any()))
        .thenReturn(EitherT.rightT(taxAccountSummary))

      val result = sut.taxAccountSummary(nino, TaxYear()).value
      Await.result(result, 5 seconds) mustBe Right(taxAccountSummary)
    }
  }

  "nonTaxCodeIncomes" must {
    "return Non tax code income" in {
      val sut = createSut
      when(taxAccountConnector.nonTaxCodeIncomes(any(), any())(any()))
        .thenReturn(Future.successful(nonTaxCodeIncome))

      val result = sut.nonTaxCodeIncomes(nino, TaxYear())
      Await.result(result, 5 seconds) mustBe nonTaxCodeIncome
    }
  }

  "updateEstimatedIncome" must {
    "return success update response" in {
      val sut = createSut
      when(taxAccountConnector.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Done))

      val result = sut.updateEstimatedIncome(nino, 100, TaxYear(), 1)
      Await.result(result, 5 seconds) mustBe Done
    }

    "return failure update response" in {
      val sut = createSut
      when(taxAccountConnector.updateEstimatedIncome(any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(new InternalServerException("Failed")))

      val result = sut.updateEstimatedIncome(nino, 100, TaxYear(), 1)
      assertThrows[InternalServerException](Await.result(result, 5 seconds))
    }
  }

  "totalTax" must {
    "return total tax" in {
      val sut      = createSut
      val totalTax = TotalTax(1000, Nil, None, None, None)
      when(taxAccountConnector.totalTax(any(), any())(any()))
        .thenReturn(Future.successful(totalTax))

      val result = sut.totalTax(nino, TaxYear())
      Await.result(result, 5 seconds) mustBe totalTax
    }
  }

  "scottishBandRate" must {
    "return a map of tax bands with corresponding rates" when {
      "tai connector returns total tax value with tax bands" in {
        val sut      = createSut
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
                TaxBand("1150L", "1150L", 10000, 500, Some(5000), Some(20000), 10)
              )
            )
          ),
          None,
          None,
          None
        )

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(totalTax))

        val result = sut.scottishBandRates(nino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map("D0" -> 20, "1150L" -> 10)
      }
    }

    "throw an empty map" when {
      "connector returns exception response" in {
        val sut = createSut

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.failed(new UnauthorizedException("Error message")))
        val result = sut.scottishBandRates(nino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map()
      }

      "none of the tax code is scottish" in {
        val sut = createSut

        val result = sut.scottishBandRates(nino, TaxYear(), taxCodeIncomes.map(_.taxCode))
        Await.result(result, 5 seconds) mustBe Map()
      }

      "tai connector returns total tax value without tax bands" in {
        val sut      = createSut
        val totalTax =
          TotalTax(1000, List(IncomeCategory(UkDividendsIncomeCategory, 10, 20, 30, Nil)), None, None, None)

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(totalTax))
        val result = sut.scottishBandRates(nino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map()
      }

      "tai connector returns total tax value without income category" in {
        val sut      = createSut
        val totalTax = TotalTax(1000, Nil, None, None, None)

        when(taxAccountConnector.totalTax(any(), any())(any()))
          .thenReturn(Future.successful(totalTax))
        val result = sut.scottishBandRates(nino, TaxYear(), taxCodes)
        Await.result(result, 5 seconds) mustBe Map()
      }
    }
  }

  val taxAccountSummary = TaxAccountSummary(111, 222, 333.23, 444.44, 111.11)

  private val taxCodeIncome1 =
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live)
  val taxCodeIncomes         = Seq(
    taxCodeIncome1,
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val taxCodes = Seq("SD0", "1150L")

  private val nonTaxCodeIncome = NonTaxCodeIncome(
    Some(income.UntaxedInterest(UntaxedInterestIncome, None, 100, "Untaxed Interest")),
    Seq(OtherNonTaxCodeIncome(Profit, None, 100, "Profit"))
  )

  private def createSut = new SUT

  val taxAccountConnector = mock[TaxAccountConnector]

  private class SUT
      extends TaxAccountService(
        taxAccountConnector
      )

}
