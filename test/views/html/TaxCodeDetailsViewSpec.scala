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

package views.html

import org.jsoup.Jsoup
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{DescriptionListViewModel, TaxCodeViewModel}

import scala.collection.immutable.ListMap

class TaxCodeDetailsViewSpec extends TaiViewSpec {

  "Tax code view page" must {
    behave like pageWithTitle("main heading")
    behave like pageWithCombinedHeaderNewFormatNew(messages("tai.taxCode.preHeader"), "main heading")
    behave like pageWithBackLink()

    "display the provided lede message" in {
      doc must haveParagraphWithText("lede message")
    }

    "display navigational links to other pages in the service" in {
      doc must haveLinkElement(
        "taxFreeAmountLink",
        controllers.routes.TaxFreeAmountController.taxFreeAmount().url,
        messages("check.your.tax.free.amount")
      )
      doc must haveLinkElement(
        "incomeTaxEstimateLink",
        controllers.routes.EstimatedIncomeTaxController.estimatedIncomeTax().url,
        messages("check.your.income.tax.estimate")
      )
      doc must haveLinkElement(
        "taxableIncomeLink",
        controllers.routes.TaxAccountSummaryController.onPageLoad().url,
        messages("return.to.your.income.tax.summary")
      )
    }

    "contain a link to the income details for this employer" in {
      val view = template(viewModel)
      val doc  = Jsoup.parse(view.toString())

      doc must haveLinkElement(
        "employmentDetails",
        controllers.routes.IncomeSourceSummaryController.onPageLoad(employerId).url,
        messages("tai.taxCode.check_employment")
      )
    }

    "contain details element with tax code information" in {
      val view = template(viewModel)
      val doc  = Jsoup.parse(view.toString())

      doc must haveElementAtPathWithText("#taxCodeTerm_1_1", messages("tai.taxCode.part.announce", "K") + " K")
      doc must haveElementWithId("taxCodeDescription_1_1")
    }
  }

  val employerId          = 9876543
  val taxCodeDescription1 =
    DescriptionListViewModel("Your tax code for employer1: BR", ListMap("K" -> messages("tai.taxCode.BR")))
  val taxCodeDescription2 = DescriptionListViewModel(
    "Your tax code for employer2: D0",
    ListMap("D0" -> messages("tai.taxCode.DX", 40), "K" -> messages("tai.taxCode.BR"))
  )

  val viewModel: TaxCodeViewModel = TaxCodeViewModel(
    "main heading",
    "lede message",
    Seq(taxCodeDescription1, taxCodeDescription2),
    messages("tai.taxCode.preHeader"),
    messages("tai.taxCode.check_employment"),
    Some(employerId)
  )

  private val template = inject[TaxCodeDetailsView]

  override def view: Html = template(viewModel)
}
