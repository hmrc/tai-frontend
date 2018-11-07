/*
 * Copyright 2018 HM Revenue & Customs
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

import play.twirl.api.Html
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.{DateHelper, TaxYearRangeUtil}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.incomeTaxComparison.{EstimatedIncomeTaxComparisonItem, EstimatedIncomeTaxComparisonViewModel, IncomeTaxComparisonViewModel}
import uk.gov.hmrc.tai.viewModels.{IncomeSourceComparisonViewModel, IncomeSourceViewModel, TaxCodeComparisonViewModel, TaxFreeAmountComparisonViewModel}
import uk.gov.hmrc.time.TaxYearResolver

class MainSpec extends TaiViewSpec {
  "Cy plus one view" must {
    behave like pageWithTitle(messages("tai.incomeTaxComparison.heading"))
    behave like pageWithCombinedHeader(preHeaderText = incomeTaxComparisonViewModel.username,
      mainHeaderText = messages("tai.incomeTaxComparison.heading"),
      preHeaderAnnouncementText = Some(messages("tai.incomeTaxComparison.preHeading.screenReader")))

    "show the income tax section with heading" in {
      doc(view) must haveSectionWithId("incomeTax")
      doc(view) must haveH2HeadingWithText(messages("tai.incomeTaxComparison.incomeTax.subHeading.more", "£100"))
    }

    "display a link to return to choose tax year page" in {
      doc must haveLinkWithUrlWithID("returnToChooseTaxYearLink", controllers.routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().url)
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
      doc(view) must haveParagraphWithText(messages("tai.incomeTaxComparison.whatHappensNext.tellUsAboutChanges.description",startOfNextTaxYear))
    }




    "have the tell us about a change links" in {
      doc(view) must haveLinkElement(id = "companyBenefitsLink",
        href = ApplicationConfig.companyBenefitsLinkUrl,
        text = messages("tai.incomeTaxComparison.tellAboutChange.companyBenefitsText"))

      doc(view) must haveLinkElement(id = "investmentIncomeLink",
        href = ApplicationConfig.investmentIncomeLinkUrl,
        text = messages("tai.incomeTaxComparison.tellAboutChange.investmentIncomeText"))

      doc(view) must haveLinkElement(id = "taxableStateBenefitLink",
        href = ApplicationConfig.taxableStateBenefitLinkUrl,
        text = messages("tai.incomeTaxComparison.tellAboutChange.stateBenefitsText"))

      doc(view) must haveLinkElement(id = "otherIncomeLink",
        href = ApplicationConfig.otherIncomeLinkUrl,
        text = messages("tai.incomeTaxComparison.tellAboutChange.otherIncomeText"))
    }



  }

  private lazy val currentYearItem = EstimatedIncomeTaxComparisonItem(TaxYear(), 100)
  private lazy val startOfNextTaxYear = Dates.formatDate(TaxYearResolver.startOfNextTaxYear)
  private lazy val nextYearItem = EstimatedIncomeTaxComparisonItem(TaxYear().next, 200)
  private lazy val estimatedIncomeTaxComparisonViewModel = EstimatedIncomeTaxComparisonViewModel(Seq(currentYearItem, nextYearItem))

  private lazy val incomeTaxComparisonViewModel = IncomeTaxComparisonViewModel("USERNAME", estimatedIncomeTaxComparisonViewModel,
    TaxCodeComparisonViewModel(Nil), TaxFreeAmountComparisonViewModel(Nil, Nil),IncomeSourceComparisonViewModel(Nil,Nil))

  override def view: Html = views.html.incomeTaxComparison.Main(incomeTaxComparisonViewModel)
}
