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

package views.html.pensions

import controllers.routes
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class AddPensionErrorPageSpec extends TaiViewSpec {
  "Display error page when pension provider cannot be added" must {
    behave like pageWithTitle(messages("tai.addPensionProvider.errorPage.title"))
    behave like pageWithCombinedHeader(
      messages("add.missing.pension"),
      messages("tai.addPensionProvider.errorPage.title"))

    "have link" in {
      doc must haveLinkWithUrlWithID("returnToYourIncomeDetails", routes.TaxAccountSummaryController.onPageLoad.url)
    }

    "have paragraph" in {
      doc must haveParagraphWithText(messages("tai.addPensionProvider.errorPage.para1", "fake pension provider"))
      doc must haveParagraphWithText(messages("tai.addPensionProvider.errorPage.para2"))
    }

    behave like pageWithBackLink
  }
  private val addPensionErrorPage = inject[addPensionErrorPage]
  override def view: Html = addPensionErrorPage("fake pension provider")
}
