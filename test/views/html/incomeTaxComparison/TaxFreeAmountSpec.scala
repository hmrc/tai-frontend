/*
 * Copyright 2020 HM Revenue & Customs
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

package views.html.incomeTaxComparison

import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.util.{HtmlFormatter, MonetaryUtil, ViewModelHelper}
import uk.gov.hmrc.tai.viewModels._

class TaxFreeAmountSpec extends TaiViewSpec with ViewModelHelper {
  "Tax free amount comparision view" must {

    val taxYearEnds = "Current tax year ends " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().end))
    val taxYearStarts = "Next tax year from " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().next.start))

    "display heading" in {
      doc must haveHeadingH2WithText(messages("tai.incomeTaxComparison.taxFreeAmount.subHeading"))
    }

    "display personal allowance increase message when CY+1 PA is greater than CY PA" in {
      val startOfNextTaxYear = HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().next.start))
      val PA_CY_PLUS_ONE_INDEX = 1
      val personalAllowanceCYPlusOneAmount =
        MonetaryUtil.withPoundPrefixAndSign(MoneyPounds(model.personalAllowance.values(PA_CY_PLUS_ONE_INDEX), 0))
      doc must haveStrongWithText(
        messages(
          "tai.incomeTaxComparison.taxFreeAmount.PA.information1",
          personalAllowanceCYPlusOneAmount,
          startOfNextTaxYear))

    }

    "display paragraph" in {
      doc must haveParagraphWithText(messages("tai.incomeTaxComparison.taxFreeAmount.description"))
      doc must haveParagraphWithText(messages("tai.incomeTaxComparison.taxFreeAmount.PA.information2"))
    }

    "display  personal allowance table" in {
      doc must haveTdWithText(messages("tai.income.personalAllowance"))
      doc must haveTdWithText(taxYearEnds + " £11,500")
      doc must haveTdWithText(taxYearStarts + " £11,850")
    }

    "display additions table" in {
      doc must haveCaptionWithText(messages("tai.incomeTaxComparison.taxFreeAmount.additions.caption"))
      doc must haveTdWithText(messages("tai.taxFreeAmount.table.taxComponent.GiftAidPayments"))
      doc must haveTdWithText(taxYearEnds + " £1,000")
      doc must haveTdWithText(taxYearStarts + " £1,100")

      doc must haveTdWithText(messages("tai.taxFreeAmount.table.taxComponent.PartTimeEarnings"))
      doc must haveTdWithText(taxYearStarts + " not applicable")
    }

    "display deductions table" in {
      doc must haveCaptionWithText(messages("tai.incomeTaxComparison.taxFreeAmount.deductions.caption"))
      doc must haveTdWithText(messages("tai.taxFreeAmount.table.taxComponent.OtherEarnings"))
      doc must haveTdWithText(taxYearEnds + " £1,000")
      doc must haveTdWithText(taxYearStarts + " £1,100")

      doc must haveTdWithText(messages("tai.taxFreeAmount.table.taxComponent.CasualEarnings"))
      doc must haveTdWithText(taxYearEnds + " not applicable")
      doc must haveTdWithText(taxYearStarts + " £1,100")

    }

    "display total table" in {
      doc must haveTdWithText(messages("tai.incomeTaxComparison.taxFreeAmount.totalTFA"))
      doc must haveTdWithText("Current tax year £3,000")
      doc must haveTdWithText("Next tax year £3,300")
    }

    "display no additions details" when {
      "there are no additions" in {
        val docWithOutAdditions = doc(viewWithoutAdditionAndDeductions)

        docWithOutAdditions must haveTdWithText(messages("tai.incomeTaxComparison.taxFreeAmount.noAdditions"))
        docWithOutAdditions must haveTdWithText(taxYearEnds + " £0")
        docWithOutAdditions must haveTdWithText(taxYearStarts + " £0")

      }
    }

    "display no deductions details" when {
      "there are no deductions" in {
        val docWithOutDeductions = doc(viewWithoutAdditionAndDeductions)

        docWithOutDeductions must haveTdWithText(messages("tai.incomeTaxComparison.taxFreeAmount.noDeductions"))
        docWithOutDeductions must haveTdWithText(taxYearEnds + " £0")
        docWithOutDeductions must haveTdWithText(taxYearStarts + " £0")
      }
    }
  }

  private lazy val personalAllowance = PersonalAllowance(Seq(11500, 11850))
  private lazy val additionsRow1 = Row("GiftAidPayments", Seq(Some(1000), Some(1100)))
  private lazy val additionsRow2 = Row("PartTimeEarnings", Seq(Some(1000), None))
  private lazy val additions = Additions(Seq(additionsRow1, additionsRow2), Total(Seq(2000, 1100)))

  private lazy val deductionsRow1 = Row("OtherEarnings", Seq(Some(1000), Some(1100)))
  private lazy val deductionsRow2 = Row("CasualEarnings", Seq(None, Some(1100)))
  private lazy val deductions = Deductions(Seq(deductionsRow1, deductionsRow2), Total(Seq(1000, 2200)))

  private lazy val footer = Footer(Seq(3000, 3300))

  private lazy val model = TaxFreeAmountComparisonViewModel(personalAllowance, additions, deductions, footer)
  private lazy val modelWithOutAdditionsAndDeductions =
    TaxFreeAmountComparisonViewModel(personalAllowance, Additions(Nil, Total(Nil)), Deductions(Nil, Total(Nil)), footer)

  override def view: Html = views.html.incomeTaxComparison.TaxFreeAmount(model)
  def viewWithoutAdditionAndDeductions: Html =
    views.html.incomeTaxComparison.TaxFreeAmount(modelWithOutAdditionsAndDeductions)
}
