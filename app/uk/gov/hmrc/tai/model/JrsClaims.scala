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

package uk.gov.hmrc.tai.model

import play.api.libs.json.Json
import uk.gov.hmrc.tai.config.ApplicationConfig

final case class JrsClaims(employers: List[Employers]) {

  val hasMultipleEmployments: Boolean = employers.size > 1
  val employerMessageKey = if (hasMultipleEmployments) "employers" else "employer"

}

object JrsClaims {

  implicit val formats = Json.format[JrsClaims]

  def apply(appConfig: ApplicationConfig, jrsClaimsData: JrsClaims): Option[JrsClaims] =
    if (jrsClaimsData.employers.nonEmpty)
      Some(JrsClaims(Employers.sortEmployerslist(appConfig, jrsClaimsData.employers)))
    else None

}
