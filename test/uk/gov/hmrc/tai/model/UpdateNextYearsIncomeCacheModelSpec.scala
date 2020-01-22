/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.cache.UpdateNextYearsIncomeCacheModel
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

class UpdateNextYearsIncomeCacheModelSpec extends PlaySpec {

  val employmentName = "EmploymentName"
  val employmentId = 1
  val currentValue = 1000
  val isPension = false

  "hasEstimatedIncomeChanged" must {
    "return true" when {
      "current value is different from new value" in {
        val newValue = 2000
        val model =
          UpdateNextYearsIncomeCacheModel(employmentName, employmentId, isPension, currentValue, Some(newValue))

        model.hasEstimatedIncomeChanged mustBe Some(true)
      }

      "new value is not defined" in {
        val model = UpdateNextYearsIncomeCacheModel(employmentName, employmentId, isPension, currentValue)

        model.hasEstimatedIncomeChanged mustBe None
      }
    }
    "return false" when {
      "current value and new value are equal" in {
        val newValue = 1000
        val model =
          UpdateNextYearsIncomeCacheModel(employmentName, employmentId, isPension, currentValue, Some(newValue))

        model.hasEstimatedIncomeChanged mustBe Some(false)
      }
    }
  }

  "toCacheMap" must {
    "return a Map[String, String] without a new amount" when {
      "the new amount is None" in {
        val model = UpdateNextYearsIncomeCacheModel(employmentName, employmentId, isPension, currentValue)

        val expected = Map[String, String](
          UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME -> employmentName,
          UpdateNextYearsIncomeConstants.EMPLOYMENT_ID   -> employmentId.toString,
          UpdateNextYearsIncomeConstants.IS_PENSION      -> isPension.toString,
          UpdateNextYearsIncomeConstants.CURRENT_AMOUNT  -> currentValue.toString
        )

        model.toCacheMap mustBe expected
      }
    }

    "return a Map[String, String] with a new amount" when {
      "the new amount is defined" in {
        val newValue = 2000

        val model =
          UpdateNextYearsIncomeCacheModel(employmentName, employmentId, isPension, currentValue, Some(newValue))

        val expected = Map[String, String](
          UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME -> employmentName,
          UpdateNextYearsIncomeConstants.EMPLOYMENT_ID   -> employmentId.toString,
          UpdateNextYearsIncomeConstants.IS_PENSION      -> isPension.toString,
          UpdateNextYearsIncomeConstants.CURRENT_AMOUNT  -> currentValue.toString,
          UpdateNextYearsIncomeConstants.NEW_AMOUNT      -> newValue.toString
        )

        model.toCacheMap mustBe expected
      }
    }
  }
}
