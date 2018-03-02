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

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec


class RemoveBankBuildingSocietyCheckYourAnswersSpec extends TaiViewSpec{

  private val id = 1
  private val bankName = "TEST"

  "Remove bank account view" should {
    behave like pageWithTitle(messages("tai.bbsi.remove.checkYourAnswers.title", bankName))
    behave like pageWithBackButton(controllers.income.bbsi.routes.BbsiController.decision(id))
    behave like pageWithCancelLink(controllers.income.bbsi.routes.BbsiController.accounts())
    behave like pageWithButtonForm("/check-income-tax/income/bank-building-society-savings/1/remove/check-your-answers",
      messages("tai.submit"))
    behave like pageWithCombinedHeader(messages("tai.bbsi.remove.checkYourAnswers.preHeading"),
      messages("tai.bbsi.remove.checkYourAnswers.title", bankName))

    "display description" in {
      doc must haveParagraphWithText(messages("tai.bbsi.remove.checkYourAnswers.description", bankName))
    }
  }

  override def view: Html = views.html.incomes.bbsi.remove.bank_building_society_check_your_answers(id, bankName)
}
