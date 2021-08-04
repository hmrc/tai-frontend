/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.income

import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants
import uk.gov.hmrc.tai.viewModels.GoogleAnalyticsSettings
import utils.BaseSpec

class ConfirmAmountEnteredViewModelSpec extends BaseSpec {

  val employmentId = 1000
  val employerName = "name"
  val currentAmount = 123
  val estimatedIncome = 456

  "nextYearEstimatedPay" should {
    "produce a Google Analytics settings" when {
      "CY Irregular pay" in {
        val viewModel =
          ConfirmAmountEnteredViewModel(employmentId, employerName, currentAmount, estimatedIncome, IrregularPay)

        val expectedDimensions =
          Some(Map(GoogleAnalyticsConstants.taiCYEstimatedIncome -> "currentAmount=£123;newAmount=£456"))
        val expectedSettings: GoogleAnalyticsSettings = GoogleAnalyticsSettings(expectedDimensions)

        viewModel.gaSettings mustBe expectedSettings
      }

      "CY Annual pay" in {
        val viewModel = ConfirmAmountEnteredViewModel(employerName, currentAmount, estimatedIncome)

        val expectedDimensions =
          Some(Map(GoogleAnalyticsConstants.taiCYEstimatedIncome -> "currentAmount=£123;newAmount=£456"))
        val expectedSettings: GoogleAnalyticsSettings = GoogleAnalyticsSettings(expectedDimensions)

        viewModel.gaSettings mustBe expectedSettings
      }

      "CY+1" in {
        val viewModel =
          ConfirmAmountEnteredViewModel(employmentId, employerName, currentAmount, estimatedIncome, NextYearPay)

        val expectedDimensions =
          Some(Map(GoogleAnalyticsConstants.taiCYPlusOneEstimatedIncome -> "currentAmount=£123;newAmount=£456"))
        val expectedSettings: GoogleAnalyticsSettings = GoogleAnalyticsSettings(expectedDimensions)

        viewModel.gaSettings mustBe expectedSettings
      }
    }
  }
}
