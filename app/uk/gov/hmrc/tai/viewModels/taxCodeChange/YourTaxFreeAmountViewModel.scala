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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.CodingComponentPairModel
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.TaxFreeInfo

case class YourTaxFreeAmountViewModel(
  previousTaxFreeInfo: Option[TaxFreeInfo],
  currentTaxFreeInfo: TaxFreeInfo,
  allowances: Seq[CodingComponentPairModel],
  deductions: Seq[CodingComponentPairModel]) {

  val showPreviousColumn: Boolean = previousTaxFreeInfo.isDefined
  val columns: Int = {
    if (previousTaxFreeInfo.isDefined) {
      3
    } else {
      2
    }
  }
}

object YourTaxFreeAmountViewModel extends ViewModelHelper {
  def prettyPrint(value: BigDecimal): String =
    withPoundPrefixAndSign(MoneyPounds(value, 0))

  def totalPrevious(sequence: Seq[CodingComponentPairModel]): String = {
    val total = sequence.map(_.previous).sum
    prettyPrint(total)
  }

  def totalCurrent(sequence: Seq[CodingComponentPairModel]): String = {
    val total = sequence.map(_.current).sum
    prettyPrint(total)
  }

  val additionsTranslationMap: Map[String, String] = {
    Map(
      "title"      -> "tai.taxFreeAmount.table.additions.caption",
      "totalTitle" -> "tai.taxFreeAmount.table.additions.total",
      "noItems"    -> "tai.taxFreeAmount.table.additions.noAddition"
    )
  }

  val deductionsTranslationMap: Map[String, String] = {
    Map(
      "title"      -> "tai.taxFreeAmount.table.deductions.caption",
      "totalTitle" -> "tai.taxFreeAmount.table.deductions.total",
      "noItems"    -> "tai.taxFreeAmount.table.deductions.noDeduction"
    )
  }
}
