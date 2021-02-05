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

import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import uk.gov.hmrc.tai.config.ApplicationConfig

case class JrsClaims(employers: List[Employers]) {

  val fmt = DateTimeFormat.forPattern("MMMM, yyyy")

  def sortEmployerslist(appConfig: ApplicationConfig): JrsClaims = {

    val employersList = for (employer <- employers)
      yield
        Employers(employer.name, employer.employerReference, sortClaimData(employer.claims, firstClaimDate(appConfig)))

    JrsClaims(employersList.sortBy(_.name))
  }

  def sortClaimData(yearAndMonthList: List[YearAndMonth], firstClaimDate: YearMonth): List[YearAndMonth] = {

    val dateTypeList =
      for (data <- yearAndMonthList if !data.yearAndMonth.isBefore(firstClaimDate))
        yield data

    dateTypeList.sortWith((x, y) => x.yearAndMonth.isBefore(y.yearAndMonth))
  }

  def firstClaimDate(appConfig: ApplicationConfig): YearMonth =
    YearMonth.parse(appConfig.jrsClaimsFromDate)
}

object JrsClaims {

  implicit val formats = Json.format[JrsClaims]

}
