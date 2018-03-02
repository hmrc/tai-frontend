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

import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.time.TaxYearResolver


trait TaxPeriodLabelService {

  lazy val taxYearPrefix = Messages("tai.taxYearHeading") + " "


  def shortFormCurrentTaxPeriodLabel : String = {
    taxYearPrefix + TaxYearResolver.startOfCurrentTaxYear.toString("yy") + "/" + TaxYearResolver.endOfCurrentTaxYear.toString("yy")
  }

  def shortFormCurrentYearMinus1TaxPeriodLabel : String = {
    taxYearPrefix + TaxYearResolver.startOfCurrentTaxYear.minusYears(1).toString("yy") + "/" + TaxYearResolver.endOfCurrentTaxYear.minusYears(1).toString("yy")
  }

  def shortFormCurrentYearMinus2TaxPeriodLabel : String = {
    taxYearPrefix + TaxYearResolver.startOfCurrentTaxYear.minusYears(2).toString("yy") + "/" + TaxYearResolver.endOfCurrentTaxYear.minusYears(2).toString("yy")
  }

  def longFormCurrentTaxPeriodLabel : String = {
    TaxYearResolver.startOfCurrentTaxYear.toString("d MMMM yyyy") + " to " + TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM yyyy")
  }

  def longFormCurrentYearMinus1TaxPeriodLabel : String = {
    TaxYearResolver.startOfCurrentTaxYear.minusYears(1).toString("d MMMM yyyy") + " to " + TaxYearResolver.endOfCurrentTaxYear.minusYears(1).toString("d MMMM yyyy")
  }

  def longFormCurrentYearMinus2TaxPeriodLabel : String = {
    TaxYearResolver.startOfCurrentTaxYear.minusYears(2).toString("d MMMM yyyy") + " to " + TaxYearResolver.endOfCurrentTaxYear.minusYears(2).toString("d MMMM yyyy")
  }

  def taxPeriodLabel(year: Int) : String = {
    TaxYearResolver.startOfTaxYear(year).toString("d MMMM yyyy") + " to " + TaxYearResolver.startOfTaxYear( year + 1).minusDays(1).toString("d MMMM yyyy")
  }
}

object TaxPeriodLabelService extends TaxPeriodLabelService
