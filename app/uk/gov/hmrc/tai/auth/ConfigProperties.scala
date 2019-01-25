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

package uk.gov.hmrc.tai.auth

import play.api.Play
import uk.gov.hmrc.tai.config.DefaultRunMode

object ConfigProperties extends DefaultRunMode {

  import play.api.Play.current

  val postSignInRedirectUrl: Option[String] = Play.configuration.getString(s"govuk-tax.$env.login-callback.url")

  val activatePaperless: Boolean = Play.configuration.getBoolean(s"govuk-tax.$env.activatePaperless")
    .getOrElse(throw new IllegalStateException(s"Could not find configuration for govuk-tax.$env.activatePaperless"))

  val activatePaperlessEvenIfGatekeeperFails: Boolean = Play.configuration.getBoolean(s"govuk-tax.$env.activatePaperlessEvenIfGatekeeperFails")
    .getOrElse(throw new IllegalStateException(s"Could not find configuration for govuk-tax.$env.activatePaperless"))

  val taxPlatformTaiRootUri: String = Play.configuration.getString(s"govuk-tax.$env.taxPlatformTaiRootUri").getOrElse("http://noConfigTaiRootUri")
}
