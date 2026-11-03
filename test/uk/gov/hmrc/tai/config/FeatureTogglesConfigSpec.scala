/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tai.config

import uk.gov.hmrc.mongoFeatureToggles.internal.config.FeatureFlagsConfig
import uk.gov.hmrc.tai.model.admin.FeatureFlagNameTai.{CyPlusOneToggle, DesignatoryDetailsCheck, IncomeTaxHistoryToggle}
import utils.BaseSpec

class FeatureTogglesConfigSpec extends BaseSpec {

  private val featureFlagsConfig = inject[FeatureFlagsConfig]

  "feature toggle configuration" should {
    "define all application toggles with remote and local configuration values" in {
      val toggles = Seq(
        CyPlusOneToggle      -> "Enable/disable CY+1 (next tax year) features, including income comparison and estimated pay updates.",
        IncomeTaxHistoryToggle -> "Enable/disable access to the income tax history section showing previous tax years",
        DesignatoryDetailsCheck -> "Enable/disable fetching of personal designatory details (e.g., name, address) from Citizen Details service."
      )

      toggles.foreach { case (toggle, description) =>
        val path = s"mongo-feature-toggles-client.toggles.${toggle.name}"

        app.configuration.get[String](s"$path.description") mustBe description
        app.configuration.get[String](s"$path.toggleSourceValue") mustBe "remote-configuration"
        app.configuration.get[Boolean](s"$path.remote-configuration.defaultState") mustBe false
        app.configuration.get[Boolean](s"$path.local-configuration.isEnabled") mustBe false
        featureFlagsConfig.get(toggle.name).isDefined mustBe true
      }
    }
  }
}
