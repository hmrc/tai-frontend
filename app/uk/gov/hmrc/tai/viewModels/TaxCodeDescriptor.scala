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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.{BasisOperation, Week1Month1BasisOperation}
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.urls.Link

import scala.collection.immutable.ListMap

trait TaxCodeDescriptor {

  case class TaxCodeDescription(taxCode: String, basisOperation: BasisOperation, scottishTaxRateBands: Map[String, BigDecimal])

  def describeTaxCode(taxCode: String, basisOperation: BasisOperation, scottishTaxRateBands: Map[String, BigDecimal], year: TaxYear = TaxYear())
              (implicit messages: Messages): ListMap[String, String] = {

    val previousOrCurrent = if (year <= TaxYear().prev) ".prev" else ""

    val explanationRules: Seq[Function1[TaxCodeDescription, ListMap[String, String]]] = Seq(
      scottishTaxCodeExplanation(previousOrCurrent),
      untaxedTaxCodeExplanation(previousOrCurrent),
      fetchTaxCodeExplanation(previousOrCurrent),
      emergencyTaxCodeExplanation(previousOrCurrent)
    )

    val taxDescription = TaxCodeDescription(taxCode, basisOperation, scottishTaxRateBands)
    explanationRules.foldLeft(ListMap[String, String]())((expl, rule) => expl ++ rule(taxDescription))
  }

  private def scottishTaxCodeExplanation(previousOrCurrent: String)(implicit messages: Messages): TaxCodeDescription => ListMap[String, String] = (taxCodeDescription: TaxCodeDescription) => {
    val scottishRegex = "^S".r
    val taxCode = taxCodeDescription.taxCode
    scottishRegex.findFirstIn(taxCode) match {
      case Some(code) => ListMap(code -> Messages(s"tai.taxCode$previousOrCurrent.$code",
        Link.toExternalPage(url = ApplicationConfig.scottishRateIncomeTaxUrl, value=Some(Messages("tai.taxCode.scottishIncomeText.link"))).toHtml))
      case _ => ListMap[String, String]()
    }
  }

  private def untaxedTaxCodeExplanation(previousOrCurrent: String)(implicit messages: Messages): TaxCodeDescription => ListMap[String, String] = (taxCodeDescription: TaxCodeDescription) => {
    val untaxedRegex = "K".r
    val taxCode = taxCodeDescription.taxCode
    untaxedRegex.findFirstIn(taxCode) match {
      case Some(code) =>
        val amount = taxAmount(taxCode)
        val messageAmount = MoneyPounds(amount * TaiConstants.TaxAmountFactor, 0).quantity
        ListMap(code -> Messages(s"tai.taxCode$previousOrCurrent.$code"),
          amount.toString -> Messages(s"tai.taxCode.untaxedAmount", messageAmount))
      case _ => ListMap[String, String]()
    }
  }

  private def standAloneTaxCodeExplanation(previousOrCurrent: String)(implicit messages: Messages): TaxCodeDescription => ListMap[String, String] = (taxCodeDescription: TaxCodeDescription) => {
    val standAloneRegex = "0T|BR|NT".r
    val scottishStandAloneRegex = "D0|D1|D2|D3|D4|D5|D6|D7|D8".r

    val taxCode = taxCodeDescription.taxCode
    (standAloneRegex.findFirstIn(taxCode), scottishStandAloneRegex.findFirstIn(taxCode)) match {
      case (Some(code), None) => ListMap(code -> Messages(s"tai.taxCode$previousOrCurrent.$code"))
      case (None, Some(code)) => ListMap(code -> Messages(s"tai.taxCode$previousOrCurrent.DX", taxCodeDescription.scottishTaxRateBands.getOrElse(code, BigDecimal(0))))
      case _ => ListMap[String, String]()
    }
  }

  private def suffixTaxCodeExplanation(previousOrCurrent: String)(implicit messages: Messages): TaxCodeDescription => ListMap[String, String] = (taxCodeDescription: TaxCodeDescription) => {
    val suffixRegex = """L|M|\d[0-9]*N|\d[1-9]T|\d+0T|[1-9]T""".r
    val taxCode = taxCodeDescription.taxCode
    suffixRegex.findFirstIn(taxCode) match {
      case Some(code) =>
        val amount = taxAmount(taxCode)
        val messageAmount = MoneyPounds(amount * TaiConstants.TaxAmountFactor, 0).quantity
        ListMap(amount.toString -> Messages(s"tai.taxCode$previousOrCurrent.amount", messageAmount),
          code.last.toString -> Messages(s"tai.taxCode$previousOrCurrent.${code.last.toString}"))
      case _ => ListMap[String, String]()
    }
  }

  private def emergencyTaxCodeExplanation(previousOrCurrent: String)(implicit messages: Messages): TaxCodeDescription => ListMap[String, String] = (taxCodeDescription: TaxCodeDescription) => {
    taxCodeDescription.basisOperation match {
      case Week1Month1BasisOperation => ListMap(TaiConstants.EmergencyTaxCode -> Messages(s"tai.taxCode$previousOrCurrent.X"))
      case _ => ListMap[String, String]()
    }
  }

  private def fetchTaxCodeExplanation(previousOrCurrent: String)(implicit messages: Messages): TaxCodeDescription => ListMap[String, String] = (taxCodeDescription: TaxCodeDescription) => {
    val codeExplanation = suffixTaxCodeExplanation(previousOrCurrent)(messages)(taxCodeDescription)

    if (codeExplanation.isEmpty)
      standAloneTaxCodeExplanation(previousOrCurrent)(messages)(taxCodeDescription)
    else
      codeExplanation
  }

  private def taxAmount(taxCode: String): Int = {
    val digitsRegex = """(\d+)""".r
    val amount = digitsRegex.findAllIn(taxCode).toList.headOption
    amount.map(_.toInt).getOrElse(0)
  }
}
