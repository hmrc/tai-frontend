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

package views.html.taxCodeChange

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.time.TaxYearResolver

class TaxCodeComparisonViewSpec extends TaiViewSpec {

  val startDate = TaxYearResolver.startOfCurrentTaxYear
  val taxCodeRecord1 = TaxCodeRecord("tax code", startDate, startDate.plusDays(1),"Employer 1")
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(2), endDate = TaxYearResolver.endOfCurrentTaxYear)
  val taxCodeChange: TaxCodeChange = TaxCodeChange(taxCodeRecord1, taxCodeRecord2)

  override def view = views.html.taxCodeChange.taxCodeComparison(taxCodeChange)

  "tax code comparison" should {
    behave like pageWithBackLink

    behave like pageWithTitle(Messages("taxCode.change.journey.preHeading"))

    behave like pageWithCombinedHeader(
      preHeaderText = Messages("taxCode.change.journey.preHeading"),
      mainHeaderText = Messages("taxCode.change.yourTaxCodeChanged.h1", Dates.formatDate(taxCodeChange.mostRecentTaxCodeChangeDate)))

    "display the correct paragraphs" in {
      doc(view) must haveParagraphWithText(Messages("taxCode.change.yourTaxCodeChanged.paragraph"))
    }

    "display the previous tax code" in {
      doc(view) must haveHeadingH2WithText(taxCodeChange.previous.employerName)
      doc(view) must haveHeadingH3WithText(Messages("tai.taxCode.title.pt2", Dates.formatDate(taxCodeChange.previous.startDate), Dates.formatDate(taxCodeChange.previous.endDate)))
      doc(view).toString must include(taxCodeChange.previous.taxCode)
    }

    "display the current tax code" in {
      doc(view) must haveHeadingH2WithText(taxCodeChange.current.employerName)
      doc(view) must haveHeadingH3WithText(Messages("tai.taxCode.title.pt2", Dates.formatDate(taxCodeChange.current.startDate), Dates.formatDate(taxCodeChange.current.endDate)))
      doc(view).toString must include(taxCodeChange.current.taxCode)
    }

    "display a button linking to the 'check your tax-free amount page" in {
      doc(view) must haveLinkElement("check-your-tax-button", routes.TaxCodeChangeController.yourTaxFreeAmount().url.toString, Messages("taxCode.change.yourTaxCodeChanged.checkYourTaxButton"))
    }
  }

}
