/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.i18n.Messages
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

case class UpdateEmploymentCheckYourAnswersViewModel(
  id: Int,
  employerName: String,
  whatYouToldUs: String,
  contactByPhone: String,
  phoneNumber: Option[String]
) {
  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {
    val currentlyWorkHereLine = CheckYourAnswersConfirmationLine(
      messages("tai.updateEmployment.cya.currentlyWorkHere"),
      messages("tai.label.yes"),
      controllers.employments.routes.EndEmploymentController.onPageLoad(id).url
    )
    val whatYouToldUsLine     = CheckYourAnswersConfirmationLine(
      messages("tai.checkYourAnswers.whatYouToldUs"),
      whatYouToldUs,
      controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(id).url
    )
    val contactByPhoneLine    = CheckYourAnswersConfirmationLine(
      messages("tai.checkYourAnswers.contactByPhone"),
      contactByPhone,
      controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
    )
    val phoneNumberLine       = CheckYourAnswersConfirmationLine(
      messages("tai.phoneNumber"),
      phoneNumber.getOrElse(""),
      controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber().url
    )

    if (contactByPhone == messages("tai.label.yes")) {
      Seq(currentlyWorkHereLine, whatYouToldUsLine, contactByPhoneLine, phoneNumberLine)
    } else {
      Seq(currentlyWorkHereLine, whatYouToldUsLine, contactByPhoneLine)
    }
  }
}
