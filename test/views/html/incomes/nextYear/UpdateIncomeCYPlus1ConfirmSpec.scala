/*
 * Copyright 2021 HM Revenue & Customs
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

import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.DateHelper.toDisplayFormat
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.{ConfirmAmountEnteredViewModel, IrregularPay}

class UpdateIncomeCYPlus1ConfirmSpec extends TaiViewSpec {

  val employerName = "employerName"
  val currentAmount = 1234
  val estimatedAmount = 1000
  val employmentId = 1

  val vm = ConfirmAmountEnteredViewModel(employmentId, employerName, currentAmount, estimatedAmount, IrregularPay)
  private val template = inject[updateIncomeCYPlus1Confirm]

  override lazy val view: Html = template(vm)

  "Edit income Irregular Hours view" should {
    behave like pageWithBackLink
    behave like pageWithTitle(messages("tai.irregular.title"))
    behave like pageWithCombinedHeader(
      messages("tai.updateIncome.CYPlus1.preheading", employerName),
      messages("tai.incomes.confirm.save.heading", TaxYearRangeUtil.currentTaxYearRangeSingleLine)
    )

    "display the users current estimated income" in {
      doc(view) must haveParagraphWithText(messages("tai.updateIncome.CYPlus1.confirm.paragraph") + " £1,000")
    }

    "display a message explaining the results of changing the estimated pay" in {
      doc(view) must haveParagraphWithText(messages("tai.updateIncome.CYPlus1.confirm.details.p1"))
      doc(view) must haveParagraphWithText(messages("tai.updateIncome.CYPlus1.confirm.details.p2"))
    }

    "display a message explaining when change will take effect" in {
      doc(view) must haveParagraphWithText(
        messages("tai.updateIncome.CYPlus1.confirm.changeEffectiveFrom", toDisplayFormat(Some(TaxYear().next.start)))
      )
    }

    "display a confirm and send button" in {
      doc(view) must haveLinkElement(
        id = "confirmAndSend",
        href = s"/update-income/edit-income-irregular-hours/$employmentId/submit",
        text = messages("tai.confirmAndSend")
      )
    }

    "display a cancel link" in {
      doc(view) must haveLinkWithText(messages("tai.cancel.noSave"))

    }
  }

}
