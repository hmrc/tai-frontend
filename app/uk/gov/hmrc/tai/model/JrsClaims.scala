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

import play.api.libs.json.{Format, JsPath, Json, Reads}

case class YearAndMonth(yearAndMonth: String)

object YearAndMonth {

  def getMonthNameAndYear(monthNumber: Array[String]): YearAndMonth = {
    val months = Array(
      "January",
      "February",
      "March",
      "April",
      "May",
      "June",
      "July",
      "August",
      "September",
      "October",
      "November",
      "December")
    YearAndMonth(s"${months(monthNumber(1).toInt - 1)} ${monthNumber(0)}")
  }

  implicit val reads: Reads[YearAndMonth] =
    (JsPath \ "yearAndMonth").read[String].map(a => getMonthNameAndYear(a.split("-")))
  (YearAndMonth.apply _)

  implicit val writes = Json.writes[YearAndMonth]
}

case class Employers(name: String, employerReference: String, claims: List[YearAndMonth])

object Employers {
  implicit lazy val format: Format[Employers] = Json.format[Employers]
}

case class JrsClaims(employers: List[Employers])

object JrsClaims {
  implicit lazy val format: Format[JrsClaims] = Json.format[JrsClaims]
}
