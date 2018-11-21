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

package uk.gov.hmrc.tai.model.cache

import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

final case class UpdateNextYearsIncomeCacheModel(employmentName: String, employmentId: Int, isPension: Boolean, currentValue: Int, newValue: Option[Int] = None) {
  def toCacheMap: Map[String, String] = {
    Map(
      UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME -> employmentName,
      UpdateNextYearsIncomeConstants.EMPLOYMENT_ID -> employmentId.toString,
      UpdateNextYearsIncomeConstants.IS_PENSION -> isPension.toString,
      UpdateNextYearsIncomeConstants.CURRENT_AMOUNT -> currentValue.toString
    ) ++ { newValue match {
      case Some(_) => Map(UpdateNextYearsIncomeConstants.NEW_AMOUNT -> newValue.get.toString)
      case None => Map()
    }}
  }
}
