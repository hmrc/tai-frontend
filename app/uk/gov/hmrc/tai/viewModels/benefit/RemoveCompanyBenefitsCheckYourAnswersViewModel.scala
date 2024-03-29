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

package uk.gov.hmrc.tai.viewModels.benefit

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.{Money, ViewModelHelper}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class RemoveCompanyBenefitsCheckYourAnswersViewModel(
  tableHeader: String,
  stopDate: LocalDate,
  valueOfBenefit: Option[String],
  contactByPhone: String,
  phoneNumber: Option[String]
) {

  val beforeTaxYearStart = stopDate isBefore TaxYear().start

  def journeyConfirmationLines(implicit messages: Messages): Seq[CheckYourAnswersConfirmationLine] = {

    val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.whatYouToldUs"),
      Messages("tai.noLongerGetBenefit"),
      controllers.benefits.routes.CompanyBenefitController.decision().url
    )

    val stopDateLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.dateBenefitEnded"),
      stopDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy")), // TODO add welsh?
      controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url
    )

    val valueOfBenefitLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.valueOfBenefit"),
      Money.pounds(BigDecimal(valueOfBenefit.getOrElse("0"))).toString().trim.replace("&pound;", "\u00A3"),
      controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url
    )

    val contactByPhoneLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.contactByPhone"),
      if (contactByPhone == FormValuesConstants.YesValue)
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

    if (contactByPhone == FormValuesConstants.YesValue) {
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

object RemoveCompanyBenefitsCheckYourAnswersViewModel extends ViewModelHelper {

  def apply(
    benefitType: String,
    employerName: String,
    stopDate: LocalDate,
    valueOfBenefit: Option[String],
    contactByPhone: String,
    phoneNumber: Option[String]
  )(implicit messages: Messages): RemoveCompanyBenefitsCheckYourAnswersViewModel = {
    val tableHeader = Messages("tai.benefits.ended.tableHeader", employerName, benefitType)
    new RemoveCompanyBenefitsCheckYourAnswersViewModel(
      tableHeader,
      stopDate,
      valueOfBenefit,
      contactByPhone,
      phoneNumber
    )
  }

}
