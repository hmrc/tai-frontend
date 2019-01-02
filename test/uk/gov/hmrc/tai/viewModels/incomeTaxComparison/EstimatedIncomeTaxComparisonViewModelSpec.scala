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

package uk.gov.hmrc.tai.viewModels.incomeTaxComparison

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.TaxYear

class EstimatedIncomeTaxComparisonViewModelSpec extends PlaySpec {

  "EstimatedIncomeTaxComparisonViewModel" should {
    "return correct change in tax amount" when {
      "CY and CY+1 items are supplied to the view model" in {

        val cy = EstimatedIncomeTaxComparisonItem(TaxYear(),100)
        val cyPlusOne = EstimatedIncomeTaxComparisonItem(TaxYear().next,200)
        val SUT = EstimatedIncomeTaxComparisonViewModel(Seq(cy,cyPlusOne))

        SUT.changeInTaxAmount mustBe 100

      }
    }

    "sort the comparison items by ascending year" when {
      "CY and CY+1 items are supplied to the view model" in {

        val cy = EstimatedIncomeTaxComparisonItem(TaxYear(),100)
        val cyPlusOne = EstimatedIncomeTaxComparisonItem(TaxYear().next,200)

        val SUT = EstimatedIncomeTaxComparisonViewModel(Seq(cyPlusOne, cy))

        SUT.comparisonItemsByYear mustBe Seq(cy, cyPlusOne)
      }
    }
  }
}
