/*
 * Copyright 2022 HM Revenue & Customs
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

package views.html.incomes

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.HtmlFormatter.htmlNonBroken
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class EditSuccessViewSpec extends TaiViewSpec {

  private val employerId = 1
  private val employerName = "fakeFieldValue"

  "Edit Success Employment view" should {
    "contain the success heading" in {
      doc(view) must havePanelWithHeaderText(
        messages("tai.incomes.updated.check.heading")
      )
    }
    "contain the success body" in {
      doc(view) must havePanelWithBodyText(
        s"${htmlNonBroken(messages("tai.incomes.updated.check.heading.pt2"))} " +
          s"${htmlNonBroken(messages("tai.incomes.updated.check.heading.pt3"))}"
      )
    }

    "contain the success paragraph" in {
      doc(view).getElementsByTag("p").text must include(
        messages("tai.incomes.updated.check.text")
      )
    }

    "contain the may change paragraph" in {
      doc(view).getElementsByTag("p").text must include(messages("tai.incomes.seeChanges.text", employerName))
    }
  }

  private def editSuccess = inject[EditSuccessView]
  override def view: Html = editSuccess(employerName, employerId)
}
