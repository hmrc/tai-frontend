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

package uk.gov.hmrc.tai.config

trait FeatureTogglesConfig { self: ApplicationConfig =>
  val cyPlusOneEnabled: Boolean = getOptional[Boolean]("tai.cyPlusOne.enabled").getOrElse(false)
  val jrsClaimsEnabled: Boolean = getOptional[Boolean]("tai.jrsClaims.enabled").getOrElse(false)
  val welshLanguageEnabled: Boolean = getOptional[Boolean]("tai.feature.welshLanguage.enabled").getOrElse(false)
  val companyCarForceRedirectEnabled: Boolean =
    getOptional[Boolean]("tai.feature.companyCarForceRedirect.enabled").getOrElse(false)
  val cyPlus1EstimatedPayEnabled: Boolean = getOptional[Boolean]("tai.cyPlusOne.enabled").getOrElse(false)
  lazy val accessibilityStatementToggle: Boolean =
    getOptional[Boolean]("accessibility-statement.toggle").getOrElse(false)
  lazy val isTaiCy3Enabled: Boolean = getOptional[Boolean]("tai.cy3.enabled").getOrElse(false)
  val trackingEnabled: Boolean = getOptional[Boolean]("tai.tracking.enabled").getOrElse(false)

  lazy val incomeTaxHistoryEnabled: Boolean = getOptional[Boolean]("tai.incomeTaxHistory.enabled").getOrElse(true)

  val numberOfPreviousYearsToShow: Int = 5 // Always 5 in all configs
  val numberOfPreviousYearsToShowIncomeTaxHistory: Int = 3
}
