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

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.tai.config.ApplicationConfig

case class YearAndMonth(yearAndMonth: String)

object YearAndMonth {

  implicit val formats = Json.format[YearAndMonth]

}

case class Employers(name: String, employerReference: String, claims: List[YearAndMonth])

object Employers {

  implicit val formats = Json.format[Employers]

}

case class JrsClaims(employers: List[Employers]) {

  def sortEmployerslist(appConfig: ApplicationConfig): JrsClaims = {

    val employersList = for (employer <- employers)
      yield
        (Employers(
          employer.name,
          employer.employerReference,
          sortClaimData(employer.claims, LocalDate.parse(appConfig.jrsClaimsFromDate))))

    JrsClaims(employersList.sortBy(_.name))
  }

  def sortClaimData(yearAndMonthList: List[YearAndMonth], firstClaimDate: LocalDate) = {

    val dateTypeList =
      for (data <- yearAndMonthList if !LocalDate.parse(data.yearAndMonth).isBefore(firstClaimDate))
        yield (LocalDate.parse(data.yearAndMonth))

    val sortedDateTypeList = dateTypeList.sortBy(_.toDate)

    for (date <- sortedDateTypeList)
      yield (YearAndMonth(s"${date.monthOfYear().getAsText} ${date.getYear}"))
  }

  def firstClaimDate(appConfig: ApplicationConfig) = {
    val date = LocalDate.parse(appConfig.jrsClaimsFromDate)
    s"${date.monthOfYear().getAsText} ${date.getYear}"
  }
}

object JrsClaims {

  implicit val formats = Json.format[JrsClaims]

}
