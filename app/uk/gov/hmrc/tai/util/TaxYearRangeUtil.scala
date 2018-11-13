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

object TaxYearRangeUtil {

  private val messageRangeKeyBetween = "tai.taxYear.between"
  private val messageRangeKeyFromAndTo = "tai.taxYear"

  def dynamicDateRange(from:LocalDate, to:LocalDate)(implicit messages: Messages): String = {
    dateRange(messageRangeKeyFromAndTo, from, to)
  }

  def currentTaxYearRange(implicit messages: Messages): String = {
    dateRange(messageRangeKeyFromAndTo, TaxYearResolver.startOfCurrentTaxYear, TaxYearResolver.endOfCurrentTaxYear)
  }

  def currentTaxYearRangeSingleLine(implicit messages: Messages): String = {
    dateRangeSingleLine(messageRangeKeyFromAndTo, TaxYearResolver.startOfCurrentTaxYear, TaxYearResolver.endOfCurrentTaxYear)
  }

  def currentTaxYearRangeBetweenDelimited(implicit messages: Messages): String = {
    dateRange(messageRangeKeyBetween, TaxYearResolver.startOfCurrentTaxYear, TaxYearResolver.endOfCurrentTaxYear)
  }

  def currentTaxYearRangeSingleLineBetweenDelimited(implicit messages: Messages): String = {
    dateRangeSingleLine(messageRangeKeyBetween, TaxYearResolver.startOfCurrentTaxYear, TaxYearResolver.endOfCurrentTaxYear)
  }

  def currentTaxYearRangeYearOnly(implicit messages: Messages): String = {
    val start = TaxYear().start.toString("yyyy")
    val end = TaxYear().end.toString("yyyy")

    messages("tai.taxYear", start, end)
  }

  private def dateRangeSingleLine(messageKey: String, from:LocalDate, to:LocalDate)(implicit messages: Messages): String = {
    HtmlFormatter.htmlNonBroken(dateRange(messageKey,from, to))
  }

  private def dateRange(messageKey: String, from:LocalDate, to:LocalDate)(implicit messages: Messages): String = {
    if(from isAfter to) {
      throw new IllegalArgumentException(s"From date:$from cannot be after To date:$to")
    } else {
      messages(messageKey,
        HtmlFormatter.htmlNonBroken(Dates.formatDate(from)),
        HtmlFormatter.htmlNonBroken(Dates.formatDate(to)))
    }
  }
}