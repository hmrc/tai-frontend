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

package views.html.incomes.nextYear

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import uk.gov.hmrc.tai.util.ViewModelHelper.{currentTaxYearRangeHtmlNonBreak, withPoundPrefix}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class updateIncomeCYPlus1EditSpec extends TaiViewSpec {

  val employerName = "Employer Name"
  val employmentID = 1
  val currentEstPay = 1234
  val isPension = false


  "CYPlus1 Edit Page" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET",controllers.routes.IncomeTaxComparisonController.onPageLoad.url))
    behave like pageWithCombinedHeader(
      messages("tai.updateIncome.CYPlus1.preheading", employerName),
      messages("tai.updateIncome.CYPlus1.edit.heading", currentTaxYearRangeHtmlNonBreak))
    behave like pageWithContinueInputForm(controllers.income.routes.UpdateIncomeNextYearController.edit(employmentID).url)

    "contain the correct content when income is from employment" in {
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.edit.paragraph1"))
      doc(view).getElementsByTag("p").text must include(messages("tai.incomes.edit.howTo.grossAmount"))
      doc(view).getElementsByTag("li").text must include(messages("tai.incomes.edit.howTo.contribution"))
      doc(view).getElementsByTag("li").text must include(messages("tai.incomes.edit.howTo.charity"))
      doc(view).getElementsByTag("li").text must include(messages("tai.incomes.edit.howTo.expenses"))
      doc(view).getElementsByTag("h2").text must include(messages("tai.updateIncome.CYPlus1.edit.subheading", employerName))
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.edit.wholePounds"))
    }

    "contain the correct content when income is from pension" in {
      val isPension = true
      val pensionView: Html = views.html.incomes.nextYear.updateIncomeCYPlus1Edit(employerName, employmentID, isPension, currentEstPay,
        AmountComparatorForm.createForm(taxablePayYTD = Some(currentEstPay)))

      doc(pensionView).getElementsByTag("p").text mustNot include(messages("tai.updateIncome.CYPlus1.edit.paragraph1", employerName))
      doc(pensionView).getElementsByTag("p").text mustNot include(messages("tai.incomes.edit.howTo.grossAmount", employerName))
      doc(pensionView).getElementsByTag("li").text mustNot include(messages("tai.incomes.edit.howTo.contribution", employerName))
      doc(pensionView).getElementsByTag("li").text mustNot include(messages("tai.incomes.edit.howTo.contribution", employerName))
      doc(pensionView).getElementsByTag("li").text mustNot include(messages("tai.incomes.edit.howTo.charity", employerName))
      doc(pensionView).getElementsByTag("li").text mustNot include(messages("tai.incomes.edit.howTo.expenses", employerName))

      doc(pensionView).getElementsByTag("h2").text must include(messages("tai.updateIncome.CYPlus1.edit.subheading", employerName))
      doc(pensionView).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.edit.wholePounds"))
    }

    "display the users current estimated income" in {
      doc(view) must haveClassWithText(messages("tai.irregular.currentAmount"), "form-label")
      doc(view) must haveParagraphWithText(withPoundPrefix(MoneyPounds(BigDecimal(currentEstPay),0)))
    }

    "have an input box for user to enter new amount" in {
      doc(view) must haveInputLabelWithText("income",
        messages("tai.irregular.newAmount") + " " + messages("tai.inPounds")
      )
      doc(view).getElementsByClass("form-control-currency").size() mustBe 1
    }
  }


  override def view: Html = views.html.incomes.nextYear.updateIncomeCYPlus1Edit(employerName, employmentID, isPension, currentEstPay,
    AmountComparatorForm.createForm(taxablePayYTD = Some(currentEstPay)))
}
