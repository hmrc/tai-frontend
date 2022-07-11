/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.benefit.BenefitViewModel

import java.time.format.DateTimeFormatter

class RemoveBenefitTotalValueViewSpec extends TaiViewSpec {

  "removeBenefitTotalValue" must {
    behave like pageWithTitle(Messages("tai.remove.company.benefit.total.value.heading", benefitName, employerName))
    behave like pageWithCombinedHeaderNewTemplate(
      Messages("tai.benefits.ended.journey.preHeader"),
      Messages("tai.remove.company.benefit.total.value.heading", benefitName, employerName),
      Some(messages("tai.ptaHeader.accessible.preHeading"))
    )

    behave like pageWithContinueButtonFormNew("/check-income-tax/remove-company-benefit/total-value-of-benefit")
    behave like pageWithCancelLink(controllers.benefits.routes.RemoveCompanyBenefitController.cancel())
    behave like haveBackButtonWithUrl(controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url)

    "contain static paragraph with text" in {
      doc must haveParagraphWithText(Messages("tai.remove.company.benefit.total.value.dontKnow"))
    }
    "contain static bullet points with text" in {
      doc must haveBulletPointWithText(Messages("tai.remove.company.benefit.total.value.ask"))
      doc must haveBulletPointWithText(Messages("tai.remove.company.benefit.total.value.enter"))
    }
    "contain label with static text" in {
      val label = doc(view).select("#valueOfBenefit").get(0).text
      label must include(Messages("tai.remove.company.benefit.total.value.value"))
      label must include(Messages("tai.inPounds"))

    }
    "contain hint with static text" in {
      val hint = doc(view).select("#valueOfBenefit").get(0).text
      hint must include(Messages("tai.remove.company.benefit.total.value.hint"))
    }
    "contain summary with text and a hidden text" in {
      doc must haveSummaryWithText(Messages("tai.remove.company.benefit.total.value.whatHappens.link"))
      doc must haveDetailsWithText(
        Messages(
          "tai.remove.company.benefit.total.value.whatHappens.desc",
          TaxYear().start.format(DateTimeFormatter.ofPattern("yyyy")),
          TaxYear().end.format(DateTimeFormatter.ofPattern("yyyy")),
          TaxYear().end.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        ))
    }
    "contain an input field with pound symbol appended" in {
      doc must haveElementAtPathWithId("input", "totalValue")
      doc must haveElementAtPathWithClass("div", "govuk-input__prefix")
    }

  }

  private lazy val employerName = "HMRC"
  private lazy val benefitName = "Other Benefit"

  private val template = inject[RemoveBenefitTotalValueView]

  override def view: Html =
    template(BenefitViewModel(employerName, benefitName), CompanyBenefitTotalValueForm.form)

}
