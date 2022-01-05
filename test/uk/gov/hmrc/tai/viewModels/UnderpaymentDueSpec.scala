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

package uk.gov.hmrc.tai.viewModels

import controllers.routes
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{GiftAidAdjustment, UnderPaymentFromPreviousYear}
import utils.BaseSpec

class UnderpaymentDueSpec extends BaseSpec {

  "UnderpaymentDueSpec" when {

    "UnderPaymentFromPreviousYear is present should contain the correct values from the CodingComponent" in {

      val codingComponents =
        Seq(CodingComponent(UnderPaymentFromPreviousYear, Some(1), 500.00, "UnderPaymentFromPreviousYear", Some(123)))

      val result = UnderpaymentDue(codingComponents)

      result.allowanceReducedBy mustEqual 500.00
      result.sourceAmount mustEqual 123.00
    }

    "UnderPaymentFromPreviousYear is present but has a missing input amount" in {
      val inputAmount: Option[BigDecimal] = None

      val codingComponents =
        Seq(CodingComponent(UnderPaymentFromPreviousYear, Some(1), 500.00, "UnderPaymentFromPreviousYear", inputAmount))

      val result = UnderpaymentDue(codingComponents)

      result.allowanceReducedBy mustEqual 0
      result.sourceAmount mustEqual 0
    }

    "UnderPaymentFromPreviousYear is not present" in {
      val inputAmount: Option[BigDecimal] = None

      val codingComponents =
        Seq(CodingComponent(GiftAidAdjustment, Some(1), 500.00, "GiftAidAdjustment", inputAmount))

      val result = UnderpaymentDue(codingComponents)

      result.allowanceReducedBy mustEqual 0
      result.sourceAmount mustEqual 0
    }
  }
}
