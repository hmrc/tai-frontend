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

package views.html.incomes

import uk.gov.hmrc.tai.forms.incomes.bbsi.UpdateInterestForm
import uk.gov.hmrc.tai.viewModels.income.BbsiUpdateAccountViewModel
import play.api.data.Form
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BankBuildingSocietyUpdateInterestSpec extends TaiViewSpec {

  "Update Interest view" should {

    behave like pageWithTitle(messages("tai.bbsi.update.captureInterest.title", bankName))
    behave like pageWithCombinedHeader(messages("tai.bbsi.update.captureInterest.preHeading"),
      messages("tai.bbsi.update.captureInterest.title", bankName))
    behave like pageWithContinueButtonForm("/check-income-tax/income/bank-building-society-savings/1/update/interest")
    behave like pageWithBackLink
    behave like pageWithCancelLink(controllers.income.bbsi.routes.BbsiController.accounts())

    "display description" in {
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.para1",
        TaxYear().start.toString(dateFormat),
        TaxYear().end.toString(dateFormat),
        "£1,000"))

      doc must haveInputLabelWithText("untaxedInterest", messages("tai.bbsi.update.captureInterest.textBox.title"))
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.para2"))
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.para3"))
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.accordion.desc1"))
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.accordion.desc2"))
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.para4",
        TaxYear().year.toString,
        TaxYear().end.getYear.toString,
        TaxYear().end.toString(dateFormat)))
    }

    "display error message" when {
      "untaxed interest is empty" in {
        val view: Html = views.html.incomes.bbsi.update.bank_building_society_update_interest(model, untaxedInterestErrorForm)

        doc(view) must haveErrorLinkWithText(messages("tai.bbsi.update.form.interest.blank"))
      }
    }

    "display accordion" in {
      doc must haveSummaryWithText(messages("tai.bbsi.update.captureInterest.accordion.title"))
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.accordion.desc1"))
      doc must haveParagraphWithText(messages("tai.bbsi.update.captureInterest.accordion.desc2"))
    }

  }

  private lazy val dateFormat = "d MMMM yyyy"
  private lazy val bankName = "TEST"
  private lazy val interest = 1000
  private lazy val id = 1

  private lazy val model = BbsiUpdateAccountViewModel(id, interest, bankName)

  private lazy val untaxedInterestForm: Form[String] = UpdateInterestForm.form.bind(Map(
    "untaxedInterest" -> "1000"
  ))

  private lazy val untaxedInterestErrorForm: Form[String] = UpdateInterestForm.form.bind(Map(
    "untaxedInterest" -> ""
  ))

  override def view: Html = views.html.incomes.bbsi.update.bank_building_society_update_interest(model, untaxedInterestForm)
}

