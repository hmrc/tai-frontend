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

import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class CheckYourAnswersSpec extends TaiViewSpec {

  val employerName = "employer1"

  "checkYourAnswers" should {

    behave like pageWithTitle(messages("tai.checkYourAnswers"))
    behave like pageWithCombinedHeader(messages("tai.incomes.edit.preHeading",employerName), messages("tai.checkYourAnswers"))
    behave like pageWithCancelLink(controllers.routes.TaxAccountSummaryController.onPageLoad)
    behave like pageWithBackLink

    "display confirmation static text" in{
      doc must haveParagraphWithText(messages("tai.checkYourAnswers.confirmText"))
    }
  }



  override def view = views.html.incomes.checkYourAnswers(employerName)
}