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

package uk.gov.hmrc.tai.viewModels.benefit

import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.play.views.formatting.Money
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

case class RemoveCompanyBenefitCheckYourAnswersViewModel(
  tableHeader: String,
  stopDate: String,
  valueOfBenefit: Option[String],
  contactByPhone: String,
  phoneNumber: Option[String])
    extends FormValuesConstants {

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {

    val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.whatYouToldUs"),
      Messages("tai.noLongerGetBenefit"),
      controllers.benefits.routes.CompanyBenefitController.decision().url
    )

    val stopDateLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.dateBenefitEnded"),
      stopDate,
      controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url
    )

    val valueOfBenefitLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.valueOfBenefit"),
      Money.pounds(BigDecimal(valueOfBenefit.getOrElse("0"))).toString().trim.replace("&pound;", "\u00A3"),
      controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url
    )

    val contactByPhoneLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.contactByPhone"),
      if (contactByPhone == YesValue)
        Messages("tai.checkYourAnswers.contactByPhone.Yes")
      else
        Messages("tai.checkYourAnswers.contactByPhone.No"),
      controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url
    )

    val phoneNumberLine = CheckYourAnswersConfirmationLine(
      Messages("tai.phoneNumber"),
      phoneNumber.getOrElse(""),
      controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url
    )

    if (contactByPhone == YesValue) {
      if (valueOfBenefit.isEmpty) {
        Seq(whatYouToldUsLine, stopDateLine, contactByPhoneLine, phoneNumberLine)
      } else {
        Seq(whatYouToldUsLine, stopDateLine, valueOfBenefitLine, contactByPhoneLine, phoneNumberLine)
      }
    } else {
      if (valueOfBenefit.isEmpty) {
        Seq(whatYouToldUsLine, stopDateLine, contactByPhoneLine)
      } else {
        Seq(whatYouToldUsLine, stopDateLine, valueOfBenefitLine, contactByPhoneLine)
      }
    }

  }

}

object RemoveCompanyBenefitCheckYourAnswersViewModel extends ViewModelHelper {

  def apply(
    benefitType: String,
    employerName: String,
    stopDate: String,
    valueOfBenefit: Option[String],
    contactByPhone: String,
    phoneNumber: Option[String])(implicit messages: Messages): RemoveCompanyBenefitCheckYourAnswersViewModel = {
    val tableHeader = Messages("tai.benefits.ended.tableHeader", employerName, benefitType)
    new RemoveCompanyBenefitCheckYourAnswersViewModel(
      tableHeader,
      stopDate,
      valueOfBenefit,
      contactByPhone,
      phoneNumber)
  }

}
