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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import uk.gov.hmrc.tai.model.CodingComponentPairModel
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxFreeInfo
import utils.BaseSpec

class YourTaxFreeAmountViewModelSpec extends BaseSpec with ViewModelHelper {

  private val pairs = Seq(
    CodingComponentPairModel("Thing", 1000, 2000),
    CodingComponentPairModel("Thing", 3000, 3000)
  )

  private val taxFreeInfo = TaxFreeInfo("12-12-2015", 2000, 1000)

  "YourTaxFreeAmountViewModel" should {
    "set showPreviousColumn as false with no previousTaxFreeInfo" in {
      val viewModel = YourTaxFreeAmountViewModel(None, taxFreeInfo, Seq.empty, Seq.empty)
      viewModel.showPreviousColumn mustBe false
    }

    "set showPreviousColumn as true with a previousTaxFreeInfo" in {
      val viewModel = YourTaxFreeAmountViewModel(Some(taxFreeInfo), taxFreeInfo, Seq.empty, Seq.empty)
      viewModel.showPreviousColumn mustBe true
    }

    "set columns as 2 with no previousTaxFreeInfo" in {
      val viewModel = YourTaxFreeAmountViewModel(None, taxFreeInfo, Seq.empty, Seq.empty)
      viewModel.columns mustBe 2
    }

    "set columns as 3 with a previousTaxFreeInfo" in {
      val viewModel = YourTaxFreeAmountViewModel(Some(taxFreeInfo), taxFreeInfo, Seq.empty, Seq.empty)
      viewModel.columns mustBe 3
    }

    "prettyPrint BigDecimals as currency" in {
      YourTaxFreeAmountViewModel.prettyPrint(1000) mustBe "£1,000"
    }

    "sum previous when passed a seq of pairs and return as currency" in {
      YourTaxFreeAmountViewModel.totalPrevious(pairs) mustBe "£4,000"
    }

    "sum current when passed a seq of pairs and return as currency" in {
      YourTaxFreeAmountViewModel.totalCurrent(pairs) mustBe "£5,000"
    }
  }
}
