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

package uk.gov.hmrc.tai.viewModels

import hmrc.nps2.{TaxBand, TaxObject}
import uk.gov.hmrc.tai.model.TaxSummaryDetails
import uk.gov.hmrc.tai.util.{BandTypesConstants, DateFormatConstants, ViewModelHelper}

import scala.language.postfixOps

case class TaxExplanationViewModel(nonSavings: Seq[TaxBand], savings: Seq[TaxBand], dividends: Seq[TaxBand], bandType: String)
  extends ViewModelHelper with DateFormatConstants {
  val totalTax: BigDecimal = List(nonSavings, savings, dividends).flatten.map(_.tax).sum
}


object TaxExplanationViewModel extends BandTypesConstants {
  def apply(taxSummaryDetails: TaxSummaryDetails, scottishTaxRateEnabled: Boolean): TaxExplanationViewModel = {
    val taxObjects = taxSummaryDetails.currentYearAccounts.flatMap(_.nps).map(_.taxObjects)

    val nonSavings = List(
      TaxObject.Type.NonSavings
    ) map (taxObjectType => taxObjects get taxObjectType) flatMap (_.taxBands) flatten

    val savings = List(
      TaxObject.Type.UntaxedInterest,
      TaxObject.Type.BankInterest,
      TaxObject.Type.ForeignInterest
    ) map (taxObjectType => taxObjects get taxObjectType) flatMap (_.taxBands) flatten

    val dividends = List(
      TaxObject.Type.UkDividends,
      TaxObject.Type.ForeignDividends
    ) map (taxObjectType => taxObjects get taxObjectType) flatMap (_.taxBands) flatten

    val nonZeroNonSavings = nonSavings.filterNot(_.rate == 0)
    val nonZeroSavings = savings.filterNot(_.rate == 0)
    val nonZeroDividends = dividends.filterNot(_.rate == 0)

    val bandType = {
      for {
        taxCodeDetails <- taxSummaryDetails.taxCodeDetails
        employments <- taxCodeDetails.employment
      } yield {
        if(employments.exists(_.taxCode.getOrElse("").startsWith("S")) && scottishTaxRateEnabled){
          ScottishBands
        } else {
          UkBands
        }
      }
    } getOrElse UkBands

    TaxExplanationViewModel(nonZeroNonSavings, nonZeroSavings, nonZeroDividends, bandType)
  }
}