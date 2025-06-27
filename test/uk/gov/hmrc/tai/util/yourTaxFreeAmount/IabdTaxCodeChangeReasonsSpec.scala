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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import uk.gov.hmrc.tai.model.CodingComponentPair
import uk.gov.hmrc.tai.model.domain._
import utils.BaseSpec

class IabdTaxCodeChangeReasonsSpec extends BaseSpec {

  val taxFreeInfo: TaxFreeInfo                 = TaxFreeInfo("12-12-2015", 2000, 1000)
  val jobExpensesIncrease: CodingComponentPair = CodingComponentPair(JobExpenses, Some(2), Some(50), Some(100), None)
  val carBenefitDecrease: CodingComponentPair  = CodingComponentPair(CarBenefit, Some(1), Some(5555), Some(2345), None)
  val taxCodeChange: TaxCodeChange             = mock[TaxCodeChange]

  val iabdTaxCodeChangeReasons = new IabdTaxCodeChangeReasons

  "iabdReasons method" must {
    "have no reasons" when {
      "there are no allowances and deductions" in {
        val pairs   = AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
        val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

        reasons mustBe Seq.empty
      }

      "there is no current amount" in {
        val noCurrentAmount = CodingComponentPair(JobExpenses, None, Some(123), None, None)
        val pairs           = AllowancesAndDeductionPairs(Seq(noCurrentAmount), Seq.empty)

        val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

        reasons mustBe Seq.empty
      }
    }
  }

  "starting a new benefit" must {
    "give multiple reasons when you have multiple new benefits" in {
      val newBenefit1 = CodingComponentPair(JobExpenses, None, None, Some(12345), None)
      val newBenefit2 = CodingComponentPair(CarBenefit, None, None, Some(98765), None)

      val pairs = AllowancesAndDeductionPairs(Seq(newBenefit1), Seq(newBenefit2))

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq(
        messagesApi("tai.taxCodeComparison.iabd.added", "job expenses", "£12,345"),
        messagesApi("tai.taxCodeComparison.iabd.added", "car benefit", "£98,765")
      )
    }

    "return all new allowances/deductions in the correct order when you have multiple new allowances/deductions" in {
      val inputAmount = Some(BigDecimal(12345))

      val newDebt          = CodingComponentPair(EstimatedTaxYouOweThisYear, None, None, Some(123), inputAmount)
      val newAllowance     = CodingComponentPair(JobExpenses, None, None, Some(123), None)
      val changedAllowance = CodingComponentPair(VehicleExpenses, None, Some(100), Some(123), None)
      val newDeduction     = CodingComponentPair(CarBenefit, None, None, Some(123), None)
      val changedDeduction = CodingComponentPair(MedicalInsurance, None, Some(200), Some(123), None)

      val pairs =
        AllowancesAndDeductionPairs(Seq(changedAllowance, newAllowance), Seq(changedDeduction, newDebt, newDeduction))

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq(
        messagesApi("tai.taxCodeComparison.iabd.added", "job expenses", "£123"),
        messagesApi(
          "tai.taxCodeComparison.iabd.ammended",
          "vehicle expenses",
          messagesApi("tai.taxCodeComparison.iabd.increased"),
          "£100",
          "£123"
        ),
        messagesApi("tai.taxCodeComparison.iabd.added", "car benefit", "£123"),
        messagesApi(
          "tai.taxCodeComparison.iabd.ammended",
          "medical insurance",
          messagesApi("tai.taxCodeComparison.iabd.reduced"),
          "£200",
          "£123"
        ),
        messagesApi("tai.taxCodeComparison.iabd.we.estimated.you.have.underpaid", "£12,345")
      )
    }

    "give a reason for an earlier year's adjustment" in {
      val newBenefit = CodingComponentPair(EarlyYearsAdjustment, None, None, Some(123), None)
      val pairs      = AllowancesAndDeductionPairs(Seq(newBenefit), Seq.empty)

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)
      reasons mustBe Seq(messagesApi("tai.taxCodeComparison.iabd.you.have.claimed.expenses"))
    }

    "give a reason with the amount for underpaid from a previous year" in {
      val inputAmount = Some(BigDecimal(4567))

      val newBenefit = CodingComponentPair(UnderPaymentFromPreviousYear, None, None, Some(123), inputAmount)
      val pairs      = AllowancesAndDeductionPairs(Seq(newBenefit), Seq.empty)

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)
      reasons mustBe Seq(messagesApi("tai.taxCodeComparison.iabd.you.have.underpaid", "£4,567"))
    }

    "give a reason with the amount for estimated tax owed this year" in {
      val inputAmount = Some(BigDecimal(890))

      val newBenefit = CodingComponentPair(EstimatedTaxYouOweThisYear, None, None, Some(123), inputAmount)
      val pairs      = AllowancesAndDeductionPairs(Seq(newBenefit), Seq.empty)

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)
      reasons mustBe Seq(messagesApi("tai.taxCodeComparison.iabd.we.estimated.you.have.underpaid", "£890"))
    }
  }

  "amending a benefit" must {
    "have no reasons if the previous and current amounts are the same" in {
      val sameAmountAllowance = CodingComponentPair(JobExpenses, Some(2), Some(12345), Some(12345), None)
      val pairs               = AllowancesAndDeductionPairs(Seq.empty, Seq(sameAmountAllowance))

      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq.empty
    }

    "give multiple reasons for a tax code change" in {
      val pairs   = AllowancesAndDeductionPairs(Seq(jobExpensesIncrease), Seq(carBenefitDecrease))
      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq(
        "your job expenses has increased from £50 to £100",
        "your car benefit has reduced from £5,555 to £2,345"
      )
    }
  }

  "handle HICBCPaye with ammended message correctly" in {
    val hicbcIncrease = CodingComponentPair(HICBCPaye, Some(1), Some(100), Some(200), None)
    val hicbcDecrease = CodingComponentPair(HICBCPaye, Some(1), Some(300), Some(200), None)

    val increasePairs = AllowancesAndDeductionPairs(Seq(hicbcIncrease), Seq.empty)
    val decreasePairs = AllowancesAndDeductionPairs(Seq(hicbcDecrease), Seq.empty)

    val increaseReasons = iabdTaxCodeChangeReasons.reasons(increasePairs)
    val decreaseReasons = iabdTaxCodeChangeReasons.reasons(decreasePairs)

    increaseReasons mustBe Seq(
      messagesApi(
        "tai.taxCodeComparison.iabd.ammended",
        HICBCPaye.toV2Message(),
        messagesApi("tai.taxCodeComparison.iabd.increased"),
        "£100",
        "£200"
      )
    )

    decreaseReasons mustBe Seq(
      messagesApi(
        "tai.taxCodeComparison.iabd.ammended",
        HICBCPaye.toV2Message(),
        messagesApi("tai.taxCodeComparison.iabd.reduced"),
        "£300",
        "£200"
      )
    )
  }
}
