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

package uk.gov.hmrc.tai.service

import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.time.TaxYearResolver


trait TaxPeriodLabelService {

  def taxYearPrefix(implicit messages: Messages) = messages("tai.taxYearHeading") + " "


  def shortFormCurrentTaxPeriodLabel(implicit messages: Messages) : String = {
    taxYearPrefix + TaxYearResolver.startOfCurrentTaxYear.toString("yy") + "/" + TaxYearResolver.endOfCurrentTaxYear.toString("yy")
  }

  def shortFormCurrentYearMinus1TaxPeriodLabel(implicit messages: Messages) : String = {
    taxYearPrefix + TaxYearResolver.startOfCurrentTaxYear.minusYears(1).toString("yy") + "/" + TaxYearResolver.endOfCurrentTaxYear.minusYears(1).toString("yy")
  }

  def shortFormCurrentYearMinus2TaxPeriodLabel(implicit messages: Messages) : String = {
    taxYearPrefix + TaxYearResolver.startOfCurrentTaxYear.minusYears(2).toString("yy") + "/" + TaxYearResolver.endOfCurrentTaxYear.minusYears(2).toString("yy")
  }

  def longFormCurrentTaxPeriodLabel(implicit messages: Messages) : String = {
    Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear) + " " + messages("language.to") + " " + Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)
  }

  def longFormCurrentYearMinus1TaxPeriodLabel(implicit messages: Messages) : String = {
    Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear.minusYears(1)) + " " + messages("language.to") + " " + Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear.minusYears(1))
  }

  def longFormCurrentYearMinus2TaxPeriodLabel(implicit messages: Messages) : String = {
    Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear.minusYears(2)) + " " + messages("language.to") + " " + Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear.minusYears(2))
  }

  def taxPeriodLabel(year: Int)(implicit messages: Messages) : String = {
    Dates.formatDate(TaxYearResolver.startOfTaxYear(year)) + " " + messages("language.to") + " " + Dates.formatDate(TaxYearResolver.startOfTaxYear( year + 1).minusDays(1))
  }
}

object TaxPeriodLabelService extends TaxPeriodLabelService
