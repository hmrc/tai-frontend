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

package views.html.estimatedIncomeTax

import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec


class whyThisIsAnEstimateDescSpec extends TaiViewSpec {

  "have static messages" in {

    doc(view) must haveH2HeadingWithText(messages("tai.estimatedIncome.whyEstimate.link"))

    doc(view) must haveParagraphWithText(Html(
      messages("tai.estimatedIncome.whyEstimate.desc", Dates.formatDate(TaxYear().next.end))).body)

  }

  override def view: Html = views.html.estimatedIncomeTax.whyThisIsAnEstimateDesc()
}
