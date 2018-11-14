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
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.income.{BasisOfOperation, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.ListMap


trait TaxCodeDescriptor {

  type TaxCodeDescriptionTranslator = TaxCodeDescription => ListMap[String, String]

  case class TaxCodeDescription(taxCode: String, basisOperation: BasisOfOperation, scottishTaxRateBands: Map[String, BigDecimal])

  def describeTaxCode(taxCode: String,
                      basisOperation: BasisOfOperation,
                      scottishTaxRateBands: Map[String, BigDecimal],
                      isCurrentYear: Boolean = true)
                     (implicit messages: Messages): ListMap[String, String] = {


    val explanationRules: Seq[TaxCodeDescriptionTranslator] = Seq(
      scottishTaxCodeExplanation(isCurrentYear),
      welshTaxCodeExplanation(isCurrentYear),
      untaxedTaxCodeExplanation(isCurrentYear),
      fetchTaxCodeExplanation(isCurrentYear),
      emergencyTaxCodeExplanation(isCurrentYear)
    )

    val taxDescription = TaxCodeDescription(taxCode, basisOperation, scottishTaxRateBands)
    explanationRules.foldLeft(ListMap[String, String]())((expl, rule) => expl ++ rule(taxDescription))
  }

  private def scottishTaxCodeExplanation(isCurrent: Boolean)(implicit messages: Messages): TaxCodeDescriptionTranslator = (taxCodeDescription: TaxCodeDescription) => {
    val previousOrCurrent = if (isCurrent) "" else ".prev"
    val scottishRegex = "^S".r
    val taxCode = taxCodeDescription.taxCode

    scottishRegex.findFirstIn(taxCode) match {
      case Some(code) => ListMap(code -> messages(s"tai.taxCode$previousOrCurrent.$code",
        Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value=Some(messages("tai.taxCode.scottishIncomeText.link"))).toHtml))
      case _ => ListMap[String, String]()
    }
  }

  private def welshTaxCodeExplanation(isCurrent: Boolean)(implicit messages: Messages): TaxCodeDescriptionTranslator = (taxCodeDescription: TaxCodeDescription) => {
    val previousOrCurrent = if (isCurrent) "" else ".prev"
    val welshRegex = "^C".r
    val taxCode = taxCodeDescription.taxCode
    welshRegex.findFirstIn(taxCode) match {
      case Some(code) => ListMap(code -> messages(s"tai.taxCode$previousOrCurrent.$code",
        Link.toExternalPage(url =
          if(messages.lang.language == "cy") {ApplicationConfig.welshRateIncomeTaxWelshUrl} else {ApplicationConfig.welshRateIncomeTaxUrl},
          value=Some(messages("tai.taxCode.welshIncomeText.link"))).toHtml))
      case _ => ListMap[String, String]()
    }
  }

  private def untaxedTaxCodeExplanation(isCurrent: Boolean)(implicit messages: Messages): TaxCodeDescriptionTranslator = (taxCodeDescription: TaxCodeDescription) => {
    val previousOrCurrent = if (isCurrent) "" else ".prev"
    val untaxedRegex = "K".r
    val taxCode = taxCodeDescription.taxCode

    untaxedRegex.findFirstIn(taxCode) match {
      case Some(code) =>
        val amount = taxAmount(taxCode)
        val messageAmount = MoneyPounds(amount * TaiConstants.TaxAmountFactor, 0).quantity
        ListMap(code -> messages(s"tai.taxCode$previousOrCurrent.$code"),
          amount.toString -> messages(s"tai.taxCode.untaxedAmount", messageAmount))
      case _ => ListMap[String, String]()
    }
  }

  private def fetchTaxCodeExplanation(isCurrent: Boolean)(implicit messages: Messages): TaxCodeDescriptionTranslator = (taxCodeDescription: TaxCodeDescription) => {
    val codeExplanation = suffixTaxCodeExplanation(taxCodeDescription, isCurrent)

    if (codeExplanation.isEmpty)
      standAloneTaxCodeExplanation(taxCodeDescription, isCurrent)
    else
      codeExplanation
  }

  private def emergencyTaxCodeExplanation(isCurrent: Boolean)(implicit messages: Messages): TaxCodeDescriptionTranslator = (taxCodeDescription: TaxCodeDescription) => {
    val previousOrCurrent = if (isCurrent) "" else ".prev"

    taxCodeDescription.basisOperation match {
      case Week1Month1BasisOfOperation => ListMap(TaiConstants.EmergencyTaxCode -> messages(s"tai.taxCode$previousOrCurrent.X"))
      case _ => ListMap[String, String]()
    }
  }
  
  private def standAloneTaxCodeExplanation(taxCodeDescription: TaxCodeDescription, isCurrent: Boolean)(implicit messages: Messages): ListMap[String, String] = {
    val previousOrCurrent = if (isCurrent) "" else ".prev"

    val standAloneRegex = "0T|BR|NT".r
    val scottishStandAloneRegex = "D0|D1|D2|D3|D4|D5|D6|D7|D8".r

    val taxCode = taxCodeDescription.taxCode

    (standAloneRegex.findFirstIn(taxCode), scottishStandAloneRegex.findFirstIn(taxCode)) match {
      case (Some(code), None) => ListMap(code -> messages(s"tai.taxCode$previousOrCurrent.$code"))
      case (None, Some(code)) => ListMap(code -> messages(s"tai.taxCode$previousOrCurrent.DX", taxCodeDescription.scottishTaxRateBands.getOrElse(code, BigDecimal(0))))
      case _ => ListMap[String, String]()
    }
  }

  private def suffixTaxCodeExplanation(taxCodeDescription: TaxCodeDescription, isCurrent: Boolean)(implicit messages: Messages): ListMap[String, String] = {
    val previousOrCurrent = if (isCurrent) "" else ".prev"
    val suffixRegex = """L|M|\d[0-9]*N|\d[1-9]T|\d+0T|[1-9]T""".r
    val taxCode = taxCodeDescription.taxCode

    suffixRegex.findFirstIn(taxCode) match {
      case Some(code) =>
        val amount = taxAmount(taxCode)
        val messageAmount = MoneyPounds(amount * TaiConstants.TaxAmountFactor, 0).quantity
        ListMap(amount.toString -> messages(s"tai.taxCode$previousOrCurrent.amount", messageAmount),
          code.last.toString -> messages(s"tai.taxCode$previousOrCurrent.${code.last.toString}"))
      case _ => ListMap[String, String]()
    }
  }
  
  private def taxAmount(taxCode: String): Int = {
    val digitsRegex = """(\d+)""".r
    val amount = digitsRegex.findAllIn(taxCode).toList.headOption
    amount.map(_.toInt).getOrElse(0)
  }
}
