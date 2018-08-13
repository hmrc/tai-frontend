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

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{TaxCodeHistory, TaxCodeRecord}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class TaxCodeComparisonViewSpec extends TaiViewSpec {

  val date = new LocalDate(2018, 5, 23)
  val taxCodeRecord1 = TaxCodeRecord(TaxYear(2018), 1, "A1111", date, date.plusDays(1),"Employer 1")
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = date.plusMonths(1), endDate = date.plusMonths(1).plusDays(1))
  val taxCodeHistory: TaxCodeHistory = TaxCodeHistory(taxCodeRecord1, taxCodeRecord2)

  override def view = views.html.taxCodeChange.taxCodeComparison(taxCodeHistory)

  "tax code comparison" should {
    behave like pageWithBackLink

    behave like pageWithTitle(Messages("taxCode.change.journey.preHeading"))

    behave like pageWithCombinedHeader(
      preHeaderText = Messages("taxCode.change.journey.preHeading"),
      mainHeaderText = Messages("taxCode.change.yourTaxCodeChanged.h1", Dates.formatDate(taxCodeHistory.mostRecentTaxCodeChangeDate)))

    "display the correct paragraphs" in {
      doc(view) must haveParagraphWithText(Messages("taxCode.change.yourTaxCodeChanged.paragraph"))
    }

    "display the previous tax code" in {
      doc(view) must haveHeadingH2WithText(taxCodeHistory.previous.employerName)
      doc(view) must haveHeadingH3WithText(Messages("tai.taxCode.title.pt2", Dates.formatDate(taxCodeHistory.previous.startDate), Dates.formatDate(taxCodeHistory.previous.endDate)))
      doc(view).toString must include(taxCodeHistory.previous.taxCode)
    }

    "display the current tax code" in {
      doc(view) must haveHeadingH2WithText(taxCodeHistory.current.employerName)
      doc(view) must haveHeadingH3WithText(Messages("tai.taxCode.title.pt2", Dates.formatDate(taxCodeHistory.current.startDate), Dates.formatDate(taxCodeHistory.current.endDate)))
      doc(view).toString must include(taxCodeHistory.current.taxCode)
    }
  }

}
