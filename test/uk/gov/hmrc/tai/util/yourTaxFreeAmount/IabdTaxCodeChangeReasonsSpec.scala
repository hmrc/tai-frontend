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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import controllers.FakeTaiPlayApplication
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.CodingComponentPair
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}

class IabdTaxCodeChangeReasonsSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi = app.injector.instanceOf[MessagesApi]

  val taxFreeInfo = TaxFreeInfo("12-12-2015", 2000, 1000)
  val jobExpensesIncrease = CodingComponentPair(JobExpenses, Some(2), Some(50), Some(100))
  val carBenefitDecrease = CodingComponentPair(CarBenefit, Some(1), Some(5555), Some(2345))
  val taxCodeChange = mock[TaxCodeChange]

  val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
  val incomeCatergories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
  val totalTax: TotalTax = TotalTax(1000, Seq(incomeCatergories), None, None, None)

  val iabdTaxCodeChangeReasons = new IabdTaxCodeChangeReasons(totalTax)

  "iabdReasons method" must {
    "have no reasons" when {
      "there are no allowances and deductions" in {
        val pairs = AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
        val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

        reasons mustBe Seq.empty
      }

      "there is no current amount" in {
        val noCurrentAmount = CodingComponentPair(JobExpenses, None, Some(123), None)
        val pairs = AllowancesAndDeductionPairs(Seq(noCurrentAmount), Seq.empty)

        val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

        reasons mustBe Seq.empty
      }
    }
  }

  "starting a new benefit" must {
    "give multiple reasons when you have multiple new benefits" in {
      val newBenefit1 = CodingComponentPair(JobExpenses, None, None, Some(12345))
      val newBenefit2 = CodingComponentPair(CarBenefit, None, None, Some(98765))

      val pairs = AllowancesAndDeductionPairs(Seq(newBenefit1), Seq(newBenefit2))

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq(
        messagesApi("tai.taxCodeComparison.iabd.added", "Job expenses", "£12,345"),
        messagesApi("tai.taxCodeComparison.iabd.added", "Car benefit", "£98,765")
      )
    }

    "return all new allowances/deductions in the correct order when you have multiple new allowances/deductions" in {
      val newDebt = CodingComponentPair(EstimatedTaxYouOweThisYear, None, None, Some(123))
      val newAllowance = CodingComponentPair(JobExpenses, None, None, Some(123))
      val changedAllowance = CodingComponentPair(VehicleExpenses, None, Some(100), Some(123))
      val newDeduction = CodingComponentPair(CarBenefit, None, None, Some(123))
      val changedDeduction = CodingComponentPair(MedicalInsurance, None, Some(200), Some(123))

      val pairs =
        AllowancesAndDeductionPairs(Seq(changedAllowance, newAllowance), Seq(changedDeduction, newDebt, newDeduction))

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq(
        messagesApi("tai.taxCodeComparison.iabd.added", "Job expenses", "£123"),
        messagesApi(
          "tai.taxCodeComparison.iabd.ammended",
          "Vehicle expenses",
          messagesApi("tai.taxCodeComparison.iabd.increased"),
          "£100",
          "£123"),
        messagesApi("tai.taxCodeComparison.iabd.added", "Car benefit", "£123"),
        messagesApi(
          "tai.taxCodeComparison.iabd.ammended",
          "Medical insurance",
          messagesApi("tai.taxCodeComparison.iabd.reduced"),
          "£200",
          "£123"),
        messagesApi("tai.taxCodeComparison.iabd.we.estimated.you.have.underpaid", "£24")
      )
    }

    "give a reason for an earlier year's adjustment" in {
      val newBenefit = CodingComponentPair(EarlyYearsAdjustment, None, None, Some(123))
      val pairs = AllowancesAndDeductionPairs(Seq(newBenefit), Seq.empty)

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)
      reasons mustBe Seq(messagesApi("tai.taxCodeComparison.iabd.you.have.claimed.expenses"))
    }

    "give a reason with the amount for underpaid from a previous year" in {
      val newBenefit = CodingComponentPair(UnderPaymentFromPreviousYear, None, None, Some(123))
      val pairs = AllowancesAndDeductionPairs(Seq(newBenefit), Seq.empty)

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)
      reasons mustBe Seq(messagesApi("tai.taxCodeComparison.iabd.you.have.underpaid", "£24"))
    }

    "give a reason with the amount for estimated tax owed this year" in {
      val newBenefit = CodingComponentPair(EstimatedTaxYouOweThisYear, None, None, Some(123))
      val pairs = AllowancesAndDeductionPairs(Seq(newBenefit), Seq.empty)

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)
      reasons mustBe Seq(messagesApi("tai.taxCodeComparison.iabd.we.estimated.you.have.underpaid", "£24"))
    }
  }

  "amending a benefit" must {
    "have no reasons if the previous and current amounts are the same" in {
      val sameAmountAllowance = CodingComponentPair(JobExpenses, Some(2), Some(12345), Some(12345))
      val pairs = AllowancesAndDeductionPairs(Seq.empty, Seq(sameAmountAllowance))

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq.empty
    }

    "give multiple reasons for a tax code change" in {
      val pairs = AllowancesAndDeductionPairs(Seq(jobExpensesIncrease), Seq(carBenefitDecrease))
      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq(
        "your Job expenses has been increased from £50 to £100",
        "your Car benefit has been reduced from £5,555 to £2,345"
      )
    }
  }
}
