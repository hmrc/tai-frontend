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

import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.Nino

final case class AuthedUser(
  nino: Nino,
  utr: Option[String],
  trustedHelper: Option[TrustedHelper]
)

object AuthedUser {
  def apply(nino: Nino, trustedHelper: TrustedHelper, saUtr: Option[String]): AuthedUser =
    new AuthedUser(
      nino = Nino(trustedHelper.principalNino.getOrElse(nino.nino)),
      utr = saUtr,
      trustedHelper = Some(trustedHelper)
    )
}
