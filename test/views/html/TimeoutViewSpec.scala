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

package views.html

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class TimeoutViewSpec extends TaiViewSpec {

  "The timeout page" must {

    "have the correct title" in {
      doc must haveHeadingWithText(Messages("tai.timeout.title"))
    }

    "contain the correct content" in {
      doc must haveParagraphWithText(Messages("tai.timeout.message"))
      doc must haveParagraphWithText(Messages("tai.timeout.message.pleaseLogin"))
      doc must haveElementAtPathWithAttribute("a", "href", controllers.routes.TaxAccountSummaryController.onPageLoad().url)
    }
  }

  override def view = views.html.timeout()
}
