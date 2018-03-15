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
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.time.TaxYearResolver

trait ViewModelHelper {

  def withPoundPrefixAndSign(moneyPounds: MoneyPounds): String = {
    val sign = if (moneyPounds.isNegative) "\u2212" else ""
      s"${sign}£${moneyPounds.quantity}"
  }

  def withPoundPrefix(moneyPounds: MoneyPounds): String = s"£${moneyPounds.quantity}"

  def currentTaxYearHeaderHtmlNonBreak(format: String): String = {
    htmlNonBroken( TaxYearResolver.endOfCurrentTaxYear.toString(format) )
  }

  def nextTaxYearHeaderHtmlNonBreak(format: String): String = {
    htmlNonBroken( TaxYearResolver.startOfNextTaxYear.toString(format) )
  }

  def currentTaxYearRange(format: String): String = {
    Messages("tai.taxYear",
      TaxYearResolver.startOfCurrentTaxYear.toString(format),
      TaxYearResolver.endOfCurrentTaxYear.toString(format))
  }

  def currentTaxYearRangeHtmlNonBreak(format: String): String = {
    Messages("tai.taxYear",
      htmlNonBroken( TaxYearResolver.startOfCurrentTaxYear.toString(format) ),
      htmlNonBroken( TaxYearResolver.endOfCurrentTaxYear.toString(format) ))
  }

  def taxYearRangeHtmlNonBreak(format: String, year: Int): String = {
    Messages("tai.taxYear",
      htmlNonBroken( TaxYearResolver.startOfTaxYear(year).toString(format) ),
      htmlNonBroken( TaxYearResolver.startOfTaxYear(year+1).minusDays(1).toString(format) ))
  }

  def htmlNonBroken(string: String): String = {
    string.replace(" ", "\u00A0")
  }

  def unescapeNonBreakingSpaceOnly(string: String): String = {
    string.replace("&nbsp;", "\u00A0")
  }
}

