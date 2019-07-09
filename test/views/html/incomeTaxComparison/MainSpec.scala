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

package views.html.incomeTaxComparison

import org.jsoup.Jsoup
import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.{DateHelper, TaxYearRangeUtil}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel, IncomeTaxComparisonViewModel}
import uk.gov.hmrc.tai.viewModels.{IncomeSourceComparisonViewModel, IncomeSourceViewModel, TaxCodeComparisonViewModel, TaxFreeAmountComparisonViewModel}


class MainSpec extends TaiViewSpec {
  "Cy plus one view" must {

    "show the correct page title" in {

      doc(viewWithMore).title must include(messages("tai.incomeTaxComparison.heading.more"))
    }


    "show the correct heading when the user is estimated to pay more tax in CY plus 1" in {

      doc(viewWithMore) must haveHeadingWithText(messages("tai.incomeTaxComparison.heading.more"))
    }

    "show the correct heading when the user is estimated to pay less tax in CY plus 1" in {

      doc(viewWithLess) must haveHeadingWithText(messages("tai.incomeTaxComparison.heading.less"))

    }

    "show the correct heading when the user is estimated to pay the same tax in CY plus 1" in {

      doc(view) must haveHeadingWithText(messages("tai.incomeTaxComparison.heading.same"))
    }

    behave like pageWithCombinedHeader(preHeaderText = "USERNAME",
      mainHeaderText = messages("tai.incomeTaxComparison.heading.same"),
      preHeaderAnnouncementText = Some(messages("tai.incomeTaxComparison.preHeading.screenReader")))

    "show the income tax section with heading" in {
      doc(viewWithMore) must haveSectionWithId("incomeTax")
      doc(viewWithMore) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.more", "£1"))
    }

    "display a link to return to choose tax year page" in {
      doc must haveLinkWithUrlWithID("returnToChooseTaxYearLink", controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
      doc must haveLinkWithText(messages("your.paye.income.tax.overview"))
    }


    "show the tax codes section" in {
      doc(view) must haveSectionWithId("taxCodes")
    }

    "show the income summary section" in {
      doc(view) must haveSectionWithId("incomeSummary")
    }

    "show the tax free amount section with heading" in {
      doc(view) must haveSectionWithId("taxFreeAmount")
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.taxFreeAmount.subHeading"))
    }

    "have the what happens next with heading" in {
      doc(view) must haveSectionWithId("whatHappensNext")
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.whatHappensNext.subHeading"))
    }

    "have the what happens next section with paragraph - do nothing" in {
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.whatHappensNext.doNotDoAnything.text"))
    }

    "have the what happens next section with paragraph - estimation apply date" in {
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.whatHappensNext.estimationApplyDate.text",startOfNextTaxYear))
    }

    "have the what happens next section with paragraph - calculation may change" in {
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.whatHappensNext.calculationMayChange.text"))
    }

    "have the if information is wrong or incomplete title" in {
      doc(view) must haveHeadingH3WithText(messages("tai.incomeTaxComparison.whatHappensNext.ifInformationWrongOrIncomplete.heading"))
    }

    "have the tell us about a change paragraph" in {
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.whatHappensNext.tellAboutChange.description",startOfNextTaxYear))
    }

    "have the tell us about a change links" in {
      doc(view) must haveLinkElement(id = "companyBenefitsLink",
        href = ApplicationConfig.companyBenefitsLinkUrl,
        text = messages("tai.incomeTaxComparison.whatHappensNext.tellAboutChange.companyBenefitsText"))

      doc(view) must haveLinkElement(id = "allowancesTaxReliefsLink",
        href = ApplicationConfig.taxFreeAllowanceLinkUrl,
        text = messages("tai.incomeTaxComparison.whatHappensNext.tellAboutChange.allowanceTaxReliefText"))

      doc(view) must haveLinkElement(id = "otherIncomeLink",
        href = ApplicationConfig.otherIncomeLinkUrl,
        text = messages("tai.incomeTaxComparison.whatHappensNext.tellAboutChange.otherIncomeText"))
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
      val estimatedJourneyCompleted = IncomeTaxComparisonViewModel("USERNAME", estimatedIncomeTaxComparisonViewModel("same"),
        TaxCodeComparisonViewModel(Nil), TaxFreeAmountComparisonViewModel(Nil, Nil),IncomeSourceComparisonViewModel(Nil,Nil), true)

      def journeyCompletedView: Html = views.html.incomeTaxComparison.Main(estimatedJourneyCompleted, true)

      doc(journeyCompletedView) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.taxCodes.banner"))
    }
  }

  private lazy val currentYearItem = EstimatedIncomeTaxComparisonItem(TaxYear(), 100)
  private lazy val startOfNextTaxYear = Dates.formatDate(TaxYear().next.start)
  private lazy val nextYearItemMore = EstimatedIncomeTaxComparisonItem(TaxYear().next, 101)
  private lazy val nextYearItemSame = EstimatedIncomeTaxComparisonItem(TaxYear().next, 100)
  private lazy val nextYearItemLess = EstimatedIncomeTaxComparisonItem(TaxYear().next, 99)

  private def estimatedIncomeTaxComparisonViewModel(nextYearValue: String) = {

    nextYearValue.toLowerCase match {
      case "more" => EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItemMore))
      case "less" => EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItemLess))
      case _ => EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItemSame))
    }
  }

  val isEstimatedJourneyComplete = false

  private lazy val incomeTaxComparisonViewModelMore = IncomeTaxComparisonViewModel("USERNAME", estimatedIncomeTaxComparisonViewModel("more"),
    TaxCodeComparisonViewModel(Nil), TaxFreeAmountComparisonViewModel(Nil, Nil),IncomeSourceComparisonViewModel(Nil,Nil), isEstimatedJourneyComplete)

  private lazy val incomeTaxComparisonViewModelLess = IncomeTaxComparisonViewModel("USERNAME", estimatedIncomeTaxComparisonViewModel("less"),
    TaxCodeComparisonViewModel(Nil), TaxFreeAmountComparisonViewModel(Nil, Nil),IncomeSourceComparisonViewModel(Nil,Nil), isEstimatedJourneyComplete)

  private lazy val incomeTaxComparisonViewModelSame = IncomeTaxComparisonViewModel("USERNAME", estimatedIncomeTaxComparisonViewModel("same"),
    TaxCodeComparisonViewModel(Nil), TaxFreeAmountComparisonViewModel(Nil, Nil),IncomeSourceComparisonViewModel(Nil,Nil), isEstimatedJourneyComplete)



  def viewWithMore: Html = views.html.incomeTaxComparison.Main(incomeTaxComparisonViewModelMore, true)
  def viewWithLess: Html = views.html.incomeTaxComparison.Main(incomeTaxComparisonViewModelLess, true)
  override def view: Html = views.html.incomeTaxComparison.Main(incomeTaxComparisonViewModelSame, true)
}
