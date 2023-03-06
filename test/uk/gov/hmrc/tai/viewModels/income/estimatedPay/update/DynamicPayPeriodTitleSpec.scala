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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import uk.gov.hmrc.tai.util.constants.PayPeriodConstants._
import utils.BaseSpec

class DynamicPayPeriodTitleSpec extends BaseSpec {

  "DynamicPayPeriodTitle" must {

    "show gross pay messages" when {

      "gross pay period is monthly" in {
        GrossPayPeriodTitle.title(Some(Monthly), None) mustBe messagesApi("tai.payslip.title.month")
      }

      "gross pay period is weekly" in {
        GrossPayPeriodTitle.title(Some(Weekly), None) mustBe messagesApi("tai.payslip.title.week")
      }

      "gross pay period is fortnightly" in {
        GrossPayPeriodTitle.title(Some(Fortnightly), None) mustBe messagesApi("tai.payslip.title.2week")
      }

      "gross pay period is four weekly" in {
        GrossPayPeriodTitle.title(Some(FourWeekly), None) mustBe messagesApi("tai.payslip.title.4week")
      }

      "gross pay period is a number of days" in {
        val numberOfDays = "123"
        GrossPayPeriodTitle
          .title(Some(Other), Some(numberOfDays)) mustBe messagesApi("tai.payslip.title.days", numberOfDays)
      }

    }

    "Display taxable pay messages" when {

      "taxable pay period is monthly" in {
        TaxablePayPeriod.errorMessage(Some(Monthly), None) mustBe messagesApi("tai.taxablePayslip.title.month")
      }

      "taxable pay period is weekly" in {
        TaxablePayPeriod.errorMessage(Some(Weekly), None) mustBe messagesApi("tai.taxablePayslip.title.week")
      }

      "taxable pay period is fortnightly" in {
        TaxablePayPeriod.errorMessage(Some(Fortnightly), None) mustBe messagesApi("tai.taxablePayslip.title.2week")
      }

      "taxable pay period is four weekly" in {
        TaxablePayPeriod.errorMessage(Some(FourWeekly), None) mustBe messagesApi("tai.taxablePayslip.title.4week")
      }

      "taxable pay period is a number of days" in {
        val numberOfDays = "123"
        TaxablePayPeriod.errorMessage(Some(Other), Some(numberOfDays)) mustBe messagesApi(
          "tai.taxablePayslip.title.days",
          numberOfDays)
      }
    }
  }
}
