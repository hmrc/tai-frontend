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

package uk.gov.hmrc.tai.model

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tai.config.ApplicationConfig

final case class Employers(name: String, employerReference: String, claims: List[YearAndMonth]) {

  val hasMultipleClaims: Boolean = claims.size > 1

}

object Employers {

  implicit val formats: OFormat[Employers] = Json.format[Employers]

  def sortEmployerslist(appConfig: ApplicationConfig, employers: List[Employers]): List[Employers] = {

    val employersList = employers.map(employer =>
      Employers(employer.name, employer.employerReference, YearAndMonth.sortYearAndMonth(employer.claims, appConfig)))

    employersList.sortBy(_.name)
  }
}
