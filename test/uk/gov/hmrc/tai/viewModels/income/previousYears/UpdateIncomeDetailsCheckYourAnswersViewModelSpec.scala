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

package uk.gov.hmrc.tai.viewModels.income.previousYears

import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import utils.BaseSpec

class UpdateIncomeDetailsCheckYourAnswersViewModelSpec extends BaseSpec {

  "Update income details check your answers view model" must {
    "return journey lines without phone number line" when {
      "contactByPhone is No" in {
        val model = UpdateIncomeDetailsCheckYourAnswersViewModel("2016-2017", "something", "No", None)

        model.journeyConfirmationLines.size mustBe 2
        model.journeyConfirmationLines mustBe Seq(whatYouToldUsLine, contactByPhoneLine.copy(answer = "No"))
      }
    }

    "return journey lines with phone number line" when {
      "contactByPhone is Yes" in {
        val model = UpdateIncomeDetailsCheckYourAnswersViewModel("2016-2017", "something", "Yes", Some("1234567890"))

        model.journeyConfirmationLines.size mustBe 3
        model.journeyConfirmationLines mustBe Seq(whatYouToldUsLine, contactByPhoneLine, phoneNumberLine)
      }
    }
    "return a view model with correct table header based on tax year" in {
      implicit lazy val lang: Lang = Lang("en")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      val model = UpdateIncomeDetailsCheckYourAnswersViewModel(TaxYear(2016), "something", "Yes", Some("1234567890"))
      val dateRange = HtmlFormatter.htmlNonBroken("6 April 2016") + " to " + HtmlFormatter.htmlNonBroken("5 April 2017")
      model.tableHeader mustBe Messages("tai.income.previousYears.decision.header", dateRange)
    }

    "return a view model with correct table header based on tax year in welsh" in {
      implicit lazy val lang: Lang = Lang("cy")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      val model = UpdateIncomeDetailsCheckYourAnswersViewModel(TaxYear(2016), "something", "Yes", Some("1234567890"))
      val dateRange = HtmlFormatter.htmlNonBroken("6 Ebrill 2016") + " i " + HtmlFormatter.htmlNonBroken(
        "5 Ebrill 2017")
      model.tableHeader mustBe Messages("tai.income.previousYears.decision.header", dateRange)
    }
  }

  private lazy val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.whatYouToldUs"),
    "something",
    controllers.income.previousYears.routes.UpdateIncomeDetailsController.details().url
  )

  private lazy val contactByPhoneLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.contactByPhone"),
    "Yes",
    controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber().url
  )

  private lazy val phoneNumberLine = CheckYourAnswersConfirmationLine(
    Messages("tai.phoneNumber"),
    "1234567890",
    controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber().url
  )

}
