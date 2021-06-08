/*
 * Copyright 2021 HM Revenue & Customs
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

package views.html.estimatedIncomeTax

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.urls.Link

class navigationLinksSpec extends TaiViewSpec {

  "display navigational links to other pages in the service" in {
    doc must haveElementAtPathWithText("nav>h2", messages("tai.taxCode.sideBar.heading"))
    doc must haveLinkElement(
      "taxCodesSideLink",
      routes.YourTaxCodeController.taxCodes.url,
      messages("check.your.tax.codes"))
    doc must haveLinkElement(
      "taxFreeAmountSideLink",
      routes.TaxFreeAmountController.taxFreeAmount.url,
      messages("check.your.tax.free.amount"))
    doc must haveLinkElement(
      "taxSummarySideLink",
      controllers.routes.TaxAccountSummaryController.onPageLoad.url,
      messages("return.to.your.income.tax.summary"))
  }
}
