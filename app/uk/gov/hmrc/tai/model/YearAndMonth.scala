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
import play.api.libs.json._

final case class YearAndMonth(yearAndMonth: YearMonth)

object YearAndMonth {

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

  def sortYearAndMonth(yearAndMonthList: List[YearAndMonth], firstClaimDate: YearMonth): List[YearAndMonth] = {

    val dateTypeList =
      for (data <- yearAndMonthList if !data.yearAndMonth.isBefore(firstClaimDate))
        yield data

    dateTypeList.sortWith((x, y) => x.yearAndMonth.isBefore(y.yearAndMonth))
  }

}
