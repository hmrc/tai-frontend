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

package views.html.incomes

import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.{MonetaryUtil, TaxYearRangeUtil}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.ConfirmAmountEnteredViewModel

class ConfirmAmountEnteredSpec extends TaiViewSpec with MockitoSugar {

  val employerName = "employerName"
  val currentAmount = 1234
  val estimatedAmount = 1000
  val employmentId = 1

  val vm = ConfirmAmountEnteredViewModel.irregularPayCurrentYear(employmentId, employerName, currentAmount, estimatedAmount)
  override lazy val view: Html = views.html.incomes.confirmAmountEntered(vm)

  "Edit income Irregular Hours view" should {
    behave like pageWithBackLink
    behave like pageWithTitle(messages("tai.irregular.title"))
    behave like pageWithCombinedHeader(
      messages("tai.payPeriod.preHeading", employerName),
      messages("tai.incomes.confirm.save.heading", TaxYearRangeUtil.currentTaxYearRangeSingleLine))

    "display the users current estimated income" in {
      val mainText = messages("tai.irregular.confirm.estimatedIncome")
      val amount = MonetaryUtil.withPoundPrefix(estimatedAmount)
      doc(view) must haveParagraphWithText( s"$mainText $amount")
    }

    "display a message explaining the results of changing the estimated pay" in {
      doc(view) must haveParagraphWithText(messages("tai.incomes.confirm.save.message.details.p1"))
      doc(view) must haveParagraphWithText(messages("tai.incomes.confirm.save.message.details.p2"))
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
