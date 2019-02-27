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

package uk.gov.hmrc.tai.util

import org.scalatestplus.play.PlaySpec

class FormHelperSpec extends PlaySpec {

  "Strip Number" must {

    "return none if string is none" in {
      FormHelper.stripNumber(None).isDefined mustBe false
    }

    "return original string if only has numbers" in {
      FormHelper.stripNumber(Some("90")) mustBe Some("90")
    }

    "return striped decimal value" in {
      FormHelper.stripNumber(Some("9999.99")) mustBe Some("9999")
    }
    "return striped number without pound symbol" in {
      FormHelper.stripNumber(Some("£90")) mustBe Some("90")
    }

    "return striped number without commas" in {
      FormHelper.stripNumber(Some("999,999")) mustBe Some("999999")
    }
  }

  "New Strip Number" must {

    "return original string if only has numbers" in {
      FormHelper.stripNumber("90") mustBe "90"
    }

    "return striped decimal value" in {
      FormHelper.stripNumber("9999.99") mustBe "9999"
    }
    "return striped number without pound symbol" in {
      FormHelper.stripNumber("£90") mustBe "90"
    }

    "return striped number without commas" in {
      FormHelper.stripNumber("999,999") mustBe "999999"
    }
  }

  "isCurrency " must {
    "not allow numbers with multiple pound symbols" in {
      FormHelper.isCurrency("9£9,99£9", isWholeNumRequired = false) mustBe false
    }

    " allow if pound symbol is at the start of the form" in {
      FormHelper.isCurrency("£99,999", isWholeNumRequired = false) mustBe true
      FormHelper.isCurrency("£99,999.00", isWholeNumRequired = false) mustBe true
      FormHelper.isCurrency("£99,999,999.00", isWholeNumRequired = false) mustBe true
    }

    " allowed plain positive number with only 2 decimal places when whole Number required is false " in {
      FormHelper.isCurrency("110000", isWholeNumRequired = false) mustBe true
      FormHelper.isCurrency("110000.00", isWholeNumRequired = false) mustBe true
      FormHelper.isCurrency("110000.99", isWholeNumRequired = false) mustBe true

      FormHelper.isCurrency("110000.9", isWholeNumRequired = false) mustBe false
      FormHelper.isCurrency("110000.999", isWholeNumRequired = false) mustBe false
      FormHelper.isCurrency("-110000", isWholeNumRequired = false) mustBe false
      FormHelper.isCurrency("-110000.00", isWholeNumRequired = false) mustBe false
    }

    " allow only positive whole Number when wholeNumberRequired is true " in {
      FormHelper.isCurrency("110000", isWholeNumRequired = true) mustBe true
      FormHelper.isCurrency("110000.9", isWholeNumRequired = true) mustBe false
      FormHelper.isCurrency("110000.99", isWholeNumRequired = true) mustBe false
      FormHelper.isCurrency("110000.999", isWholeNumRequired = true) mustBe false
      FormHelper.isCurrency("-110000", isWholeNumRequired = false) mustBe false
      FormHelper.isCurrency("-110000.00", isWholeNumRequired = false) mustBe false
    }

    "fail if value entered is not a number" in {
      FormHelper.isCurrency("99.paul", isWholeNumRequired = true) mustBe false
      FormHelper.isCurrency("9.!!", isWholeNumRequired = false) mustBe false
    }
  }

  "isValidCurrency " must {

    "not allow number with when pound symbol is not at the start" in {
      FormHelper.isValidCurrency(Some("9£9,999")) mustBe false
    }

    "allow whole Numbers " in {
      FormHelper.isValidCurrency(Some("99999"), true) mustBe true
    }

    "allow fractions when whole number flag is not passed " in {
      FormHelper.isValidCurrency(Some("99999.90")) mustBe true
    }

    "allow None as a currency" in {
      FormHelper.isValidCurrency(None) mustBe true
    }
  }

  "areEqual" must {
    "be the same, ignoring commmas and dots" in {
      FormHelper.areEqual(Some("123"), Some("123")) mustBe true
      FormHelper.areEqual(Some("1,23"), Some("1,23")) mustBe true
    }

    "not be the same" in {
      FormHelper.areEqual(Some("123"), Some("124")) mustBe false
      FormHelper.areEqual(Some("1,23"), Some("1.24")) mustBe false
    }
  }

}