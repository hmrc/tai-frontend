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

package uk.gov.hmrc.tai.viewModels.income

import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.joda.time.LocalDate
import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates

case class IncomeCheckYourAnswersViewModel(
  preHeading: String,
  backLinkUrl: String,
  journeyConfirmationLines: Seq[CheckYourAnswersConfirmationLine],
  postConfirmationText: Option[String],
  submissionUrl: String,
  cancelUrl: String)

object IncomeCheckYourAnswersViewModel {

  def apply(
    preHeading: String,
    incomeSourceName: String,
    incomeSourceStart: String,
    incomeSourceRefNo: String,
    contactableByPhone: String,
    phoneNumber: Option[String],
    backLinkUrl: String,
    submissionUrl: String,
    cancelUrl: String)(implicit messages: Messages): IncomeCheckYourAnswersViewModel = {

    val journeyConfirmationLines: Seq[CheckYourAnswersConfirmationLine] = {

      val mandatoryLines = Seq(
        CheckYourAnswersConfirmationLine(
          Messages("tai.addEmployment.cya.q1"),
          incomeSourceName,
          controllers.employments.routes.AddEmploymentController.addEmploymentName.url),
        CheckYourAnswersConfirmationLine(
          Messages("tai.addEmployment.cya.q2"),
          Dates.formatDate(new LocalDate(incomeSourceStart)),
          controllers.employments.routes.AddEmploymentController.addEmploymentStartDate.url
        ),
        CheckYourAnswersConfirmationLine(
          Messages("tai.addEmployment.cya.q3"),
          incomeSourceRefNo,
          controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber.url),
        CheckYourAnswersConfirmationLine(
          Messages("tai.addEmployment.cya.q4"),
          contactableByPhone,
          controllers.employments.routes.AddEmploymentController.addTelephoneNumber.url)
      )

      val optionalPhoneNoLine = phoneNumber map { phoneNo =>
        Seq(
          CheckYourAnswersConfirmationLine(
            Messages("tai.phoneNumber"),
            phoneNo,
            controllers.employments.routes.AddEmploymentController.addTelephoneNumber.url))
      }

      if (optionalPhoneNoLine.isDefined) mandatoryLines ++ optionalPhoneNoLine.get else mandatoryLines
    }

    val postConfirmationText = Messages("tai.checkYourAnswers.confirmText")

    IncomeCheckYourAnswersViewModel(
      preHeading,
      backLinkUrl,
      journeyConfirmationLines,
      Some(postConfirmationText),
      submissionUrl,
      cancelUrl)
  }

  def apply(
    employmentId: Int,
    preHeading: String,
    incomeSourceEnd: String,
    contactableByPhone: String,
    phoneNumber: Option[String],
    backLinkUrl: String,
    submissionUrl: String,
    cancelUrl: String)(implicit messages: Messages): IncomeCheckYourAnswersViewModel = {

    val journeyConfirmationLines: Seq[CheckYourAnswersConfirmationLine] = {

      val mandatoryLines = Seq(
        CheckYourAnswersConfirmationLine(
          Messages("tai.checkYourAnswers.dateEmploymentEnded"),
          Dates.formatDate(new LocalDate(incomeSourceEnd)),
          controllers.employments.routes.EndEmploymentController.endEmploymentPage().url
        ),
        CheckYourAnswersConfirmationLine(
          Messages("tai.checkYourAnswers.contactByPhone"),
          contactableByPhone,
          controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url)
      )

      val optionalPhoneNoLine = phoneNumber map { phoneNo =>
        Seq(
          CheckYourAnswersConfirmationLine(
            Messages("tai.phoneNumber"),
            phoneNo,
            controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url))
      }

      if (optionalPhoneNoLine.isDefined) mandatoryLines ++ optionalPhoneNoLine.get else mandatoryLines
    }

    val postConfirmationText = Messages("tai.checkYourAnswers.confirmText")

    IncomeCheckYourAnswersViewModel(
      preHeading,
      backLinkUrl,
      journeyConfirmationLines,
      Some(postConfirmationText),
      submissionUrl,
      cancelUrl)
  }
}
