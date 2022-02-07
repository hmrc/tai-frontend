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

package uk.gov.hmrc.tai.viewModels.pensions

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

case class CheckYourAnswersViewModel(
  preHeading: String,
  backLinkUrl: String,
  title: String,
  journeyConfirmationLines: Seq[CheckYourAnswersConfirmationLine],
  postConfirmationText: String,
  submissionUrl: String,
  cancelUrl: String)

object CheckYourAnswersViewModel {

  def apply(
    pensionProviderName: String,
    pensionStartDate: String,
    pensionRefNo: String,
    contactableByPhone: String,
    phoneNumber: Option[String])(implicit messages: Messages): CheckYourAnswersViewModel = {

    val journeyConfirmationLines: Seq[CheckYourAnswersConfirmationLine] = {

      val mandatoryLines = Seq(
        CheckYourAnswersConfirmationLine(
          Messages("tai.addPensionProvider.cya.q1"),
          pensionProviderName,
          controllers.pensions.routes.AddPensionProviderController.addPensionProviderName().url),
        CheckYourAnswersConfirmationLine(
          Messages("tai.addPensionProvider.cya.q2"),
          Dates.formatDate(new LocalDate(pensionStartDate)),
          controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate().url
        ),
        CheckYourAnswersConfirmationLine(
          Messages("tai.addPensionProvider.cya.q3"),
          pensionRefNo,
          controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url),
        CheckYourAnswersConfirmationLine(
          Messages("tai.addPensionProvider.cya.q4"),
          contactableByPhone,
          controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url)
      )
      val optionalPhoneNoLine = phoneNumber map {
        CheckYourAnswersConfirmationLine(
          Messages("tai.phoneNumber"),
          _,
          controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url)
      }
      if (optionalPhoneNoLine.isDefined) mandatoryLines :+ optionalPhoneNoLine.get else mandatoryLines

    }

    val postConfirmationText = Messages("tai.checkYourAnswers.confirmText")

    CheckYourAnswersViewModel(
      Messages("add.missing.pension"),
      controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url,
      Messages("tai.addPensionProvider.cya.title"),
      journeyConfirmationLines,
      postConfirmationText,
      controllers.pensions.routes.AddPensionProviderController.submitYourAnswers().url,
      controllers.pensions.routes.AddPensionProviderController.cancel().url
    )
  }
}
