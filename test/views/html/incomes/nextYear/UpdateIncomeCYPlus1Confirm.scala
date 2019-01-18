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

package views.html.incomes.nextYear

import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.DateHelper.toDisplayFormat
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel
import uk.gov.hmrc.tai.viewModels.GoogleAnalyticsSettings

class UpdateIncomeCYPlus1Confirm extends TaiViewSpec with MockitoSugar {

  private val employerName = "employerName"
  private val estimatedAmount = 1000
  private val employmentId = 1

  "Edit income Irregular Hours view" should {
    behave like pageWithBackLink
    behave like pageWithTitle(messages("tai.irregular.title"))
    behave like pageWithCombinedHeader(
      messages("tai.updateIncome.CYPlus1.preheading", employerName),
      messages("tai.irregular.confirm.mainHeading", TaxYearRangeUtil.currentTaxYearRange))

    "display the users current estimated income" in {
      doc(view) must haveParagraphWithText(messages("tai.irregular.confirm.estimatedIncome", "£1,000"))
    }

    "display a message explaining the results of changing the estimated pay" in {
      doc(view) must haveParagraphWithText(messages("tai.irregular.confirm.effectOfChange"))
    }

    "display a message explaining when change will take effect" in {
      doc(view) must haveParagraphWithText(
        messages("tai.updateIncome.CYPlus1.confirm.changeEffectiveFrom",
                 toDisplayFormat(Some(TaxYear().next.start)))
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

  val vm = ConfirmAmountEnteredViewModel.irregularPayCurrentYear(employmentId, employerName, estimatedAmount)
  override lazy val view: Html = views.html.incomes.nextYear.updateIncomeCYPlus1Confirm(vm)
}
