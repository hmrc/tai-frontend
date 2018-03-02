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

package uk.gov.hmrc.tai.viewModels

import org.scalatestplus.play.PlaySpec

class PotentialUnderpaymentViewModelSpec extends PlaySpec {

  val sutBase = PotentialUnderpaymentViewModel(
    iyaCYAmount = 8.0,
    iyaCYPlusOneAmount = 12.0,
    iyaTotalAmount = 20.0,
    iyaTaxCodeChangeAmount = 10.0
  )

  "The gaDimensions method" should {

    "return a Map for setting of in year calc 'current year' google analytic dimensions" when {

      "the displayCYOnly bool is set" in {
        val sut = sutBase.copy(displayCYOnly = true)
        val gaMap = sut.gaDimensions
        gaMap mustBe Some(Map("valueOfIycdcPayment" -> "8.0", "iycdcReconciliationStatus" -> "Current Year"))
      }
    }

    "return a Map for setting of in year calc 'next year' google analytic dimensions" when {

      "the displayCYPlusOneOnly bool is set" in {
        val sut = sutBase.copy(displayCYPlusOneOnly = true)
        val gaMap = sut.gaDimensions
        gaMap mustBe Some(Map("valueOfIycdcPayment" -> "12.0", "iycdcReconciliationStatus" -> "Next Year"))
      }
    }

    "return a Map for setting of in year calc 'current and next year' google analytic dimensions" when {

      "the displayCYPlusOneOnly bool is set" in {
        val sut = sutBase.copy(displayCYAndCYPlusOneOnly = true)
        val gaMap = sut.gaDimensions
        gaMap mustBe Some(Map("valueOfIycdcPayment" -> "8.0", "iycdcReconciliationStatus" -> "Current and Next Year"))
      }
    }

    "Return None, when none of the three bools are set" in {
      val gaMap = sutBase.gaDimensions
      gaMap mustBe None
    }
  }

}
