/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{IncomeSourceComparisonDetail, IncomeSourceComparisonViewModel}
import uk.gov.hmrc.play.language.LanguageUtils.Dates

class IncomeSummarySpec extends TaiViewSpec {

  "Cy plus one income summary view" must {

    "have income from employment header" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmployment"))
    }

    "display income from employment description" in{
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.incomeTax.fromEmployer.description"))
    }


    "display employment income summary information" in{
      val document = doc(view)

      document must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYear().end.toString("d MMMM YYYY")))}")

      document must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYear().next.start.toString("d MMMM YYYY")))}")


      document must haveTdWithText(employerNameHeading + employmentOneIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + employmentOneIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + employmentOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(view, employmentTwoIncomeSourceDetail)

      document must haveTdWithText(employerNameHeading + employmentTwoIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + employmentTwoIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + employmentTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(view, employmentTwoIncomeSourceDetail)
    }

    "have income from pension header" in {
      doc(viewPensionsOnly) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromPrivatePensions"))
    }


    "display pensions income summary information" in{
      val document = doc(viewPensionsOnly)

      document must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYear().end.toString("d MMMM YYYY")))}")

      document must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYear().next.start.toString("d MMMM YYYY")))}")

      document must haveTdWithText(pensionNameHeading + pensionOneIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + pensionOneIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + pensionOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewPensionsOnly, pensionOneIncomeSourceDetail)

      document must haveTdWithText(pensionNameHeading + pensionTwoIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + pensionTwoIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + pensionTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewPensionsOnly, pensionTwoIncomeSourceDetail)

    }

    "have income and employment pension header" in {
      doc(viewCombined) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmploymentAndPrivatePensions"))
    }

    "display combined employment and private pensions income summary information" in{
      val document = doc(viewCombined)

      document must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYear().end.toString("d MMMM YYYY")))}")

      document must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYear().next.start.toString("d MMMM YYYY")))}")

      document must haveTdWithText(employerNameHeading + employmentOneIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + employmentOneIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + employmentOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, employmentOneIncomeSourceDetail)

      document must haveTdWithText(employerNameHeading + employmentTwoIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + employmentTwoIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + employmentTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, employmentTwoIncomeSourceDetail)

      document must haveTdWithText(pensionNameHeading + pensionOneIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + pensionOneIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + pensionOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, pensionOneIncomeSourceDetail)

      document must haveTdWithText(pensionNameHeading + pensionTwoIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + pensionTwoIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + pensionTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, pensionTwoIncomeSourceDetail)

    }

    "display no content when no CY or CY+1 details are available" in{
      val viewNoDetails: Html = views.html.incomeTaxComparison.IncomeSummary(IncomeSourceComparisonViewModel(Nil,Nil), showEstimatedPay)
      val document = doc(viewNoDetails)
      document mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmployment"))
      document mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromPrivatePensions"))
      document mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmploymentAndPrivatePensions"))

      document mustNot haveElementWithId("incomeSummaryComparisonTable")
    }
  }

  "not display estimated income link" when {
    "feature flag is false" in {
      val noEstimatedPayView: Html = views.html.incomeTaxComparison.IncomeSummary(employmentIncomeSourceComparisonViewModel, hideEstimatedPay)
      val document = doc(noEstimatedPayView)

      document must haveTdWithText(employerNameHeading + employmentOneIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + employmentOneIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + employmentOneIncomeSourceDetail.amountCYPlusOne)
      document mustNot haveElementWithId(s"estimated-income-link-${employmentOneIncomeSourceDetail.empId}")

      document must haveTdWithText(employerNameHeading +employmentTwoIncomeSourceDetail.name)
      document must haveTdWithText(taxYearEnds + employmentTwoIncomeSourceDetail.amountCY)
      document must haveTdWithText(taxYearStarts + employmentTwoIncomeSourceDetail.amountCYPlusOne)
      document mustNot haveElementWithId(s"estimated-income-link-${employmentTwoIncomeSourceDetail.empId}")
    }
  }

  private def hasEstimatedIncomeLink(view: Html, income: IncomeSourceComparisonDetail) = {
    doc(view) must haveLinkElement(
      s"estimated-income-link-${income.empId}",
      s"/check-income-tax/update-income/next-year/income/${income.empId}/load",
      s"Update estimated income for ${income.name}"
    )
  }

  private lazy val employerNameHeading = "Employer name "
  private lazy val pensionNameHeading = "Pension name "
  private lazy val taxYearEnds = "Current tax year ends " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().end)) + " "
  private lazy val taxYearStarts = "Next tax year from " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYear().next.start)) + " "

  private lazy val employmentOneIncomeSourceDetail = IncomeSourceComparisonDetail(1, "Company1","£15,000","£15,500")
  private lazy val employmentTwoIncomeSourceDetail = IncomeSourceComparisonDetail(2, "Company2","£16,000","£16,500")

  private lazy val pensionOneIncomeSourceDetail = IncomeSourceComparisonDetail(3, "pension1","£15,000","£15,500")
  private lazy val pensionTwoIncomeSourceDetail = IncomeSourceComparisonDetail(4, "pension2","£16,000","£16,500")

  private val employmentIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Seq(employmentOneIncomeSourceDetail,employmentTwoIncomeSourceDetail),Nil)
  private val pensionIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Nil,Seq(pensionOneIncomeSourceDetail,pensionTwoIncomeSourceDetail))
  private val combinedIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(
    Seq(employmentOneIncomeSourceDetail,employmentTwoIncomeSourceDetail),
    Seq(pensionOneIncomeSourceDetail,pensionTwoIncomeSourceDetail)
  )

  private val showEstimatedPay = true
  private val hideEstimatedPay = !showEstimatedPay

  override def view: Html = views.html.incomeTaxComparison.IncomeSummary(employmentIncomeSourceComparisonViewModel, showEstimatedPay)
  def viewPensionsOnly: Html = views.html.incomeTaxComparison.IncomeSummary(pensionIncomeSourceComparisonViewModel, showEstimatedPay)
  def viewCombined: Html = views.html.incomeTaxComparison.IncomeSummary(combinedIncomeSourceComparisonViewModel, showEstimatedPay)
}
