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

import java.net.URLEncoder

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.time.TaxYearResolver

import scala.util.Try

trait ViewModelHelper {

  def withPoundPrefixAndSign(moneyPounds: MoneyPounds): String = {
   MonetaryUtil.withPoundPrefixAndSign(moneyPounds)
  }

  def withPoundPrefix(moneyPounds: MoneyPounds): String = MonetaryUtil.withPoundPrefix(moneyPounds)

  def currentTaxYearHeaderHtmlNonBreak(implicit messages: Messages): String = {
    htmlNonBroken( Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear) )
  }

  def nextTaxYearHeaderHtmlNonBreak(implicit messages: Messages): String = {
    htmlNonBroken( Dates.formatDate(TaxYearResolver.startOfNextTaxYear) )
  }

  def currentTaxYearRangeHtmlNonBreak(implicit messages: Messages): String = {
    TaxYearRangeUtil.currentTaxYearRange
  }

  def currentTaxYearRangeHtmlNonBreakBetween(implicit messages: Messages): String = {
    TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited
  }

  def dynamicDateRangeHtmlNonBreak(from:LocalDate, to:LocalDate)(implicit messages: Messages): String = {
    TaxYearRangeUtil.dynamicDateRange(from, to)
  }

  def htmlNonBroken(string: String) = HtmlFormatter.htmlNonBroken(string)

  def isTrue(str: String): Boolean = Try(str.toBoolean).getOrElse(false)

  def urlEncode(u: String) = URLEncoder.encode(u, "UTF-8")
}

object ViewModelHelper extends ViewModelHelper
