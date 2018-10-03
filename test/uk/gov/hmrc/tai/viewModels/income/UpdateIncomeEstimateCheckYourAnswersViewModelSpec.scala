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

package uk.gov.hmrc.tai.viewModels.income

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}

class UpdateIncomeEstimateCheckYourAnswersViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport{

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Update income estimate check your answers view model" must {
    "return all monthly journey lines" when{
      "taxable pay and bonus or overtime pay is provided" in{
        viewModel.journeyConfirmationLines.size mustBe 7

      }
    }
  }

  val paymentFrequency = "Monthly"
  val totalMonthlyPay = "10000"
  val hasDeductions ="Yes"
  val taxablePay = Some("1800")
  val hasBonusOrOvertime = "Yes"
  val hasExtraBonusOrOvertime = Some("Yes")
  val totalBonusOrOvertime = Some("3000")

  val viewModel = UpdateIncomeEstimateCheckYourAnswersViewModel(
    paymentFrequency,
    totalMonthlyPay,
    hasDeductions,
    taxablePay,
    hasBonusOrOvertime,
    hasExtraBonusOrOvertime,
    totalBonusOrOvertime
  )

}
