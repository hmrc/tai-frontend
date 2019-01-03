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

package views.html.benefits

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.benefits.CompanyBenefitTotalValueForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.benefit.BenefitViewModel

class RemoveBenefitTotalValuePageSpec extends TaiViewSpec {

  "removeBenefitTotalValue" must {
    behave like pageWithTitle(Messages("tai.remove.company.benefit.total.value.heading", benefitName, employerName))
    behave like pageWithCombinedHeader(Messages("tai.benefits.ended.journey.preHeader"),
      Messages("tai.remove.company.benefit.total.value.heading", benefitName, employerName))

    behave like pageWithContinueButtonForm("/check-income-tax/remove-company-benefit/total-value-of-benefit")
    behave like pageWithCancelLink(controllers.benefits.routes.RemoveCompanyBenefitController.cancel())
    behave like pageWithBackLink

    "contain static paragraph with text" in {
      doc must haveParagraphWithText(Messages("tai.remove.company.benefit.total.value.dontKnow"))
    }
    "contain static bullet points with text" in {
      doc must haveBulletPointWithText(Messages("tai.remove.company.benefit.total.value.ask"))
      doc must haveBulletPointWithText(Messages("tai.remove.company.benefit.total.value.enter"))
    }
    "contain label with static text" in {
      val label = doc(view).select("form .form-label").get(0).text
      label must include(Messages("tai.remove.company.benefit.total.value.value"))
      label must include(Messages("tai.inPounds"))

    }
    "contain hint with static text" in {
      val hint = doc(view).select("form .form-hint").get(0).text
      hint mustBe Messages("tai.remove.company.benefit.total.value.hint")
    }
    "contain summary with text and a hidden text" in {
      doc must haveSummaryWithText(Messages("tai.remove.company.benefit.total.value.whatHappens.link"))
      doc must haveSpanWithText(Messages("tai.remove.company.benefit.total.value.whatHappens.desc", uk.gov.hmrc.time.TaxYearResolver.startOfCurrentTaxYear.toString("yyyy"),uk.gov.hmrc.time.TaxYearResolver.endOfCurrentTaxYear.toString("yyyy"), uk.gov.hmrc.time.TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM yyyy")))
    }
    "contain an input field with pound symbol appended" in {
      doc must haveElementAtPathWithId("input", "totalValue")
      doc must haveElementAtPathWithClass("input", "form-control-currency")
    }

  }

  private lazy val employerName = "HMRC"
  private lazy val benefitName = "Other Benefit"

  override def view: Html = views.html.benefits.removeBenefitTotalValue(BenefitViewModel(employerName, benefitName), CompanyBenefitTotalValueForm.form)

}
