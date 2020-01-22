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

import org.scalatestplus.play.PlaySpec

class GoogleAnalyticsSettingsSpec extends PlaySpec {

  val gaKey = "key"
  val currentAmount = 12345
  val newAmount = 67890

  val expectedDimensions = Some(Map(gaKey -> "currentAmount=£12,345;newAmount=£67,890"))

  "createForAnnualIncome" must {
    "create a google analytics settings for ints" in {
      val newAmount = 67890
      val actual = GoogleAnalyticsSettings.createForAnnualIncome(gaKey, currentAmount, newAmount)

      actual mustBe GoogleAnalyticsSettings(expectedDimensions)
    }

    "create a google analytics settings for strings" in {
      val newAmount = Some("67890")
      val actual = GoogleAnalyticsSettings.createForAnnualIncome(gaKey, currentAmount, newAmount)
      actual mustBe GoogleAnalyticsSettings(expectedDimensions)
    }
  }
}
