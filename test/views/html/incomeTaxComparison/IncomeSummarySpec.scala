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
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{IncomeSourceComparisonDetail, IncomeSourceComparisonViewModel}
import uk.gov.hmrc.time.TaxYearResolver

class IncomeSummarySpec extends TaiViewSpec {

  "Cy plus one income summary view" must {

    "have income from employment header" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmployment"))
    }

    "display income from employment description" in{
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.incomeTax.fromEmployer.description"))
    }


    "display employment income summary information" in{

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM YYYY")))}")

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYearResolver.startOfNextTaxYear.toString("d MMMM YYYY")))}")


      doc(view) must haveTdWithText(employerNameHeading + employmentOneIncomeSourceDetail.name)
      doc(view) must haveTdWithText(taxYearEnds + employmentOneIncomeSourceDetail.amountCY)
      doc(view) must haveTdWithText(taxYearStarts + employmentOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(view, employmentTwoIncomeSourceDetail)

      doc(view) must haveTdWithText(employerNameHeading + employmentTwoIncomeSourceDetail.name)
      doc(view) must haveTdWithText(taxYearEnds + employmentTwoIncomeSourceDetail.amountCY)
      doc(view) must haveTdWithText(taxYearStarts + employmentTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(view, employmentTwoIncomeSourceDetail)
    }

    "have income from pension header" in {
      doc(viewPensionsOnly) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromPrivatePensions"))
    }


    "display pensions income summary information" in{

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM YYYY")))}")

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYearResolver.startOfNextTaxYear.toString("d MMMM YYYY")))}")

      doc(viewPensionsOnly) must haveTdWithText(pensionNameHeading + pensionOneIncomeSourceDetail.name)
      doc(viewPensionsOnly) must haveTdWithText(taxYearEnds + pensionOneIncomeSourceDetail.amountCY)
      doc(viewPensionsOnly) must haveTdWithText(taxYearStarts + pensionOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewPensionsOnly, pensionOneIncomeSourceDetail)

      doc(viewPensionsOnly) must haveTdWithText(pensionNameHeading + pensionTwoIncomeSourceDetail.name)
      doc(viewPensionsOnly) must haveTdWithText(taxYearEnds + pensionTwoIncomeSourceDetail.amountCY)
      doc(viewPensionsOnly) must haveTdWithText(taxYearStarts + pensionTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewPensionsOnly, pensionTwoIncomeSourceDetail)

    }

    "have income and employment pension header" in {
      doc(viewCombined) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmploymentAndPrivatePensions"))
    }

    "display combined employment and private pensions income summary information" in{

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM YYYY")))}")

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYearResolver.startOfNextTaxYear.toString("d MMMM YYYY")))}")

      doc(viewCombined) must haveTdWithText(employerNameHeading + employmentOneIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(taxYearEnds + employmentOneIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(taxYearStarts + employmentOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, employmentOneIncomeSourceDetail)

      doc(viewCombined) must haveTdWithText(employerNameHeading + employmentTwoIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(taxYearEnds + employmentTwoIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(taxYearStarts + employmentTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, employmentTwoIncomeSourceDetail)

      doc(viewCombined) must haveTdWithText(pensionNameHeading + pensionOneIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(taxYearEnds + pensionOneIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(taxYearStarts + pensionOneIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, pensionOneIncomeSourceDetail)

      doc(viewCombined) must haveTdWithText(pensionNameHeading + pensionTwoIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(taxYearEnds + pensionTwoIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(taxYearStarts + pensionTwoIncomeSourceDetail.amountCYPlusOne)
      hasEstimatedIncomeLink(viewCombined, pensionTwoIncomeSourceDetail)

    }

    "display no content when no CY or CY+1 details are available" in{

      doc(viewNoDetails) mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmployment"))
      doc(viewNoDetails) mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromPrivatePensions"))
      doc(viewNoDetails) mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmploymentAndPrivatePensions"))

      doc(viewNoDetails) mustNot haveElementWithId("incomeSummaryComparisonTable")
    }
  }

  private def hasEstimatedIncomeLink(view: Html, income: IncomeSourceComparisonDetail) = {
    doc(view) must haveLinkElement(
      s"estimated-income-link-${income.empId}",
      s"/check-income-tax/update-income/next-year/income/${income.empId}/start",
      s"Update estimated income for ${income.name}"
    )
  }

  private lazy val employerNameHeading = "Employer name "
  private lazy val pensionNameHeading = "Pension name "
  private lazy val taxYearEnds = "Current tax year ends " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)) + " "
  private lazy val taxYearStarts = "Next tax year from " + HtmlFormatter.htmlNonBroken(Dates.formatDate(TaxYearResolver.startOfNextTaxYear)) + " "

  private lazy val employmentOneIncomeSourceDetail = IncomeSourceComparisonDetail(1, "Company1","£15,000","£15,500")
  private lazy val employmentTwoIncomeSourceDetail = IncomeSourceComparisonDetail(2, "Company2","£16,000","£16,500")

  private lazy val pensionOneIncomeSourceDetail = IncomeSourceComparisonDetail(3, "pension1","£15,000","£15,500")
  private lazy val pensionTwoIncomeSourceDetail = IncomeSourceComparisonDetail(4, "pension2","£16,000","£16,500")

  private val employmentIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Seq(employmentOneIncomeSourceDetail,employmentTwoIncomeSourceDetail),Nil)
  private val pensionIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Nil,Seq(pensionOneIncomeSourceDetail,pensionTwoIncomeSourceDetail))
  private val combinedIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Seq(employmentOneIncomeSourceDetail,employmentTwoIncomeSourceDetail)
                                                                                          ,Seq(pensionOneIncomeSourceDetail,pensionTwoIncomeSourceDetail))

  override def view: Html = views.html.incomeTaxComparison.IncomeSummary(employmentIncomeSourceComparisonViewModel)
  def viewPensionsOnly: Html = views.html.incomeTaxComparison.IncomeSummary(pensionIncomeSourceComparisonViewModel)
  def viewCombined: Html = views.html.incomeTaxComparison.IncomeSummary(combinedIncomeSourceComparisonViewModel)
  def viewNoDetails: Html = views.html.incomeTaxComparison.IncomeSummary(IncomeSourceComparisonViewModel(Nil,Nil))
}
