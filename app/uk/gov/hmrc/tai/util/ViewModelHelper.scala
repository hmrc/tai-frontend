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

import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.time.TaxYearResolver
import TaiConstants.encodedMinusSign
import uk.gov.hmrc.play.language.LanguageUtils.Dates

trait ViewModelHelper {

  def withPoundPrefixAndSign(moneyPounds: MoneyPounds): String = {
    val sign = if (moneyPounds.isNegative) encodedMinusSign else ""
      s"${sign}£${moneyPounds.quantity}"
  }

  def withPoundPrefix(moneyPounds: MoneyPounds): String = s"£${moneyPounds.quantity}"

  //used without year!
  def currentTaxYearHeaderHtmlNonBreak(format: String): String = {
    htmlNonBroken( TaxYearResolver.endOfCurrentTaxYear.toString(format) )
  }

  //used without year!
  def nextTaxYearHeaderHtmlNonBreak(format: String): String = {
    htmlNonBroken( TaxYearResolver.startOfNextTaxYear.toString(format) )
  }

  def currentTaxYearRange(implicit messages: Messages): String = {
    messages("tai.taxYear",
      Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear),
      Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear))
  }

  def currentTaxYearRangeHtmlNonBreak(implicit messages: Messages): String = {
    messages("tai.taxYear",
      htmlNonBroken( Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear) ),
      htmlNonBroken( Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear) ))
  }

  def htmlNonBroken(string: String): String = {
    string.replace(" ", "\u00A0")
  }

  def unescapeNonBreakingSpaceOnly(string: String): String = {
    string.replace("&nbsp;", "\u00A0")
  }
}

