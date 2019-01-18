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

import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec



class noCurrentIncomeSpec extends TaiViewSpec {


  "noCurrentIncome view" must {

    behave like pageWithCombinedHeader(
      messages("tai.taxYear",
        Dates.formatDate(TaxYear().start).replace(" ", "\u00A0"),
        Dates.formatDate(TaxYear().end).replace(" ", "\u00A0")
      ),
      messages("tai.estimatedIncome.title"),
      Some(messages("tai.estimatedIncome.accessiblePreHeading"))
    )

    behave like pageWithBackLink


    "display no current income" in {
      doc(view) must haveParagraphWithText(messages("tai.no.increasesTax"))
    }
  }

  override def view = views.html.estimatedIncomeTax.noCurrentIncome()

}
