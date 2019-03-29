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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.util.constants.EditIncomePayPeriodConstants

class DynamicPayPeriodTitleSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with EditIncomePayPeriodConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "DynamicPayPeriodTitle" must {

    "show gross pay messages" when {

      "gross pay period is monthly" in {
        GrossPayPeriodTitle.title(Some(MONTHLY), None) mustBe messagesApi("tai.payslip.title.month")
      }

      "gross pay period is weekly" in {
        GrossPayPeriodTitle.title(Some(WEEKLY), None) mustBe messagesApi("tai.payslip.title.week")
      }

      "gross pay period is fortnightly" in {
        GrossPayPeriodTitle.title(Some(FORTNIGHTLY), None) mustBe messagesApi("tai.payslip.title.2week")
      }

      "gross pay period is a number of days" in {
        val numberOfDays = "123"
        GrossPayPeriodTitle.title(Some(OTHER), Some(numberOfDays)) mustBe messagesApi("tai.payslip.title.days", numberOfDays)
      }

    }

    "Display taxable pay messages" when {

      "taxable pay period is monthly" in {
        TaxablePayPeriod.errorMessage(Some(MONTHLY), None) mustBe messagesApi("tai.taxablePayslip.title.month")
      }

      "taxable pay period is weekly" in{
        TaxablePayPeriod.errorMessage(Some(WEEKLY), None) mustBe messagesApi("tai.taxablePayslip.title.week")
      }

      "taxable pay period is fortnightly" in{
        TaxablePayPeriod.errorMessage(Some(FORTNIGHTLY), None) mustBe messagesApi("tai.taxablePayslip.title.2week")
      }

      "taxable pay period is a number of days" in {
        val numberOfDays = "123"
        TaxablePayPeriod.errorMessage(Some(OTHER), Some(numberOfDays)) mustBe messagesApi("tai.taxablePayslip.title.days", numberOfDays)
      }
    }
  }
}
