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

package uk.gov.hmrc.tai.viewModels.incomeTaxComparison

import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.viewModels.{IncomeSourceComparisonViewModel, TaxCodeComparisonViewModel, TaxFreeAmountComparisonViewModel}
import utils.BaseSpec

class IncomeTaxComparisonViewModelSpec extends BaseSpec {

  def createIncomeTaxComparisonViewModel(
    isEstimatedCYPlusOneJourneyCompleted: Boolean): IncomeTaxComparisonViewModel = {
    val currentYearItem = EstimatedIncomeTaxComparisonItem(TaxYear(), 100)
    val nextYearItem = EstimatedIncomeTaxComparisonItem(TaxYear().next, 200)
    val estimatedIncomeTaxComparisonViewModel = EstimatedIncomeTaxComparisonViewModel(
      Seq(currentYearItem, nextYearItem))

    IncomeTaxComparisonViewModel(
      "USERNAME",
      estimatedIncomeTaxComparisonViewModel,
      TaxCodeComparisonViewModel(Nil),
      TaxFreeAmountComparisonViewModel(Nil, Nil),
      IncomeSourceComparisonViewModel(Nil, Nil),
      isEstimatedCYPlusOneJourneyCompleted
    )
  }

  "IncomeTaxComparisonViewModel" when {

    "not show the banner when journey is incomplete" in {
      val model = createIncomeTaxComparisonViewModel(isEstimatedCYPlusOneJourneyCompleted = false)
      model.isEstimatedCYPlusOneJourneyCompleted mustBe false
    }

    "not show the banner when journey is complete" in {
      val model = createIncomeTaxComparisonViewModel(isEstimatedCYPlusOneJourneyCompleted = true)
      model.isEstimatedCYPlusOneJourneyCompleted mustBe true
    }

  }
}
