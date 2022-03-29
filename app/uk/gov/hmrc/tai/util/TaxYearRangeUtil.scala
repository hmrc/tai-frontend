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

package uk.gov.hmrc.tai.util

import java.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxYear
import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.{TimeZone, ULocale}

import java.time.format.DateTimeFormatter
import java.util.Date
object TaxYearRangeUtil {

  private def toDate(date: LocalDate): java.util.Date = java.sql.Date.valueOf(date)

  private val messageRangeKeyBetween = "tai.taxYear.between"
  private val messageRangeKeyFromAndTo = "tai.taxYear"

  def dynamicDateRange(from: LocalDate, to: LocalDate)(implicit messages: Messages): String =
    dateRange(messageRangeKeyFromAndTo, from, to)

  def currentTaxYearRange(implicit messages: Messages): String =
    dateRange(messageRangeKeyFromAndTo, TaxYear().start, TaxYear().end)

  def currentTaxYearRangeBetweenDelimited(implicit messages: Messages): String =
    dateRange(messageRangeKeyBetween, TaxYear().start, TaxYear().end)

  def futureTaxYearRange(yearsFromNow: Int)(implicit messages: Messages): String = {

    val year: TaxYear = TaxYear.fromNow(yearsFromNow)

    dateRange(messageRangeKeyFromAndTo, year.start, year.end)

  }

  def currentTaxYearRangeYearOnly(implicit messages: Messages): String = {
    val start = TaxYear().start.getYear
    val end = TaxYear().end.getYear

    messages("tai.taxYear", start, end)
  }

  private def createDateFormatForPattern(pattern: String)(implicit messages: Messages): SimpleDateFormat = {
    val uLocale = new ULocale(messages.lang.code)
    val validLang: Boolean = ULocale.getAvailableLocales.contains(uLocale)
    val locale: ULocale = if (validLang) uLocale else ULocale.getDefault
    val sdf = new SimpleDateFormat(pattern, locale)
    sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"))
    sdf
  }

  private def dateRange(messageKey: String, from: LocalDate, to: LocalDate)(implicit messages: Messages): String =
    if (from isAfter to) {
      throw new IllegalArgumentException(s"From date:$from cannot be after To date:$to")
    } else {
      messages(
        messageKey,
        HtmlFormatter.htmlNonBroken(createDateFormatForPattern("d MMMM y").format(toDate(from))),
        HtmlFormatter.htmlNonBroken(createDateFormatForPattern("d MMMM y").format(toDate(to)))
      )
    }

  def formatDate(date: LocalDate)(implicit messages: Messages): String =
    createDateFormatForPattern("d MMMM y").format(toDate(date))

  def formatDateAbbrMonth(date: LocalDate)(implicit messages: Messages): String =
    createDateFormatForPattern("d MMM y").format(toDate(date))
}
