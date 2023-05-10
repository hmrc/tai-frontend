/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util.constants

object BandTypesConstants {
  val UkBands: String = "uk.bandtype" // not used
  val ScottishBands: String = "scottish.bandtype" // not used
  val TaxFreeAllowanceBand: String = "pa"
  val StarterSavingsRate: String = "SR"
  val PersonalSavingsRate: String = "PSR"
  val SavingsBasicRate: String = "LSR" // not used
  val SavingsHigherRate: String = "HSR1"
  val SavingsAdditionalRate: String = "HSR2"
  val DividendZeroRate: String = "SDR"
  val DividendBasicRate: String = "LDR" // not used
  val DividendHigherRate: String = "HDR1"
  val DividendAdditionalRate: String = "HDR2" // not used
  val ZeroBand: String = "ZeroBand"
  val NonZeroBand: String = "NonZeroBand"
  val TaxGraph: String = "taxGraph"
  val TaxFree: String = "TaxFree"
  val BasicRate: String = "B" // not used
}
