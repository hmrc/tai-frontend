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


    "display employments income summary table" in{

      doc must haveThWithText(messages("tai.CurrentTaxYearEnds",TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM")))
      doc must haveThWithText(messages("tai.NextTaxYearFrom",TaxYearResolver.startOfNextTaxYear.toString("d MMMM YYYY")))

      doc must haveTdWithText(employmentOneIncomeSourceDetail.name)
      doc must haveTdWithText(employmentOneIncomeSourceDetail.amountCY)
      doc must haveTdWithText(employmentOneIncomeSourceDetail.amountCYPlusOne)

      doc must haveTdWithText(employmentTwoIncomeSourceDetail.name)
      doc must haveTdWithText(employmentTwoIncomeSourceDetail.amountCY)
      doc must haveTdWithText(employmentTwoIncomeSourceDetail.amountCYPlusOne)

    }
  }

  private lazy val employmentOneIncomeSourceDetail = IncomeSourceComparisonDetail("Company1","£15,000","£15,500")
  private lazy val employmentTwoIncomeSourceDetail = IncomeSourceComparisonDetail("Company2","£16,000","£16,500")

  private val incomeSourceComparisonViewModel = IncomeSourceComparisonViewModel(Seq(employmentOneIncomeSourceDetail,employmentTwoIncomeSourceDetail))

  override def view: Html = views.html.incomeTaxComparison.IncomeSummary(incomeSourceComparisonViewModel)
}
