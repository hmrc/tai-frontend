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

package uk.gov.hmrc.tai.model.admin

import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlagName

case object CyPlusOneToggle extends FeatureFlagName {
  override val name: String                = "cy-plus-one-toggle"
  override val description: Option[String] = Some(
    "Enable/disable CY+1 (next tax year) features, including income comparison and estimated pay updates."
  )
}

case object IncomeTaxHistoryToggle extends FeatureFlagName {
  override val name: String                = "income-tax-history-toggle"
  override val description: Option[String] = Some(
    "Enable/disable access to the income tax history section showing previous tax years"
  )
}

case object DesignatoryDetailsCheck extends FeatureFlagName {
  override val name: String                = "designatory-details-check"
  override val description: Option[String] = Some(
    "Enable/disable fetching of personal designatory details (e.g., name, address) from Citizen Details service."
  )
}
