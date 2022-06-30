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
import uk.gov.hmrc.tai.service.TaxPeriodLabelService
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

    "display print button" should {
      behave like pageWithPrintThisPageButton("Print this page")

    }

    val taxYears = (TaxYear().year until (TaxYear().year - 5) by -1).map(TaxYear(_)).toList

    for (taxYear <- taxYears) {
      s"display correct tax year link: $taxYear" in {
        doc must haveParagraphWithText(
          messages("tai.incomeTax.history.table.link", TaxPeriodLabelService.taxPeriodLabel(taxYear.year)))
      }

      s"display unavailable if the tax code isn't present or the tax code $taxYear" in {
        val maybeTaxCode = incomeTaxYears.collectFirst {
          case IncomeTaxYear(`taxYear`, List(viewModel)) =>
            viewModel.maybeTaxCode
        }.flatten

        maybeTaxCode match {
          case Some(taxCode) =>
            doc must haveListItemWithText(s"Tax code $taxCode")
          case None =>
            doc must haveListItemWithText("Tax code " + messages("tai.incomeTax.history.unavailable"))
        }
      }
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
    Some(s"taxCode-${taxYear.start}")
  )

  val historyViewModel1: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    "ern",
    TaxYear(2021).start,
    TaxYear(2021).end,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )

  val historyViewModel2: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    "ern",
    TaxYear(2020).start,
    TaxYear(2020).end,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    None
  )

  val historyViewModel3: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    "ern",
    TaxYear(2019).start,
    TaxYear(2019).end,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    None
  )

  val historyViewModel4: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    "ern",
    TaxYear(2019).start,
    TaxYear(2019).end,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )

  val historyViewModel5: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    "ern",
    TaxYear(2018).start,
    TaxYear(2018).end,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )

  val incomeTaxYears: List[IncomeTaxYear] = List(
    IncomeTaxYear(taxYear, List(historyViewModel)),
    IncomeTaxYear(TaxYear(2021), List(historyViewModel1)),
    IncomeTaxYear(TaxYear(2020), List(historyViewModel2)),
    IncomeTaxYear(TaxYear(2019), List(historyViewModel4)),
    IncomeTaxYear(TaxYear(2018), List(historyViewModel5))
  )

  val person = fakePerson(nino)

  override def view: Html =
    incomeTaxHistoryView(appConfig, person, incomeTaxYears)
}
