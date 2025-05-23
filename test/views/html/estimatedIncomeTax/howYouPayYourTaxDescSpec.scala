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

package views.html.estimatedIncomeTax

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import views.html.includes.link

class howYouPayYourTaxDescSpec extends TaiViewSpec {

  "have static messages" in {

    doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.howYouPay.heading"))

    doc(view) must haveParagraphWithText(
      Html(messages("tai.estimatedIncome.howYouPay.desc", messages("tai.estimatedIncome.taxCodes.link"))).body
    )

    doc(view).select("#howYouPayDesc").html().replaceAll("\\s+", "") mustBe Html(
      messages(
        "tai.estimatedIncome.howYouPay.desc",
        link(
          id = Some("taxCodesLink"),
          linkClasses = Seq("display-for-print"),
          url = routes.YourTaxCodeController.taxCodes().url.toString,
          copy = Messages("tai.estimatedIncome.taxCodes.link")
        )
      )
    ).body.replaceAll("\\s+", "")
  }

  override def view: Html = views.html.estimatedIncomeTax.howYouPayYourTaxDesc()
}
