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

package uk.gov.hmrc.tai.viewModels.employments

import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.journeyCache.AddEmploymentConstants

case class PayrollNumberViewModel(employmentName: String, firstPayChoice: Boolean, backUrl: String)

object PayrollNumberViewModel {

  def apply(cache: Map[String, String]): PayrollNumberViewModel = {
    val employerName = cache.getOrElse(AddEmploymentConstants.NameKey, "")
    val firstPayChoice = cache.get(AddEmploymentConstants.StartDateWithinSixWeeks) match {
      case Some(FormValuesConstants.YesValue) => true
      case _                                  => false
    }
    val backUrl = cache.get(AddEmploymentConstants.ReceivedFirstPayKey) match {
      case None => controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url
      case _    => controllers.employments.routes.AddEmploymentController.receivedFirstPay().url
    }
    PayrollNumberViewModel(employerName, firstPayChoice, backUrl)
  }
}
