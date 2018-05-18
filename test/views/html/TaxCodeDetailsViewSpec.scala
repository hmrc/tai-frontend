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

package views.html

import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{DescriptionListViewModel, TaxCodeViewModel}

import scala.collection.immutable.ListMap

class TaxCodeDetailsViewSpec extends TaiViewSpec {

  "Tax code view page" must {
    behave like pageWithTitle("main heading")
    behave like pageWithCombinedHeader(messages("tai.taxCode.preHeader"),"main heading")
    behave like pageWithBackLink

    "display the provided lede message" in {
      doc must haveParagraphWithText("lede message")
    }

    "display navigational links to other pages in the service" in {
      doc must haveLinkElement("taxFreeAmountLink", controllers.routes.TaxFreeAmountController.taxFreeAmount.url, messages("tai.incomeTax.taxFree.link"))
      doc must haveLinkElement("taxableIncomeLink", controllers.routes.TaxAccountSummaryController.onPageLoad.url, messages("tai.incomeTaxSummary.link"))
    }

    "display an list of tax-code-part descriptions, for each tax code provided in the view model" in {
      doc must haveUnorderedListWithId("taxCodeList1")
      doc must haveH2HeadingWithIdAndText("taxCodeList1Heading", "Your tax code for employer1: BR")
      doc must haveElementAtPathWithText("#taxCodeTerm_1_1 span", messages("tai.taxCode.part.announce", "K"))
      doc must haveElementAtPathWithText("#taxCodeTerm_1_1 span", "K")
      doc must haveElementAtPathWithText("#taxCodeDescription_1_1", s"${messages("tai.taxCode.definition.announce")} ${messages("tai.taxCode.BR")}")

      doc must haveUnorderedListWithId("taxCodeList2")
      doc must haveH2HeadingWithIdAndText("taxCodeList2Heading", "Your tax code for employer2: D0")
      doc must haveElementAtPathWithText("#taxCodeTerm_2_1 span", messages("tai.taxCode.part.announce", "D0"))
      doc must haveElementAtPathWithText("#taxCodeTerm_2_1 span", "D0")
      doc must haveElementAtPathWithText("#taxCodeDescription_2_1", s"${messages("tai.taxCode.definition.announce")} ${messages("tai.taxCode.DX", 40)}")

      doc must haveElementAtPathWithText("#taxCodeTerm_2_2 span", messages("tai.taxCode.part.announce", "K"))
      doc must haveElementAtPathWithText("#taxCodeTerm_2_2 span", "K")
      doc must haveElementAtPathWithText("#taxCodeDescription_2_2", s"${messages("tai.taxCode.definition.announce")} ${messages("tai.taxCode.BR")}")
    }
  }

  val taxCodeDescription1 = DescriptionListViewModel("Your tax code for employer1: BR", ListMap("K" -> messages("tai.taxCode.BR")))
  val taxCodeDescription2 = DescriptionListViewModel("Your tax code for employer2: D0", ListMap( "D0" -> messages("tai.taxCode.DX", 40), "K" -> messages("tai.taxCode.BR") ))

  val viewModel: TaxCodeViewModel = TaxCodeViewModel("main heading", "main heading", "lede message", Seq(taxCodeDescription1, taxCodeDescription2))

  override def view = views.html.taxCodeDetails(viewModel)
}
