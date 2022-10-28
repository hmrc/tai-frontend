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

package uk.gov.hmrc.tai.viewModels.employments

import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}
import utils.BaseSpec

class PayrollNumberViewModelSpec extends BaseSpec with JourneyCacheConstants {

  "Payroll number view model" must {
    "create an instance of view model" when {

      "employment name and firstPayChoice is yes" in {
        val cacheMap =
          Map(AddEmployment_NameKey -> "XJ", AddEmployment_StartDateWithinSixWeeks -> FormValuesConstants.YesValue)
        val result = PayrollNumberViewModel(cacheMap)
        result mustBe PayrollNumberViewModel(
          "XJ",
          true,
          controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url)

      }

      "employment name and firstPayChoice is no" in {
        val cacheMap = Map(AddEmployment_NameKey -> "XJ")
        val result = PayrollNumberViewModel(cacheMap)
        result mustBe PayrollNumberViewModel(
          "XJ",
          false,
          controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url)
      }

      "back url changes if AddEmployment_ReceivedFirstPayKey is present" in {
        val cacheMap = Map(AddEmployment_NameKey -> "XJ", AddEmployment_ReceivedFirstPayKey -> "asdf")
        val result = PayrollNumberViewModel(cacheMap)
        result mustBe PayrollNumberViewModel(
          "XJ",
          false,
          controllers.employments.routes.AddEmploymentController.receivedFirstPay().url)
      }
    }
  }

}
