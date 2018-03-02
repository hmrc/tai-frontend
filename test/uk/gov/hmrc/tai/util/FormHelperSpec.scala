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

package uk.gov.hmrc.tai.util

import uk.gov.hmrc.play.test.UnitSpec

class FormHelperSpec extends UnitSpec {

  "Strip Number" should {

    "return none if string is none" in {
      FormHelper.stripNumber(None).isDefined shouldBe false
    }

    "return original string if only has numbers" in {
      FormHelper.stripNumber(Some("90")) shouldBe Some("90")
    }

    "return striped decimal value" in {
      FormHelper.stripNumber(Some("9999.99")) shouldBe Some("9999")
    }
    "return striped number without pound symbol" in {
      FormHelper.stripNumber(Some("£90")) shouldBe Some("90")
    }

    "return striped number without commas" in {
      FormHelper.stripNumber(Some("999,999")) shouldBe Some("999999")
    }
  }



  "New Strip Number" should {

    "return original string if only has numbers" in {
      FormHelper.stripNumber("90") shouldBe "90"
    }

    "return striped decimal value" in {
      FormHelper.stripNumber("9999.99") shouldBe "9999"
    }
    "return striped number without pound symbol" in {
      FormHelper.stripNumber("£90") shouldBe "90"
    }

    "return striped number without commas" in {
      FormHelper.stripNumber("999,999") shouldBe "999999"
    }
  }

  "isCurrency " should {
    "not allow numbers with multiple pound symbols" in {
      FormHelper.isCurrency("9£9,99£9", isWholeNumRequired = false) shouldBe false
    }

    " allow if pound symbol is at the start of the form" in {
      FormHelper.isCurrency("£99,999", isWholeNumRequired = false) shouldBe true
      FormHelper.isCurrency("£99,999.00", isWholeNumRequired = false) shouldBe true
      FormHelper.isCurrency("£99,999,999.00", isWholeNumRequired = false) shouldBe true
    }

    " allowed plain positive number with only 2 decimal places when whole Number required is false " in {
      FormHelper.isCurrency("110000", isWholeNumRequired = false) shouldBe true
      FormHelper.isCurrency("110000.00", isWholeNumRequired = false) shouldBe true
      FormHelper.isCurrency("110000.99", isWholeNumRequired = false) shouldBe true

      FormHelper.isCurrency("110000.9", isWholeNumRequired = false) shouldBe false
      FormHelper.isCurrency("110000.999", isWholeNumRequired = false) shouldBe false
      FormHelper.isCurrency("-110000", isWholeNumRequired = false) shouldBe false
      FormHelper.isCurrency("-110000.00", isWholeNumRequired = false) shouldBe false
    }

    " allow only positive whole Number when wholeNumberRequired is true " in {
      FormHelper.isCurrency("110000", isWholeNumRequired = true) shouldBe true
      FormHelper.isCurrency("110000.9", isWholeNumRequired = true) shouldBe false
      FormHelper.isCurrency("110000.99", isWholeNumRequired = true) shouldBe false
      FormHelper.isCurrency("110000.999", isWholeNumRequired = true) shouldBe false
      FormHelper.isCurrency("-110000", isWholeNumRequired = false) shouldBe false
      FormHelper.isCurrency("-110000.00", isWholeNumRequired = false) shouldBe false
    }

    "fail if value entered is not a number" in {
      FormHelper.isCurrency("99.paul", isWholeNumRequired = true) shouldBe false
      FormHelper.isCurrency("9.!!", isWholeNumRequired = false) shouldBe false
    }
  }

  "isValidCurrency " should {

    "not allow number with when pound symbol is not at the start" in {
      FormHelper.isValidCurrency(Some("9£9,999")) shouldBe false
    }

    "allow whole Numbers " in {
      FormHelper.isValidCurrency(Some("99999"), true) shouldBe true
    }

    "allow fractions when whole number flag is not passed " in {
      FormHelper.isValidCurrency(Some("99999.90")) shouldBe true
    }

    "allow None as a currency" in {
      FormHelper.isValidCurrency(None) shouldBe true
    }
  }

}