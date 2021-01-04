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

package uk.gov.hmrc.tai.viewModels.pensions.update

import play.api.i18n.Messages
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

case class UpdatePensionCheckYourAnswersViewModel(
  id: Int,
  pensionProviderName: String,
  receivePension: String,
  whatYouToldUs: String,
  contactByPhone: String,
  phoneNumber: Option[String]) {

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {

    val receivePensionLine = CheckYourAnswersConfirmationLine(
      Messages("tai.updatePension.cya.currentlyReceivePension"),
      receivePension,
      controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension().url
    )
    val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.whatYouToldUs"),
      whatYouToldUs,
      controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs().url
    )
    val contactByPhoneLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.contactByPhone"),
      contactByPhone,
      controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url
    )
    val phoneNumberLine = CheckYourAnswersConfirmationLine(
      Messages("tai.phoneNumber"),
      phoneNumber.getOrElse(""),
      controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber().url
    )

    if (contactByPhone.equals(Messages("tai.label.yes"))) {
      Seq(receivePensionLine, whatYouToldUsLine, contactByPhoneLine, phoneNumberLine)
    } else {
      Seq(receivePensionLine, whatYouToldUsLine, contactByPhoneLine)
    }
  }
}
