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

package views.html.incomes

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class EditPensionSuccessSpec extends TaiViewSpec {

  private val employerId = 1

  "Edit Success Pension view" should {
    "contain the success heading" in {
      doc(view) must haveHeadingWithText(
        s"${messages("tai.incomes.updated.check.heading")} " +
          s"${messages("tai.incomes.updated.check.heading.pt2")} " +
          s"${messages("tai.incomes.updated.check.heading.pt3")}"
      )
    }

    "contain the success paragraph and link" in {
      doc(view).getElementsByTag("p").text must include(
        messages("tai.incomes.updated.pension.check.text")
      )
    }

    "contain the may change paragraph" in {
      doc(view).getElementsByTag("p").text must include(
        messages("tai.incomes.updated.pension.seeChanges.text", employerName)
      )
    }
  }

  private def editPensionSuccess = inject[EditPensionSuccessView]
  override def view: Html        = editPensionSuccess(employerName, employerId)
}
