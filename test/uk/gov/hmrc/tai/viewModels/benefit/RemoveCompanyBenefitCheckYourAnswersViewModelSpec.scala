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

package uk.gov.hmrc.tai.viewModels.benefit

import play.api.i18n.Messages
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import utils.BaseSpec

class RemoveCompanyBenefitCheckYourAnswersViewModelSpec extends BaseSpec {

  "Remove company benefit check your answers view model" must {

    "return all journey lines" when {
      "both 'phone number' and 'benefit value' are present" in {
        baseModel.journeyConfirmationLines.size mustBe 5
        baseModel.journeyConfirmationLines mustBe Seq(
          whatYouToldUsLine,
          stopDateLine,
          valueOfBenefitLine,
          contactByPhoneLine,
          phoneNumberLine)
      }
    }

    "return relevant journey lines" when {

      "neither 'phone number' nor 'benefit value' are present" in {
        val model = baseModel.copy(valueOfBenefit = None, contactByPhone = "No", phoneNumber = None)
        model.journeyConfirmationLines.size mustBe 3
        model.journeyConfirmationLines mustBe Seq(
          whatYouToldUsLine,
          stopDateLine,
          contactByPhoneLine.copy(answer = "No"))
      }

      "only 'phone number' is not present" in {
        val model = baseModel.copy(contactByPhone = "No", phoneNumber = None)
        model.journeyConfirmationLines.size mustBe 4
        model.journeyConfirmationLines mustBe Seq(
          whatYouToldUsLine,
          stopDateLine,
          valueOfBenefitLine,
          contactByPhoneLine.copy(answer = "No"))
      }

      "only 'benefit value' is not present" in {
        val model = baseModel.copy(valueOfBenefit = None)
        model.journeyConfirmationLines.size mustBe 4
        model.journeyConfirmationLines mustBe Seq(whatYouToldUsLine, stopDateLine, contactByPhoneLine, phoneNumberLine)
      }
    }

    "return a view model with correct table header" in {
      baseModel.tableHeader mustBe Messages("tai.benefits.ended.tableHeader", "TestCompany", "Awesome Benefit")
    }
  }

  private lazy val whatYouToldUsLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.whatYouToldUs"),
    Messages("tai.noLongerGetBenefit"),
    controllers.benefits.routes.CompanyBenefitController.decision().url
  )

  private lazy val stopDateLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.dateBenefitEnded"),
    "Before 6 April",
    controllers.benefits.routes.RemoveCompanyBenefitController.stopDate().url
  )

  private lazy val valueOfBenefitLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.valueOfBenefit"),
    "£1,000,000",
    controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url
  )

  private lazy val contactByPhoneLine = CheckYourAnswersConfirmationLine(
    Messages("tai.checkYourAnswers.contactByPhone"),
    "Yes",
    controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url
  )

  private lazy val phoneNumberLine = CheckYourAnswersConfirmationLine(
    Messages("tai.phoneNumber"),
    "0123456789",
    controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url
  )

  def baseModel = RemoveCompanyBenefitCheckYourAnswersViewModel(
    "Awesome Benefit",
    "TestCompany",
    "Before 6 April",
    Some("1000000"),
    "Yes",
    Some("0123456789")
  )

}
