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

package uk.gov.hmrc.tai.viewModels.pensions.update

import play.api.i18n.Messages
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import utils.BaseSpec

class UpdatePensionCheckYourAnswersViewModelSpec extends BaseSpec {

  "Update pension check your answers view model" must {
    "generate journey lines without a phone number line" when {
      "contactByPhone property is No" in {
        val model =
          UpdatePensionCheckYourAnswersViewModel(1, "PensionProvider1", "Yes", "My pension decreased", "No", None)

        model.journeyConfirmationLines.size mustBe 3
        model.journeyConfirmationLines mustBe Seq(
          receivePensionLine,
          whatYouToldUsLine,
          contactByPhoneLine.copy(answer = "No"))
      }
    }

    "generate journey lines with a phone number line" when {
      "contactByPhone property is Yes" in {
        val model = UpdatePensionCheckYourAnswersViewModel(
          1,
          "PensionProvider2",
          "Yes",
          "My pension decreased",
          "Yes",
          Some("1234567890"))

        model.journeyConfirmationLines.size mustBe 4
        model.journeyConfirmationLines mustBe Seq(
          receivePensionLine,
          whatYouToldUsLine,
          contactByPhoneLine,
          phoneNumberLine)
      }
    }
  }

  private lazy val receivePensionLine = CheckYourAnswersConfirmationLine(
    Messages("tai.updatePension.cya.currentlyReceivePension"),
    Messages("tai.label.yes"),
    controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension().url
  )

  private lazy val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.whatYouToldUs"),
    "My pension decreased",
    controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs().url
  )

  private lazy val contactByPhoneLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.contactByPhone"),
    "Yes",
    controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url
  )

  private lazy val phoneNumberLine = CheckYourAnswersConfirmationLine(
    Messages("tai.phoneNumber"),
    "1234567890",
    controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url
  )

}
