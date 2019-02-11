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

import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import play.api.Play.current
import play.api.i18n.Messages


case class UpdateEmploymentCheckYourAnswersViewModel(id: Int,
                                                     employerName: String,
                                                     whatYouToldUs: String,
                                                     contactByPhone: String,
                                                     phoneNumber: Option[String]) {
  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {
    val currentlyWorkHereLine = CheckYourAnswersConfirmationLine(
      Messages("tai.updateEmployment.cya.currentlyWorkHere"),
      Messages("tai.label.yes"),
      controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision.url
    )
    val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.whatYouToldUs"),
      whatYouToldUs,
      controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(id).url
    )
    val contactByPhoneLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.contactByPhone"),
      contactByPhone,
      controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
    )
    val phoneNumberLine = CheckYourAnswersConfirmationLine(
      Messages("tai.phoneNumber"),
      phoneNumber.getOrElse(""),
      controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
    )

    if (contactByPhone == Messages("tai.label.yes")) {
      Seq(currentlyWorkHereLine, whatYouToldUsLine, contactByPhoneLine, phoneNumberLine)
    } else {
      Seq(currentlyWorkHereLine, whatYouToldUsLine, contactByPhoneLine)
    }
  }
}