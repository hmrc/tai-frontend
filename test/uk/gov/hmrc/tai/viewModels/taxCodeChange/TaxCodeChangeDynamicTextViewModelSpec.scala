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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{CodingComponentPairDescription, TaxFreeInfo}

class TaxCodeChangeDynamicTextViewModelSpec extends PlaySpec {

  private val taxFreeInfo = TaxFreeInfo("12-12-2015", 2000, 1000)
  private val jobExpensesIncrease: CodingComponentPairDescription = CodingComponentPairDescription("Job Expense from Sainsburys", 50, 100)
  private val carBenefitIncrease: CodingComponentPairDescription = CodingComponentPairDescription("Car Benefit from TESCO", 1000, 2000)

  "TaxCodeChangeDynamicTextViewModel apply method" must {
    "translate a YourTaxFreeAmountViewModel to a seq of dynamic text" in {
      val taxFreeAmount = YourTaxFreeAmountViewModel(None, taxFreeInfo, Seq(jobExpensesIncrease), Seq(carBenefitIncrease))

      val model = TaxCodeChangeDynamicTextViewModel(taxFreeAmount)

      model.dynamicText mustBe Seq(
        "Job Expense from Sainsburys has increased from 50 to 100",
        "Car Benefit from TESCO has increased from 1000 to 2000"
      )
    }

    "translate a allowance increase to a text" in {
      val taxFreeAmount = YourTaxFreeAmountViewModel(None, taxFreeInfo, Seq(jobExpensesIncrease), Seq.empty)

      val model = TaxCodeChangeDynamicTextViewModel(taxFreeAmount)

      model.dynamicText mustBe Seq("Job Expense from Sainsburys has increased from 50 to 100")
    }

    "translate a allowance decrease to text" in {
      val jobExpensesDecrease = CodingComponentPairDescription("Job Expense from Sainsburys", 100, 50)

      val taxFreeAmount = YourTaxFreeAmountViewModel(None, taxFreeInfo, Seq(jobExpensesDecrease), Seq.empty)

      val model = TaxCodeChangeDynamicTextViewModel(taxFreeAmount)

      model.dynamicText mustBe Seq("Job Expense from Sainsburys has decreased from 100 to 50")
    }

    "translate a deduction increase to a text" in {
      val taxFreeAmount = YourTaxFreeAmountViewModel(None, taxFreeInfo, Seq(carBenefitIncrease), Seq.empty)

      val model = TaxCodeChangeDynamicTextViewModel(taxFreeAmount)

      model.dynamicText mustBe Seq("Car Benefit from TESCO has increased from 1000 to 2000")
    }

    "translate a deduction decrease to text" in {
      val carBenefitDecrease = CodingComponentPairDescription("Car Benefit from TESCO", 2000, 1000)

      val taxFreeAmount = YourTaxFreeAmountViewModel(None, taxFreeInfo, Seq(carBenefitDecrease), Seq.empty)

      val model = TaxCodeChangeDynamicTextViewModel(taxFreeAmount)

      model.dynamicText mustBe Seq("Car Benefit from TESCO has decreased from 2000 to 1000")
    }
  }
}
