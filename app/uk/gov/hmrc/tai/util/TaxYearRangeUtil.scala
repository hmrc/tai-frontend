/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.time.TaxYearResolver

import scala.annotation.tailrec

object TaxYearRangeUtil {

  def currentTaxYearRange(implicit messages: Messages): String = {
    messages("tai.taxYear",
      Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear),
      Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear))
  }

  def currentTaxYearRangeSingleLine(implicit messages: Messages): String = {
    HtmlFormatter.htmlNonBroken(currentTaxYearRange)
  }

  def currentTaxYearRangeHtmlNonBreak(implicit messages: Messages): String = futureTaxYearRangeHtmlNonBreak(0)

  def futureTaxYearRangeHtmlNonBreak(add: Int)(implicit messages: Messages): String = {
    @tailrec
    def getYear(n: Int, year: TaxYear = TaxYear()): TaxYear = {
      if (n == 0) {
        year
      } else {
        getYear(n-1, year.next)
      }
    }

    val year: TaxYear = getYear(add)

    val st: LocalDate = TaxYearResolver.startOfCurrentTaxYear

    messages("tai.taxYear",
      HtmlFormatter.htmlNonBroken( Dates.formatDate(year.start) ),
      HtmlFormatter.htmlNonBroken( Dates.formatDate(year.end) ))
  }

  def dynamicDateRangeHtmlNonBreak(from:LocalDate, to:LocalDate)(implicit messages: Messages): String = {
    if(from isAfter to) {
      throw new IllegalArgumentException(s"From date:$from cannot be after To date:$to")
    } else {
      messages("tai.taxYear",
        HtmlFormatter.htmlNonBroken(Dates.formatDate(from)),
        HtmlFormatter.htmlNonBroken(Dates.formatDate(to)))
    }
  }

  def taxYearYearRange(year: TaxYear = TaxYear())(implicit messages: Messages): String = {
    val start = year.start.toString("yyyy")
    val end = year.end.toString("yyyy")

    messages("tai.taxYear", start, end)
  }
}
