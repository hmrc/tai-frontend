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

package uk.gov.hmrc.tai.model

import java.time.format.DateTimeFormatter
import java.time.YearMonth
import play.api.i18n.Lang
import play.api.libs.json._
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.YearAndMonth.{MonthNamesInWelsh, dateFormatter, formattedDate}

import java.time.format.DateTimeParseException

final case class YearAndMonth(yearAndMonth: YearMonth) {

  def formatYearAndMonth(lang: Lang): String =
    formattedDate(yearAndMonth, lang)
}

object YearAndMonth {

  val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

  val MonthNamesInWelsh = Map(
    1  -> "Ionawr",
    2  -> "Chwefror",
    3  -> "Mawrth",
    4  -> "Ebrill",
    5  -> "Mai",
    6  -> "Mehefin",
    7  -> "Gorffennaf",
    8  -> "Awst",
    9  -> "Medi",
    10 -> "Hydref",
    11 -> "Tachwedd",
    12 -> "Rhagfyr"
  )

  def apply(yearAndMonth: String): YearAndMonth = YearAndMonth(YearMonth.parse(yearAndMonth))

  implicit val yearMonthFormat = new Format[YearMonth] {
    override def writes(o: YearMonth): JsValue = JsString(o.toString())

    override def reads(json: JsValue): JsResult[YearMonth] = json match {
      case JsString(s) =>
        try {
          JsSuccess(YearMonth.parse(s))
        } catch {
          case _: DateTimeParseException => JsError("Invalid date parsed")
        }
    }
  }

  implicit val formats = Json.format[YearAndMonth]

  def sortYearAndMonth(yearAndMonthList: List[YearAndMonth], appConfig: ApplicationConfig): List[YearAndMonth] = {

    val dateTypeList = yearAndMonthList.filter(!_.yearAndMonth.isBefore(firstClaimDate(appConfig)))

    dateTypeList.sortWith((x, y) => x.yearAndMonth.isBefore(y.yearAndMonth))
  }

  def firstClaimDate(appConfig: ApplicationConfig): YearMonth =
    YearMonth.parse(appConfig.jrsClaimsFromDate)

  def formattedDate(yearAndMonth: YearMonth, lang: Lang): String =
    if (lang == Lang("cy")) {
      val month = MonthNamesInWelsh(yearAndMonth.getMonthValue)
      month + " " + yearAndMonth.getYear
    } else {
      yearAndMonth.format(dateFormatter)
    }
}
