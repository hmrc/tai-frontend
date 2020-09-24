/*
 * Copyright 2020 HM Revenue & Customs
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

package views.html.incomeTaxComparison

import play.api.i18n.{Lang, Messages}
import play.twirl.api.Html
import uk.gov.hmrc.play.views.formatting.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel, IncomeTaxComparisonViewModel}
import uk.gov.hmrc.tai.viewModels.{IncomeSourceComparisonViewModel, TaxCodeComparisonViewModel, TaxFreeAmountComparisonViewModel}

class MainSpec extends TaiViewSpec with ViewModelHelper {
  "Cy plus one view" must {

    val welshMessage = messagesApi.preferred(Seq(Lang("cy")))

    "show the correct page title" in {

      doc(viewWithMore(welshMessage)).title must include(welshMessage("tai.incomeTaxComparison.heading.more"))
    }

    "show the correct heading when the user is estimated to pay more tax in CY plus 1" in {

      doc(viewWithMore(welshMessage)) must haveHeadingWithText(welshMessage("tai.incomeTaxComparison.heading.more"))
    }

    "show the correct heading when the user is estimated to pay less tax in CY plus 1" in {

      doc(viewWithLess(welshMessage)) must haveHeadingWithText(welshMessage("tai.incomeTaxComparison.heading.less"))

    }

    "show the correct heading when the user is estimated to pay the same tax in CY plus 1" in {

      doc(viewWithSame(welshMessage)) must haveHeadingWithText(welshMessage("tai.incomeTaxComparison.heading.same"))
    }

    "show the correct h2 when user pays more tax next year" in {

      doc(viewWithMore(welshMessage)) must haveH2HeadingWithText(
        welshMessage("tai.incomeTaxComparison.incomeTax.subHeading.more", "£1"))
    }

    "show the correct h2 when user pays less tax next year" in {

      doc(viewWithSame(welshMessage)) must haveH2HeadingWithText(
        welshMessage("tai.incomeTaxComparison.incomeTax.subHeading.same"))
    }

    "show the correct h2 when user pays same tax next year" in {

      doc(viewWithLess(welshMessage)) must haveH2HeadingWithText(
        welshMessage("tai.incomeTaxComparison.incomeTax.subHeading.less", "£1"))
    }

    "show the correct table heading in welsh when user pays more tax next year" in {

      doc(viewWithMore(welshMessage)).text() must include(
        welshMessage("tai.incomeTaxComparison.dateWithoutWelshAmendment", Dates.formatDate(TaxYear().next.start)))
    }

    "show the correct table heading in welsh when user pays less tax next year" in {

      doc(viewWithLess(welshMessage)).text() must include(
        welshMessage("tai.incomeTaxComparison.welshAmendmentToDate", Dates.formatDate(TaxYear().next.start)))
    }

    "show the correct table heading in welsh when user pays same tax next year" in {

      doc(viewWithSame(welshMessage)).text() must include(
        welshMessage("tai.incomeTaxComparison.welshAmendmentToDate", Dates.formatDate(TaxYear().next.start)))
    }

    behave like pageWithCombinedHeader(
      preHeaderText = "USERNAME",
      mainHeaderText = messages("tai.incomeTaxComparison.heading.same"),
      preHeaderAnnouncementText = Some(messages("tai.incomeTaxComparison.preHeading.screenReader"))
    )

    "show the income tax section with heading" in {
      doc(viewWithMore) must haveDivWithId("incomeTax")
      doc(viewWithMore) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.more", "£1"))
    }

    "display a link to return to choose tax year page" in {
      doc must haveLinkWithUrlWithID(
        "returnToChooseTaxYearLink",
        controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      doc must haveLinkWithText(messages("your.paye.income.tax.overview"))
    }

    "show the tax codes section" in {
      doc(view) must haveDivWithId("taxCodes")
    }

    "show the income summary section" in {
      doc(view) must haveDivWithId("incomeSummary")
    }

    "show the tax free amount section with heading" in {
      doc(view) must haveDivWithId("taxFreeAmount")
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.taxFreeAmount.subHeading"))
    }

    "have the what happens next with heading" in {
      doc(view) must haveDivWithId("whatHappensNext")
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.whatHappensNext.subHeading"))
    }

    "have the what happens next section with paragraph - do nothing" in {
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.whatHappensNext.doNotDoAnything.text"))
    }

    "have the what happens next section with paragraph - estimation apply date" in {
      doc(view) must haveParagraphWithText(
        messages("tai.incomeTaxComparison.whatHappensNext.estimationApplyDate.text", startOfNextTaxYear))
    }

    "have the what happens next section with paragraph - calculation may change" in {
      doc(view) must haveParagraphWithText(
        messages("tai.incomeTaxComparison.whatHappensNext.calculationMayChange.text"))
    }

    "have the if information is wrong or incomplete title" in {
      doc(view) must haveHeadingH3WithText(
        messages("tai.incomeTaxComparison.whatHappensNext.ifInformationWrongOrIncomplete.heading"))
    }

    "have the tell us about a change paragraph" in {
      doc(view) must haveParagraphWithText(
        messages("tai.incomeTaxComparison.whatHappensNext.tellAboutChange.description", startOfNextTaxYear))
    }

    "have the tell us about a change links" in {
      doc(view) must haveLinkElement(
        id = "companyBenefitsLink",
        href = appConfig.otherIncomeLinkUrl,
        text = messages("tai.incomeTaxComparison.whatHappensNext.tellAboutChange.otherIncomeText")
      )
    }

    "display a link to return to PAYE Income Tax overview" in {
      val incomeTaxOverviewURL = controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage.url
      doc must haveLinkWithUrlWithID("returnToPAYEIncomeOverviewLink", incomeTaxOverviewURL)
      doc must haveLinkWithText(messages("tai.incomeTaxComparison.returnToPAYEIncomeTaxOverview.link"))
    }

    "does not show the hypothetical banner" in {
      doc(view) must not(haveH2HeadingWithText(messages("tai.incomeTaxComparison.taxCodes.banner")))
    }

    "show the hypothetical banner" in {

      doc(viewWithLess) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.taxCodes.banner"))
    }
  }

  private lazy val currentYearItem = EstimatedIncomeTaxComparisonItem(TaxYear(), 100)
  private lazy val startOfNextTaxYear = Dates.formatDate(TaxYear().next.start)
  private lazy val nextYearItemMore = EstimatedIncomeTaxComparisonItem(TaxYear().next, 101)
  private lazy val nextYearItemLess = EstimatedIncomeTaxComparisonItem(TaxYear().next, 99)

  def buildIncomeTaxComparisonViewModel(
    currentYearItem: EstimatedIncomeTaxComparisonItem,
    nextYearItem: EstimatedIncomeTaxComparisonItem,
    cyPlusOneComplete: Boolean = false): IncomeTaxComparisonViewModel =
    IncomeTaxComparisonViewModel(
      "USERNAME",
      EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItem)),
      TaxCodeComparisonViewModel(Nil),
      TaxFreeAmountComparisonViewModel(Nil, Nil),
      IncomeSourceComparisonViewModel(Nil, Nil),
      cyPlusOneComplete
    )

  private lazy val incomeTaxComparisonViewModelMore =
    buildIncomeTaxComparisonViewModel(currentYearItem, nextYearItemMore)
  private lazy val incomeTaxComparisonViewModelLess =
    buildIncomeTaxComparisonViewModel(currentYearItem, nextYearItemLess, true)
  private lazy val incomeTaxComparisonViewModelSame =
    buildIncomeTaxComparisonViewModel(currentYearItem, currentYearItem)

  def viewWithMore(implicit currMessages: Messages): Html =
    views.html.incomeTaxComparison
      .Main(incomeTaxComparisonViewModelMore, appConfig)(authRequest, currMessages, templateRenderer, partialRetriever)
  def viewWithLess(implicit currMessages: Messages): Html =
    views.html.incomeTaxComparison
      .Main(incomeTaxComparisonViewModelLess, appConfig)(authRequest, currMessages, templateRenderer, partialRetriever)
  def viewWithSame(implicit currMessages: Messages): Html =
    views.html.incomeTaxComparison
      .Main(incomeTaxComparisonViewModelSame, appConfig)(authRequest, currMessages, templateRenderer, partialRetriever)
  override def view: Html = viewWithSame
}
