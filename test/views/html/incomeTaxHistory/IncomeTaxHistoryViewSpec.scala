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

package views.html.incomeTaxHistory

import org.jsoup.Jsoup
import play.api.i18n.Messages
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
          s" ${person.address.line1.get} ${person.address.line2.get} ${person.address.line3.get} ${person.address.postcode.get} " +
          messages("tai.incomeTax.history.details.nationalInsurance") +
          s" ${person.nino.formatted}"
      )
      doc must haveListItemWithText("End date " + messages("tai.incomeTax.history.endDate.notApplicable"))
    }

    "display a details card when some of the fields are empty" in {
      val doc = Jsoup.parse(viewWithPartialAddress.toString())
      doc must haveSummaryWithText(messages("tai.incomeTax.history.details.summary", person.name))
      doc must haveDetailsWithText(
        messages("tai.incomeTax.history.details.address") +
          s" ${person.address.line1.get} ${person.address.postcode.get} " +
          messages("tai.incomeTax.history.details.nationalInsurance") +
          s" ${person.nino.formatted}"
      )
      doc must haveListItemWithText("End date " + messages("tai.incomeTax.history.endDate.notApplicable"))
    }

    "display a details card with no address" when {
      "the user does not have a registered address" in {
        val doc = Jsoup.parse(viewWithNoAddress.toString())
        doc mustNot haveSpanWithText(messages("tai.incomeTax.history.details.address"))
      }
    }

    "display print button link with javascript print function" in {
      doc must haveLinkWithUrlWithClass("print-this__link", "javascript:window.print()")
    }

    "display ERN or pension" should {
      "display pension if available" in {
        doc must haveListItemWithText(messages("tai.pensionNumber") + "pension-number")
      }

      "display ern if pension but no payroll number" in {
        doc must haveListItemWithText(messages("tai.incomeTax.history.employerReference") + "ern-for-pension")
      }

      "display ern if not a pension" in {
        doc must haveListItemWithText(messages("tai.incomeTax.history.employerReference") + "ern")
      }
    }

    "display employment start date as 'Not Available' when not found" in {
      doc(incomeTaxHistoryView(appConfig, person, emptyStartDateIncomeTaxYears)) must haveListItemWithText(
        s"Start date ${Messages("tai.incomeTaxSummary.view.startDate.error")}")
    }

    "display employment start date as 'Not Available' when 1900 or earlier" in {
      doc(incomeTaxHistoryView(appConfig, person, oldStartDateIncomeTaxYears)) must haveListItemWithText(
        s"Start date ${Messages("tai.incomeTaxSummary.view.startDate.error")}")
    }

    val taxYears = (TaxYear().year until (TaxYear().year - 5) by -1).map(TaxYear(_)).toList

    for (taxYear <- taxYears) {
      s"display correct tax year link: $taxYear" in {
        doc must haveParagraphWithText(
          messages("tai.incomeTax.history.table.link", TaxPeriodLabelService.taxPeriodLabelYears(taxYear.year)))
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
  val taxYear1899: TaxYear = TaxYear(1899)
  val historyViewModel: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = true,
    "ern",
    Some("pension-number"),
    taxYear.start,
    None,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )

  val historyViewModel1: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = true,
    "ern-for-pension",
    None,
    taxYear.start.minusYears(1),
    Some(taxYear.end.minusYears(1)),
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )

  val historyViewModel2: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = false,
    "ern",
    None,
    taxYear.start.minusYears(2),
    Some(taxYear.end.minusYears(2)),
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    None
  )

  val historyViewModel3: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = false,
    "ern",
    None,
    taxYear.start.minusYears(3),
    Some(taxYear.end.minusYears(3)),
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    None
  )

  val historyViewModel4: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = false,
    "ern",
    None,
    taxYear.start.minusYears(4),
    Some(taxYear.end.minusYears(4)),
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )

  val historyViewModel5: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = false,
    "ern",
    None,
    taxYear.start.minusYears(5),
    Some(taxYear.end.minusYears(5)),
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )

  val incomeTaxYears: List[IncomeTaxYear] = List(
    IncomeTaxYear(taxYear, List(historyViewModel)),
    IncomeTaxYear(TaxYear(taxYear.year - 1), List(historyViewModel1)),
    IncomeTaxYear(TaxYear(taxYear.year - 2), List(historyViewModel2)),
    IncomeTaxYear(TaxYear(taxYear.year - 3), List(historyViewModel4)),
    IncomeTaxYear(TaxYear(taxYear.year - 4), List(historyViewModel5))
  )

  val emptyStartDateHistoryViewModel: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = true,
    "ern",
    Some("pension-number"),
    null,
    None,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )
  val oldStartDateHistoryViewModel: IncomeTaxHistoryViewModel = IncomeTaxHistoryViewModel(
    "employerName",
    isPension = true,
    "ern",
    Some("pension-number"),
    taxYear1899.start,
    None,
    Some("taxableIncome"),
    Some("incomeTaxPaid"),
    Some(s"taxCode-${taxYear.start}")
  )
  val emptyStartDateIncomeTaxYears: List[IncomeTaxYear] = List(
    IncomeTaxYear(taxYear, List(emptyStartDateHistoryViewModel))
  )
  val oldStartDateIncomeTaxYears: List[IncomeTaxYear] = List(
    IncomeTaxYear(taxYear, List(oldStartDateHistoryViewModel))
  )

  val person = fakePerson(nino)
  val personWithNoAddress = fakePersonWithNoAddress(nino)
  val personWithPartialAddress = fakePersonWithPartialAddress(nino)

  override def view: Html =
    incomeTaxHistoryView(appConfig, person, incomeTaxYears)

  val viewWithNoAddress: Html = incomeTaxHistoryView(appConfig, personWithNoAddress, incomeTaxYears)
  val viewWithPartialAddress: Html = incomeTaxHistoryView(appConfig, personWithPartialAddress, incomeTaxYears)

}
