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
import uk.gov.hmrc.tai.forms.UpdateOrRemoveCarForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.benefit.CompanyCarChoiceViewModel

class CompanyCarUserChoiceSpec extends TaiViewSpec {

  "Change company car page" should {

    behave like pageWithTitle(messages("tai.changeCompanyCar.title"))

    behave like pageWithCombinedHeader(
      messages("tai.changeCompanyCar.sub.heading"),
      messages("tai.changeCompanyCar.heading", viewModel.carModel, viewModel.carProvider))

    behave like pageWithBackLink

    behave like pageWithContinueButtonForm("/check-income-tax/end-company-car/decision")

    "display radio buttons for user journey choice" in {

      doc must haveElementAtPathWithText("form fieldset label[for=userChoice-changecardetails]",Messages("tai.changeCompanyCar.radioLabel1"))
      doc must haveElementAtPathWithText("form fieldset label[for=userChoice-removecar]",Messages("tai.changeCompanyCar.radioLabel2"))

      doc must haveElementAtPathWithId("form fieldset input","userChoice-changecardetails")
      doc must haveElementAtPathWithId("form fieldset input","userChoice-removecar")

    }

    "display a fieldset legend" in {

      doc(view).select("form fieldset legend span").text mustBe Messages("tai.changeCompanyCar.legend")
    }

    "display a cancel button" in {

      doc(view).select("#cancelLink").get(0).attributes.get("href") mustBe routes.TaxFreeAmountControllerNew.taxFreeAmount().url
    }

    "correctly summarise form errors at the page level" in {

      val emptyFormDoc = doc(views.html.benefits.updateCompanyCar(emptyForm, viewModel))

      emptyFormDoc.select(".error-summary--show > h2").text mustBe Messages("tai.income.error.form.summary")

      val errorAnchor = emptyFormDoc.select(".error-summary--show > ul > li > a").get(0)
      errorAnchor.attributes.get("href") mustBe "#userChoice"
      errorAnchor.attributes.get("id") mustBe "userChoice-error-summary"
      errorAnchor.text mustBe Messages("tai.changeCompanyCar.error.selectOption")
    }

    "correctly highlight error items at the form level" in {

      val emptyFormDoc = doc(views.html.benefits.updateCompanyCar(emptyForm, viewModel))

      emptyFormDoc.select(".error-notification").text mustBe Messages("tai.changeCompanyCar.error.selectOption")
      emptyFormDoc.select("form > div").hasClass("form-field--error") mustBe true
    }
  }

  def viewModel = CompanyCarChoiceViewModel("Car model", "Car provider")
  def cleanForm = UpdateOrRemoveCarForm.createForm.bind(Map("userChoice" -> "changeCarDetails"))
  def emptyForm = UpdateOrRemoveCarForm.createForm.bind(Map("userChoice" -> ""))
  override def view: Html = views.html.benefits.updateCompanyCar(cleanForm, viewModel)

}
