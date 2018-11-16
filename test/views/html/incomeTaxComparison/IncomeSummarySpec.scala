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

package views.html.incomeTaxComparison

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{IncomeSourceComparisonDetail, IncomeSourceComparisonViewModel}
import uk.gov.hmrc.time.TaxYearResolver

class IncomeSummarySpec extends TaiViewSpec {

  "Cy plus one income summary view" must {

    "have income from employment header" in {
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmployment"))
    }


    "display employment income summary information" in{

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM YYYY")))}")

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYearResolver.startOfNextTaxYear.toString("d MMMM YYYY")))}")


      doc(view) must haveThWithText(employmentOneIncomeSourceDetail.name)
      doc(view) must haveTdWithText(employmentOneIncomeSourceDetail.amountCY)
      doc(view) must haveTdWithText(employmentOneIncomeSourceDetail.amountCYPlusOne)
      doc(view) must haveTdWithText(s"Update estimated income for ${employmentOneIncomeSourceDetail.name}")


      doc(view) must haveThWithText(employmentTwoIncomeSourceDetail.name)
      doc(view) must haveTdWithText(employmentTwoIncomeSourceDetail.amountCY)
      doc(view) must haveTdWithText(employmentTwoIncomeSourceDetail.amountCYPlusOne)
      doc(view) must haveTdWithText(s"Update estimated income for ${employmentTwoIncomeSourceDetail.name}")
    }

    "have income from pension header" in {
      doc(viewPensionsOnly) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromPrivatePensions"))
    }


    "display pensions income summary information" in{

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM YYYY")))}")

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYearResolver.startOfNextTaxYear.toString("d MMMM YYYY")))}")

      doc(viewPensionsOnly) must haveThWithText(pensionOneIncomeSourceDetail.name)
      doc(viewPensionsOnly) must haveTdWithText(pensionOneIncomeSourceDetail.amountCY)
      doc(viewPensionsOnly) must haveTdWithText(pensionOneIncomeSourceDetail.amountCYPlusOne)
      doc(viewPensionsOnly) must haveTdWithText(s"Update estimated income for ${pensionOneIncomeSourceDetail.name}")


      doc(viewPensionsOnly) must haveThWithText(pensionTwoIncomeSourceDetail.name)
      doc(viewPensionsOnly) must haveTdWithText(pensionTwoIncomeSourceDetail.amountCY)
      doc(viewPensionsOnly) must haveTdWithText(pensionTwoIncomeSourceDetail.amountCYPlusOne)
      doc(viewPensionsOnly) must haveTdWithText(s"Update estimated income for ${pensionTwoIncomeSourceDetail.name}")

    }

    "have income and employment pension header" in {
      doc(viewCombined) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmploymentAndPrivatePensions"))
    }

    "display combined employment and private pensions income summary information" in{

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.CurrentTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column1",TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM YYYY")))}")

      doc(view) must haveThWithText(s"${nonBreakable(messages("tai.NextTaxYear"))} " +
        s"${nonBreakable(messages("tai.incomeTaxComparison.incomeTax.column2",TaxYearResolver.startOfNextTaxYear.toString("d MMMM YYYY")))}")

      doc(viewCombined) must haveThWithText(employmentOneIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(employmentOneIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(employmentOneIncomeSourceDetail.amountCYPlusOne)
      doc(viewCombined) must haveTdWithText(s"Update estimated income for ${employmentOneIncomeSourceDetail.name}")

      doc(viewCombined) must haveThWithText(employmentTwoIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(employmentTwoIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(employmentTwoIncomeSourceDetail.amountCYPlusOne)
      doc(viewCombined) must haveTdWithText(s"Update estimated income for ${employmentTwoIncomeSourceDetail.name}")

      doc(viewCombined) must haveThWithText(pensionOneIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(pensionOneIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(pensionOneIncomeSourceDetail.amountCYPlusOne)
      doc(viewCombined) must haveTdWithText(s"Update estimated income for ${pensionOneIncomeSourceDetail.name}")

      doc(viewCombined) must haveThWithText(pensionTwoIncomeSourceDetail.name)
      doc(viewCombined) must haveTdWithText(pensionTwoIncomeSourceDetail.amountCY)
      doc(viewCombined) must haveTdWithText(pensionTwoIncomeSourceDetail.amountCYPlusOne)
      doc(viewCombined) must haveTdWithText(s"Update estimated income for ${pensionTwoIncomeSourceDetail.name}")
    }

    "display no content when no CY or CY+1 details are available" in{

      doc(viewNoDetails) mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmployment"))
      doc(viewNoDetails) mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromPrivatePensions"))
      doc(viewNoDetails) mustNot haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.incomeFromEmploymentAndPrivatePensions"))

      doc(viewNoDetails) mustNot haveElementWithId("incomeSummaryComparisonTable")
    }

  }

  private lazy val employmentOneIncomeSourceDetail = IncomeSourceComparisonDetail(1, "Company1","£15,000","£15,500")
  private lazy val employmentTwoIncomeSourceDetail = IncomeSourceComparisonDetail(2, "Company2","£16,000","£16,500")

  private lazy val pensionOneIncomeSourceDetail = IncomeSourceComparisonDetail(1, "pension1","£15,000","£15,500")
  private lazy val pensionTwoIncomeSourceDetail = IncomeSourceComparisonDetail(2, "pension2","£16,000","£16,500")

  private val employmentIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Seq(employmentOneIncomeSourceDetail,employmentTwoIncomeSourceDetail),Nil)
  private val pensionIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Nil,Seq(pensionOneIncomeSourceDetail,pensionTwoIncomeSourceDetail))
  private val combinedIncomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Seq(employmentOneIncomeSourceDetail,employmentTwoIncomeSourceDetail)
                                                                                          ,Seq(pensionOneIncomeSourceDetail,pensionTwoIncomeSourceDetail))

  override def view: Html = views.html.incomeTaxComparison.IncomeSummary(employmentIncomeSourceComparisonViewModel)
  def viewPensionsOnly: Html = views.html.incomeTaxComparison.IncomeSummary(pensionIncomeSourceComparisonViewModel)
  def viewCombined: Html = views.html.incomeTaxComparison.IncomeSummary(combinedIncomeSourceComparisonViewModel)
  def viewNoDetails: Html = views.html.incomeTaxComparison.IncomeSummary(IncomeSourceComparisonViewModel(Nil,Nil))
}
