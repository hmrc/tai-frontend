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

package uk.gov.hmrc.tai.viewModels.employments

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}

case class PayrollNumberViewModel(employmentName: String, firstPayChoice: Boolean)

object PayrollNumberViewModel extends JourneyCacheConstants with FormValuesConstants {

  def apply(cache: Map[String, String])(implicit messages: Messages): PayrollNumberViewModel = {
    val employerName = cache.getOrElse(AddEmployment_NameKey, "")
    val firstPayChoice = cache.get(AddEmployment_StartDateWithinSixWeeks) match {
      case Some(YesValue) => true
      case _              => false
    }
    PayrollNumberViewModel(employerName, firstPayChoice)
  }
}
