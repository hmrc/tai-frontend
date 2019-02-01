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

package views.html.includes

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class ConfirmationSummarySpec extends TaiViewSpec{
  override def view: Html = views.html.includes.confirmation_summary()

  "Confirmation View" must {

    "display heading" in {
      doc must haveHeadingH2WithText(messages("tai.confirmation.sectionOne.heading"))
    }

    "display paragraphs" in {
      doc must haveParagraphWithText(messages("tai.confirmation.paraOne"))
      doc must haveParagraphWithText(messages("tai.confirmation.threeWeeks.paraTwo"))
      doc must haveParagraphWithText(messages("tai.confirmation.paraThree"))
    }

  }
}
