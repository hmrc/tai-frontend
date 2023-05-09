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

package controllers.auth

import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.Nino

case class AuthedUser(
  validNino: String,
  utr: Option[String],
  providerType: Option[String],
  confidenceLevel: ConfidenceLevel,
  trustedHelper: Option[TrustedHelper]
) {
  def nino: Nino = Nino(validNino)
}

object AuthedUser {
  def apply(
    nino: Option[String],
    saUtr: Option[String],
    providerType: Option[String],
    confidenceLevel: ConfidenceLevel
  ): AuthedUser = {
    val validNino = nino.getOrElse("")
    AuthedUser(validNino, saUtr, providerType, confidenceLevel, None)
  }

  def apply(
    trustedHelper: TrustedHelper,
    saUtr: Option[String],
    providerType: Option[String],
    confidenceLevel: ConfidenceLevel
  ): AuthedUser =
    AuthedUser(
      trustedHelper.principalNino,
      saUtr,
      providerType,
      confidenceLevel,
      Some(trustedHelper)
    )
}
