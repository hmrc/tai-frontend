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

package views.html

import play.twirl.api.HtmlFormat
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class unauthorizedAccessPageSpec extends TaiViewSpec {

  private val unauthorizeAccess = inject[UnauthorizedAccessView]
  override def view: HtmlFormat.Appendable = unauthorizeAccess()

  "Unauthorized Access Page" should {
    behave like pageWithHeader(messages("tai.gatekeeper.refuse.title"))
    behave like pageWithTitle(messages("tai.gatekeeper.refuse.title"))

    "display the appropriate information in the paragraph" in {
      doc must haveParagraphWithText(messages("tai.gatekeeper.refuse.message"))
    }

    "have find out more information message displayed" in {
      val findOutMore = doc.getElementById("find-out-more")
      findOutMore.text must include(messages("tai.gov.uk.text"))
      findOutMore.text must include(messages("tai.gatekeeper.findout.more"))
    }

    "have income tax page link" in {
      val incomeTaxPageLink = doc.getElementById("income-tax-link")
      incomeTaxPageLink must haveLinkURL(messages("tai.link.income_tax.url"))
    }
  }

}
