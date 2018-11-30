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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.{TaxYear, tai}
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.model

class HistoricPayAsYouEarnViewModelSpec extends PlaySpec with FakeTaiPlayApplication {

  "HistoricPayAsYouEarnViewModel" should {

    "return Nil employments" when {
      "no employments are supplied" in {

        val sut = createSut(Nil)

        sut.employments mustBe Nil
      }

      "an employment is supplied but the employment doesn't have an AnnualAccount for the required year" in {

        val cyMinusThreeAnnualAccount = AnnualAccount("1-2-3", cyMinusThreeTaxYear, Available, Nil, Nil)
        val cyMinusOneAnnualAccount = AnnualAccount("1-2-3", cyMinusOneTaxYear, Available, Nil, Nil)

        val multiYearAccounts = Seq(cyMinusOneAnnualAccount, cyMinusThreeAnnualAccount)

        val employment = Employment("test employment", Some("111111"), empStartDateWithinCYMinusThree,
          None, multiYearAccounts, "", "", 2, None, false, false)

        val sut = createSut(cyMinusTwoTaxYear, Seq(employment))

        sut.employments mustBe Nil
      }
    }

    "return one employment with zero YTD totalIncome from zero payments" when {
      "one employment is supplied which has an AnnualAccount but no payments or updates" in {

        val employment = Employment("test employment", Some("111111"), empStartDateWithinCYMinusOne,
          None, Seq(annualAccountWithNoPayments), "", "", 2, None, false, false)

        val sut = createSut(Seq(employment))

        val employmentVMs = sut.employments

        employmentVMs.length mustBe 1

        val employmentVM = employmentVMs.head

        employmentVM.name mustBe "test employment"
        employmentVM.taxablePayYTD mustBe BigDecimal(0)
        employmentVM.id mustBe 2
      }
    }

    "return one employment with YTD totalIncome from one payment" when {
      "one employment is supplied which has an AnnualAccount containing one payment" in {

        val employment = Employment("test employment", Some("111111"), empStartDateWithinCYMinusOne,
          None, Seq(annualAccountWithSinglePayment), "", "", 2, None, false, false)

        val sut = createSut(Seq(employment))

        val employmentVMs = sut.employments

        employmentVMs.length mustBe 1

        val employmentVM = employmentVMs.head

        employmentVM.name mustBe "test employment"
        employmentVM.taxablePayYTD mustBe BigDecimal(10)
        employmentVM.id mustBe 2
      }
    }

    "return one employment with a YTD totalIncome from multiple payments" when {
      "one employment is supplied which has an AnnualAccount containing multiple payments" in {

        val employment = Employment("test employment", Some("111111"), empStartDateWithinCYMinusOne,
          None, Seq(annualAccountWithMultiplePayments), "", "", 2, None, false, false)

        val sut = createSut(Seq(employment))

        val employmentVMs = sut.employments

        employmentVMs.length mustBe 1

        val employmentVM = employmentVMs.head

        employmentVM.name mustBe "test employment"
        employmentVM.taxablePayYTD mustBe BigDecimal(20)
        employmentVM.id mustBe 2
      }
    }

    "return pensions" when {
      "the income sources receivingOccupationalPension is set to true" in {
        val employment = Employment("test pension", Some("111111"), empStartDateWithinCYMinusOne,
          None, Seq(annualAccountWithMultiplePayments), "", "", 2, None, false, true)

        val sut = createSut(Seq(employment))

        val employmentVMs = sut.employments
        val pensionVMs = sut.pensions

        employmentVMs mustBe Nil

        pensionVMs.length mustBe 1

        val pensionVM = pensionVMs.head

        pensionVM.name mustBe "test pension"
        pensionVM.id mustBe 2
      }
    }

    "return multiple employments sorted with an Id and with a YTD totalIncome from multiple payments" when {
      "multiple employments are supplied containing an AnnualAccount which has multiple payments" in {

        val employment1 = Employment("test employment1", Some("111111"), empStartDateWithinCYMinusOne,
          None, Seq(annualAccountWithMultiplePayments), "", "", 2, None, false, false)
        val employment2 = Employment("test employment2", Some("111112"), empStartDateWithinCYMinusOne,
          None, Seq(annualAccountWithMultiplePayments), "", "", 3, None, false, false)

        val sut = createSut(Seq(employment2, employment1))

        val employmentVMs = sut.employments

        employmentVMs.length mustBe 2

        employmentVMs.head.name mustBe "test employment1"
        employmentVMs.head.taxablePayYTD mustBe BigDecimal(20)
        employmentVMs.head.id mustBe 2

        employmentVMs(1).name mustBe "test employment2"
        employmentVMs(1).taxablePayYTD mustBe BigDecimal(20)
        employmentVMs(1).id mustBe 3
      }
    }

    "return true for p800ServiceIsAvailable" when {
      "the viewmodel is provided with the CY-1 tax year" in {

        val sut = createSut(cyMinusOneTaxYear, Nil)

        sut.p800ServiceIsAvailable mustBe true
      }
    }

    "return false for p800ServiceIsAvailable" when {
      "the viewmodel is provided with a tax year earlier than CY-1 such as CY-2" in {

        val sut = createSut(cyMinusTwoTaxYear, Nil)

        sut.p800ServiceIsAvailable mustBe false
      }
    }

    "return false for p800ServiceIsAvailable" when {
      "the viewmodel is provided with a tax year earlier than CY-1 such as CY-3" in {

        val sut = createSut(cyMinusThreeTaxYear, Nil)

        sut.p800ServiceIsAvailable mustBe false
      }
    }

    "return false for p800ServiceIsAvailable" when {
      "the viewmodel is provided with a tax year earlier than CY-1 such as CY-4" in {

        val sut = createSut(cyMinusFourTaxYear, Nil)

        sut.p800ServiceIsAvailable mustBe false
      }
    }
  }

  private val currentYear: Int = TaxYear().year
  private val cyMinusOneTaxYear: TaxYear = model.TaxYear(currentYear - 1)
  private val cyMinusTwoTaxYear: TaxYear = model.TaxYear(currentYear - 2)
  private val cyMinusThreeTaxYear: TaxYear = model.TaxYear(currentYear - 3)
  private val cyMinusFourTaxYear: TaxYear = model.TaxYear(currentYear - 4)

  private val empStartDateWithinCYMinusOne = cyMinusOneTaxYear.start.plusMonths(2)
  private val empStartDateWithinCYMinusThree = cyMinusThreeTaxYear.start.plusMonths(2)

  private val singleAnnualAccountsPayment = Seq(
    Payment(cyMinusOneTaxYear.start, 10, 10, 0, 0, 0, 0, Monthly))

  private val multipleAnnualAccountsPayments = Seq(
    Payment(cyMinusOneTaxYear.start, 10, 10, 0, 0, 0, 0, Weekly),
    Payment(cyMinusOneTaxYear.start.plusMonths(1), 20, 20, 0, 0, 0, 0, FourWeekly))

  private val annualAccountWithNoPayments = AnnualAccount("", cyMinusOneTaxYear, Available, Nil, Nil)
  private val annualAccountWithSinglePayment = AnnualAccount("", cyMinusOneTaxYear, Available, singleAnnualAccountsPayment, Nil)
  private val annualAccountWithMultiplePayments = AnnualAccount("", cyMinusOneTaxYear, Available, multipleAnnualAccountsPayments, Nil)

  private def createSut(employments: Seq[Employment]) = HistoricPayAsYouEarnViewModel(cyMinusOneTaxYear, employments, true)
  private def createSut(taxYear: TaxYear, employments: Seq[Employment]) = HistoricPayAsYouEarnViewModel(taxYear, employments, true)
}