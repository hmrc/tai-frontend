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

import java.time.format.DateTimeParseException

import org.joda.time.YearMonth
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.YearAndMonth.dateFormatter

final case class YearAndMonth(yearAndMonth: YearMonth) {

  def formatYearAndMonth: String =
    yearAndMonth.toString(dateFormatter)
}

object YearAndMonth {

  val dateFormatter = DateTimeFormat.forPattern("MMMM yyyy")

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

  private def firstClaimDate(appConfig: ApplicationConfig): YearMonth =
    YearMonth.parse(appConfig.jrsClaimsFromDate)

  def formattedFirstClaimDate(appConfig: ApplicationConfig): String =
    firstClaimDate(appConfig).toString(dateFormatter)
}
