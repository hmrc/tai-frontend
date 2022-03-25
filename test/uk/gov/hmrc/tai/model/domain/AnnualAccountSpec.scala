/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain

import java.time.LocalDate
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.TaxYear

class AnnualAccountSpec extends PlaySpec {

  "totalIncome" must {
    "return the latest year to date value from the payments" when {
      "there is only one payment" in {
        SutWithOnePayment.totalIncomeYearToDate mustBe 2000
      }
      "there are multiple payments" in {
        SutWithMultiplePayments.totalIncomeYearToDate mustBe 2000
      }
    }
    "return zero for the latest year to date value" when {
      "there are no payments" in {
        SutWithNoPayments.totalIncomeYearToDate mustBe 0
      }
    }
  }

  "latestPayment" must {
    "return the latest payment" when {
      "there are multiple payments" in {
        val annualAccount = AnnualAccount(TaxYear(2017), Available, List(payment1, payment2), Nil)

        annualAccount.latestPayment mustBe Some(payment2)
      }
      "there is only one payment" in {

        val annualAccount = AnnualAccount(TaxYear(2017), Available, List(payment1), Nil)

        annualAccount.latestPayment mustBe Some(payment1)
      }
    }
    "return none" when {
      "there are no payments" in {
        val annualAccount = AnnualAccount(TaxYear(2017), Available, Nil, Nil)

        annualAccount.latestPayment mustBe None
      }
    }
  }

  "isIrregularPayment" must {
    "return true" when {
      "pay frequency is BiAnnually" in {
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(payment1.copy(payFrequency = BiAnnually)), Nil)

        annualAccount.isIrregularPayment mustBe true
      }

      "pay frequency is Annually" in {
        val annualAccount = AnnualAccount(TaxYear(), Available, Seq(payment1.copy(payFrequency = Annually)), Nil)

        annualAccount.isIrregularPayment mustBe true
      }

      "pay frequency is Irregular" in {
        val annualAccount = AnnualAccount(TaxYear(), Available, Seq(payment1), Nil)

        annualAccount.isIrregularPayment mustBe true
      }

      "latest payment is Irregular" in {
        val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
        val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
        val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
        val latestPayment = Payment(new LocalDate().minusWeeks(1), 100, 50, 25, 100, 50, 25, Irregular)
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)

        annualAccount.isIrregularPayment mustBe true
      }

      "latest payment is BiAnnually" in {
        val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
        val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
        val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
        val latestPayment = Payment(new LocalDate().minusWeeks(1), 100, 50, 25, 100, 50, 25, BiAnnually)
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)

        annualAccount.isIrregularPayment mustBe true
      }

      "latest payment is Annually" in {
        val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
        val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
        val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Monthly)
        val latestPayment = Payment(new LocalDate().minusWeeks(1), 100, 50, 25, 100, 50, 25, Annually)
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)

        annualAccount.isIrregularPayment mustBe true
      }
    }

    "return false" when {
      "pay frequency is Weekly" in {
        val annualAccount = AnnualAccount(TaxYear(), Available, Seq(payment1.copy(payFrequency = Weekly)), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "pay frequency is FortNightly" in {
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(payment1.copy(payFrequency = FortNightly)), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "pay frequency is FourWeekly" in {
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(payment1.copy(payFrequency = FourWeekly)), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "pay frequency is Monthly" in {
        val annualAccount = AnnualAccount(TaxYear(), Available, Seq(payment1.copy(payFrequency = Monthly)), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "pay frequency is Quarterly" in {
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(payment1.copy(payFrequency = Quarterly)), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "pay frequency is One Off" in {
        val annualAccount = AnnualAccount(TaxYear(), Available, Seq(payment2), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "latest payment is not Irregular" in {
        val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
        val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
        val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Irregular)
        val latestPayment = Payment(new LocalDate().minusWeeks(1), 100, 50, 25, 100, 50, 25, Monthly)
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "latest payment is not BiAnnually" in {
        val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
        val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
        val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, BiAnnually)
        val latestPayment = Payment(new LocalDate().minusWeeks(1), 100, 50, 25, 100, 50, 25, OneOff)
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "latest payment is not Annually" in {
        val firstPayment = Payment(new LocalDate().minusWeeks(4), 100, 50, 25, 100, 50, 25, Monthly)
        val secondPayment = Payment(new LocalDate().minusWeeks(3), 100, 50, 25, 100, 50, 25, Monthly)
        val thirdPayment = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Annually)
        val latestPayment = Payment(new LocalDate().minusWeeks(1), 100, 50, 25, 100, 50, 25, FortNightly)
        val annualAccount =
          AnnualAccount(TaxYear(), Available, Seq(latestPayment, secondPayment, thirdPayment, firstPayment), Nil)

        annualAccount.isIrregularPayment mustBe false
      }

      "payments are not available" in {
        val annualAccount = AnnualAccount(TaxYear(), Available, Nil, Nil)

        annualAccount.isIrregularPayment mustBe false
      }
    }
  }

  val payment1 = Payment(new LocalDate().minusWeeks(2), 100, 50, 25, 100, 50, 25, Irregular)
  val payment2 = Payment(new LocalDate().minusWeeks(1), 200, 100, 50, 100, 50, 25, OneOff)

  val SutWithNoPayments =
    AnnualAccount(taxYear = TaxYear("2017"), realTimeStatus = Available, payments = Nil, endOfTaxYearUpdates = Nil)

  val SutWithNoPayroll =
    AnnualAccount(taxYear = TaxYear("2017"), realTimeStatus = Available, payments = Nil, endOfTaxYearUpdates = Nil)

  val SutWithOnePayment = AnnualAccount(
    taxYear = TaxYear("2017"),
    realTimeStatus = Available,
    payments = List(
      Payment(
        date = new LocalDate(2017, 5, 26),
        amountYearToDate = 2000,
        taxAmountYearToDate = 1200,
        nationalInsuranceAmountYearToDate = 1500,
        amount = 200,
        taxAmount = 100,
        nationalInsuranceAmount = 150,
        payFrequency = Monthly
      )),
    endOfTaxYearUpdates = Nil
  )

  val SutWithMultiplePayments = SutWithOnePayment.copy(
    payments = SutWithOnePayment.payments :+
      Payment(
        date = new LocalDate(2017, 5, 26),
        amountYearToDate = 2000,
        taxAmountYearToDate = 1200,
        nationalInsuranceAmountYearToDate = 1500,
        amount = 200,
        taxAmount = 100,
        nationalInsuranceAmount = 150,
        payFrequency = FortNightly
      ) :+
      Payment(
        date = new LocalDate(2017, 6, 26),
        amountYearToDate = 2000,
        taxAmountYearToDate = 2500,
        nationalInsuranceAmountYearToDate = 1500,
        amount = 200,
        taxAmount = 100,
        nationalInsuranceAmount = 150,
        payFrequency = FourWeekly
      ))
}
