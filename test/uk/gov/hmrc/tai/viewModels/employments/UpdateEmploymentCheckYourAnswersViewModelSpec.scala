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

package uk.gov.hmrc.tai.viewModels.employments

import controllers.FakeTaiPlayApplication
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

class UpdateEmploymentCheckYourAnswersViewModelSpec extends PlaySpec with FakeTaiPlayApplication {

  "Update employment check your answers view model" must {
    "return journey lines without phone number line" when {
      "contactByPhone is No" in {
        val model = UpdateEmploymentCheckYourAnswersViewModel(1, "Test", "what", "No", None)

        model.journeyConfirmationLines.size mustBe 3
        model.journeyConfirmationLines mustBe Seq(currentlyWorkHereLine, whatYouToldUsLine, contactByPhoneLine.copy(answer = "No"))
      }
    }

    "return journey lines with phone number line" when {
      "contactByPhone is Yes" in {
        val model = UpdateEmploymentCheckYourAnswersViewModel(1, "Test", "what", "Yes", Some("1234567890"))

        model.journeyConfirmationLines.size mustBe 4
        model.journeyConfirmationLines mustBe Seq(currentlyWorkHereLine, whatYouToldUsLine, contactByPhoneLine, phoneNumberLine)
      }
    }
  }

  private lazy val currentlyWorkHereLine = CheckYourAnswersConfirmationLine(
    Messages("tai.updateEmployment.cya.currentlyWorkHere"),
    Messages("tai.label.yes"),
    controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision.url
  )

  private lazy val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.whatYouToldUs"),
    "what",
    controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(1).url
  )

  private lazy val contactByPhoneLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.contactByPhone"),
    "Yes",
    controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
  )

  private lazy val phoneNumberLine = CheckYourAnswersConfirmationLine(
    Messages("tai.phoneNumber"),
    "1234567890",
    controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
  )

}
