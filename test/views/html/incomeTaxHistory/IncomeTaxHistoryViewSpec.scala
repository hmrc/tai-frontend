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

package views.html.incomeTaxHistory

import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.incomeTaxHistory.{IncomeTaxHistoryViewModel, IncomeTaxYear}

class IncomeTaxHistoryViewSpec extends TaiViewSpec {

  "Income tax history view" must {
    behave like pageWithTitle(messages("tai.incomeTax.history.pageTitle"))
    behave like pageWithHeader(messages("tai.incomeTax.history.title"))

    "display a back button" in {
      doc must haveBackLinkNew
    }

    "display a details card" in {
      doc must haveSummaryWithText(messages("tai.incomeTax.history.details.summary", person.name))
      doc must haveDetailsWithText(
        messages("tai.incomeTax.history.details.address") +
          s" ${person.address.line1} ${person.address.line2} ${person.address.line3} ${person.address.postcode} " +
          messages("tai.incomeTax.history.details.nationalInsurance") +
          s" ${person.nino}"
      )
    }

    "display tax years" in {
      ???
    }
  }

  val incomeTaxHistoryView = inject[IncomeTaxHistoryView]
  val taxYear: TaxYear = TaxYear()
  val historyViewModel: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    "ern",
    taxYear.start,
    taxYear.end,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some("taxCode")
  )
  val incomeTaxYears: List[IncomeTaxYear] = List(
    IncomeTaxYear(taxYear, List(historyViewModel))
  )
  val person = fakePerson(nino)
  override def view: Html =
    incomeTaxHistoryView(appConfig, person, incomeTaxYears)
}
