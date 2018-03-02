/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.TaxPeriodLabelService
import uk.gov.hmrc.tai.util.{DateFormatConstants, FormValuesConstants, ViewModelHelper}
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine



case class UpdateIncomeDetailsCheckYourAnswersViewModel(tableHeader: String,
                                                        whatYouToldUs: String,
                                                        contactByPhone: String,
                                                        phoneNumber: Option[String]) extends FormValuesConstants{

  def journeyConfirmationLines: Seq[CheckYourAnswersConfirmationLine] = {

    val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.whatYouToldUs"),
      whatYouToldUs,
      controllers.income.previousYears.routes.UpdateIncomeDetailsController.details().url
    )

    val contactByPhoneLine = CheckYourAnswersConfirmationLine(
      Messages("tai.checkYourAnswers.contactByPhone"),
      contactByPhone,
      controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber().url
    )

    val phoneNumberLine = CheckYourAnswersConfirmationLine(
      Messages("tai.phoneNumber"),
      phoneNumber.getOrElse(""),
      controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber().url
    )

    if (contactByPhone == YesValue) {
      Seq(whatYouToldUsLine, contactByPhoneLine, phoneNumberLine)
    } else {
      Seq(whatYouToldUsLine, contactByPhoneLine)
    }

  }

}


object UpdateIncomeDetailsCheckYourAnswersViewModel extends ViewModelHelper with DateFormatConstants {

  def apply(taxYear: TaxYear,
            whatYouToldUs: String,
            contactByPhone: String,
            phoneNumber: Option[String]): UpdateIncomeDetailsCheckYourAnswersViewModel = {
    val tablelHeader = Messages("tai.income.previousYears.decision.header",TaxPeriodLabelService.taxPeriodLabel(taxYear.year))
    new UpdateIncomeDetailsCheckYourAnswersViewModel(tablelHeader, whatYouToldUs, contactByPhone, phoneNumber)
  }

}
