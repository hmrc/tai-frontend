/*
 * Copyright 2022 HM Revenue & Customs
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

package builders

import controllers.auth.AuthedUser
import org.joda.time.DateTime
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.LoginTimes
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.util.constants.TaiConstants

object UserBuilder {
  val nino: Nino = new Generator().nextNino

  def apply(utr: String = "utr", providerType: String = TaiConstants.AuthProviderGG) =
    AuthedUser(
      nino.toString(),
      Some(utr),
      Some(providerType),
      ConfidenceLevel.L200,
      None,
      LoginTimes(DateTime.now(), Some(DateTime.now().minusDays(7))))

  def apply(utr: String, providerType: String, principalName: String, previousLogin: Option[DateTime]) =
    AuthedUser(
      nino.toString(),
      Some(utr),
      Some(providerType),
      ConfidenceLevel.L200,
      Some(TrustedHelper(principalName, "attorneyName", "returnLinkUrl", nino.toString())),
      LoginTimes(DateTime.now(), previousLogin)
    )
}
