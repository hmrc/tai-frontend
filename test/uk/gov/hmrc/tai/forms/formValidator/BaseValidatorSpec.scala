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

package uk.gov.hmrc.tai.forms.formValidator

import org.scalatestplus.play.PlaySpec
import play.api.data.validation.{Invalid, Valid, ValidationError}

import scala.collection.mutable

class BaseValidatorSpec extends PlaySpec {

  val taxablePayYTD = BigDecimal("10000")
  val validateTaxablePayYTDError = "testErrorMessage"
  val invalidResult = Invalid(List(ValidationError(List(validateTaxablePayYTDError), "validateInputAmount")))

  "validateInputAmount" should {
    "return Valid " when {
      "the applied value is equal to taxablePayYTD" in {
        val result =
          BaseValidator.validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError)
        result.apply(Some("10000")) mustBe Valid
      }
      "the applied value is greater than taxablePayYTD" in {
        val result =
          BaseValidator.validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError)
        result.apply(Some("10001")) mustBe Valid
      }
      "the applied value is greater than taxablePayYTD and has commas" in {
        val result =
          BaseValidator.validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError)
        result.apply(Some("10,001")) mustBe Valid
      }
    }
    "return Invalid " when {
      "the applied value is less than taxablePayYTD" in {
        val result =
          BaseValidator.validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError)
        result.apply(Some("9999")) mustBe invalidResult
      }

      "the applied value is less than taxablePayYTD and has commas" in {
        val result =
          BaseValidator.validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError)
        result.apply(Some("9,990")) mustBe invalidResult
      }

      "the applied value is empty" in {
        val result = BaseValidator
          .validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError = "testErrorMessage")
        result.apply(Some("")) mustBe invalidResult
      }

      "the applied value is not number" in {
        val result = BaseValidator
          .validateInputAmountComparisonWithTaxablePay(taxablePayYTD, validateTaxablePayYTDError = "testErrorMessage")
        result.apply(Some("aaa")) mustBe invalidResult
      }
    }
  }
}
