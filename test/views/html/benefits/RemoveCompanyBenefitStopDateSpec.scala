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

import play.api.data.Form
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm.StopDateChoice
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.time.TaxYearResolver

class RemoveCompanyBenefitStopDateSpec extends TaiViewSpec{

  "stop date" should {

    behave like pageWithTitle(messages("tai.benefits.ended.stopDate.title"))
    behave like pageWithCombinedHeader(messages("tai.benefits.ended.journey.preHeader"),
      messages("tai.benefits.ended.stopDate.heading",benefitType, empName))
    behave like pageWithCancelLink(controllers.benefits.routes.RemoveCompanyBenefitController.cancel())
    behave like pageWithBackLink
    behave like pageWithContinueButtonForm("/check-income-tax/remove-company-benefit/stop-date")

    "have two radio buttons with relevant text" in {
      doc must haveInputLabelWithText(idBeforeTaxYearEnd, messages("tai.benefits.ended.stopDate.radio.beforeTaxYearEnd", startOfCurrentTaxYear))
      doc must haveInputLabelWithText(idOnOrAfterTaxYearEnd, messages("tai.benefits.ended.stopDate.radio.onOrAfterTaxYearEnd", startOfCurrentTaxYear))
    }

    "display an explanation text paragraph" in {
      doc.getElementById("stopDate-container").text.replace(" ", "\u00A0") mustBe Html(Messages("tai.benefits.ended.stopDate.panel",startOfCurrentTaxYear,endOfCurrentTaxYear).replace(" ", "\u00A0")).toString()
    }

    "display error message" when {
      "form has error" in {
        val errorView = views.html.benefits.removeCompanyBenefitStopDate(formWithErrors,benefitType,empName)
        doc(errorView) must haveClassWithText(messages("tai.error.chooseOneOption"), "error-message")
      }

      "a decision has not been made" in {
        val errorView = views.html.benefits.removeCompanyBenefitStopDate(formWithErrors,benefitType,empName)
        doc(errorView) must haveErrorLinkWithText(messages("tai.error.chooseOneOption"))
      }

    }
  }

  private val idBeforeTaxYearEnd = "stopDateChoice-beforetaxyearend"
  private val idOnOrAfterTaxYearEnd= "stopDateChoice-onoraftertaxyearend"
  private val startOfCurrentTaxYear = TaxYearResolver.startOfCurrentTaxYear.toString("d MMMM yyyy")
  private val endOfCurrentTaxYear = TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM YYYY")
  private lazy val benefitType = "Expenses"
  private lazy val empName = "EmployerA"

  private lazy val formWithErrors: Form[Option[String]] = RemoveCompanyBenefitStopDateForm.form.bind(Map(
    StopDateChoice -> ""
  ))

  override def view = views.html.benefits.removeCompanyBenefitStopDate(RemoveCompanyBenefitStopDateForm.form,benefitType, empName)

}

