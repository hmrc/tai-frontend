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

package views.html.benefits

import controllers.routes
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.benefit.CompanyCarCheckAnswersViewModel

class CompanyCarCheckYourAnswersSpec extends TaiViewSpec {

  "Company car check your answers page" must {

    behave like pageWithTitle(messages("tai.companyCar.checkAnswers.title"))

    behave like pageWithCombinedHeader(
      messages("tai.companyCar.checkAnswers.sub.heading"),
      messages("tai.companyCar.checkAnswers.heading"))

    behave like pageWithButtonForm(routes.CompanyCarController.checkYourAnswers.url,
      messages("tai.confirmAndSend"))

    behave like pageWithCheckYourAnswersSummary

    "display a back button" when {
      "there is no fuel benefit" in{
        val docWithoutFuelDate = doc(views.html.benefits.companyCarCheckYourAnswers(companyCarViewModelWithoutFuelStopDate))
        docWithoutFuelDate must haveBackLink
      }
      "there is a fuel benefit" in{
        doc must haveBackLink
      }
    }

    "display the header for the table with the car information" in {
      doc must haveHeadingH2WithText(messages("tai.companyCar.checkAnswers.table.heading", companyCarViewModel.carModel, companyCarViewModel.carProvider))
    }

    "display the first check your answers summary line (car end date) at all times" in {
      doc must haveCheckYourAnswersSummaryLine(1, Messages("tai.companyCar.checkAnswers.table.rowOne.description"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(1, routes.CompanyCarController.getCompanyCarEndDate().url + "?edit=true")
    }

    "display the second check your answers summary line (fuel end date) when a fuel benefit stop date is present " in {
      doc must haveCheckYourAnswersSummaryLine(2, Messages("tai.companyCar.checkAnswers.table.rowTwo.description"))
      doc must haveCheckYourAnswersSummaryLineChangeLink(2, routes.CompanyCarController.getFuelBenefitEndDate().url + "?edit=true")
    }

    "doesn't display the second table row when there is no fuel benefit stop date" in {
      val docWithoutFuelDate = doc(views.html.benefits.companyCarCheckYourAnswers(companyCarViewModelWithoutFuelStopDate))
      docWithoutFuelDate mustNot haveCheckYourAnswersSummaryLine(2, Messages("tai.companyCar.checkAnswers.table.rowTwo.description"))
    }

    "display the last paragraph with fuel benefit part" in {
      doc must haveParagraphWithText(messages("tai.companyCar.checkAnswers.panel",
        companyCarViewModel.dateGivenBack,
        companyCarViewModel.dateFuelBenefitStopped,
        companyCarViewModel.taxYearStart,
        companyCarViewModel.taxYearEnd))
    }

    "display the last paragraph without fuel benefit part" in {
      val docWithoutFuelDate = doc(views.html.benefits.companyCarCheckYourAnswers(companyCarViewModelWithoutFuelStopDate))

      docWithoutFuelDate must haveParagraphWithText(messages("tai.companyCar.checkAnswers.panel.withoutFuel",
        companyCarViewModel.dateGivenBack,
        companyCarViewModel.taxYearStart,
        companyCarViewModel.taxYearEnd))
    }

    "display a cancel button" in {
      val cancelButton = doc.select("#cancelLink")
      cancelButton.size > 0
      cancelButton.get(0).attributes.get("href") mustBe routes.TaxFreeAmountController.taxFreeAmount().url
    }
  }

  val companyCarViewModel = CompanyCarCheckAnswersViewModel(carModel = "Some car model",
    carProvider = "Some Company",
    dateGivenBack = "22 June 2017",
    dateFuelBenefitStopped = "21 June 2017",
    taxYearStart = "2017",
    taxYearEnd = "2018")

  val companyCarViewModelWithoutFuelStopDate = companyCarViewModel.copy(dateFuelBenefitStopped = "")

  override def view: Html = views.html.benefits.companyCarCheckYourAnswers(companyCarViewModel)
}
