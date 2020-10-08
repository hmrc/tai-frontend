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

package uk.gov.hmrc.tai.viewModels

import org.joda.time.LocalDate
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import utils.BaseSpec

class PreviousYearUnderpaymentViewModelSpec extends BaseSpec {

  "PreviousYearUnderpaymentViewModel apply method" when {

    "given only employments within CY-1" must {
      "return an instance with a shouldHavePaid drawn from the totalEstimatedTax value of the supplied TaxAccountSummary" in {

        val result = PreviousYearUnderpaymentViewModel(codingComponents, sampleEmployments, totalTax, "", "")

        result.allowanceReducedBy mustEqual 500.00
        result.poundedAmountDue mustEqual "£100.00"
      }
    }

    "given employments accross multiple years" must {
      "return an instance with a shouldHavePaid drawn from the totalEstimatedTax value of the supplied TaxAccountSummary" in {

        val result = PreviousYearUnderpaymentViewModel(codingComponents, sampleEmployments2, totalTax, "", "")

        result.allowanceReducedBy mustEqual 500.00
        result.poundedAmountDue mustEqual "£100.00"
      }
    }
  }

  val actuallyPaid = 900.00

  val codingComponents = Seq(
    CodingComponent(UnderPaymentFromPreviousYear, Some(1), 500.00, "UnderPaymentFromPreviousYear"),
    CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
  )

  val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
  val incomeCatergories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
  val totalTax: TotalTax = TotalTax(1000, Seq(incomeCatergories), None, None, None)

  val empName = "employer name"
  val previousYear = uk.gov.hmrc.tai.model.TaxYear().prev

  val samplePayment = Payment(
    date = new LocalDate(2017, 5, 26),
    amountYearToDate = 2000,
    taxAmountYearToDate = 900,
    nationalInsuranceAmountYearToDate = 1500,
    amount = 200,
    taxAmount = 100,
    nationalInsuranceAmount = 150,
    payFrequency = Monthly
  )
  val sampleAnnualAccount = AnnualAccount("1-2-3", previousYear, Available, List(samplePayment), Nil)

  val samplePayment2 = Payment(
    date = new LocalDate(2016, 5, 26),
    amountYearToDate = 5000,
    taxAmountYearToDate = 4000,
    nationalInsuranceAmountYearToDate = 1500,
    amount = 200,
    taxAmount = 100,
    nationalInsuranceAmount = 150,
    payFrequency = Monthly
  )
  val sampleAnnualAccount2 = AnnualAccount("1-2-3", TaxYear(2016), Available, List(samplePayment), Nil)

  val sampleEmployment1 =
    Employment(
      empName,
      Live,
      None,
      new LocalDate(2017, 6, 9),
      None,
      Nil,
      "taxNumber",
      "payeNumber",
      1,
      None,
      false,
      false)
  val sampleEmployment2 = Employment(
    "emp2",
    Live,
    None,
    new LocalDate(2017, 6, 10),
    None,
    Seq(sampleAnnualAccount),
    "taxNumber",
    "payeNumber",
    2,
    None,
    false,
    false)
  val sampleEmployments = List(sampleEmployment1, sampleEmployment2)

  val sampleEmployment3 =
    Employment(
      empName,
      Live,
      None,
      new LocalDate(2016, 6, 9),
      None,
      Nil,
      "taxNumber",
      "payeNumber",
      1,
      None,
      false,
      false)
  val sampleEmployment4 = Employment(
    "emp2",
    Live,
    None,
    new LocalDate(2016, 6, 10),
    None,
    Seq(sampleAnnualAccount2),
    "taxNumber",
    "payeNumber",
    2,
    None,
    false,
    false)
  val sampleEmployments2 = sampleEmployments ++ List(sampleEmployment3, sampleEmployment4)

}
