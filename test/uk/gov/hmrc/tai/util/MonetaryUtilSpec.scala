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

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.TaiConstants.encodedMinusSign

class MonetaryUtilSpec extends PlaySpec {

  "withPoundPrefixAndSign" must {
    "return the string representation of the provided MoneyPounds with a pound symbol prefix" when {
      "the value is zero" in {
        MonetaryUtil.withPoundPrefixAndSign(MoneyPounds(0)) mustBe "£0.00"
      }
    }

    "return the string representation of the provided MoneyPounds with a pound symbol prefix" when {
      "the value is positive" in {
        MonetaryUtil.withPoundPrefixAndSign(MoneyPounds(1000)) mustBe "£1,000.00"
      }
    }
    "return the string representation of the provided MoneyPounds with a pound symbol prefix and negative sign" when {
      "the value is negative" in {
        MonetaryUtil.withPoundPrefixAndSign(MoneyPounds(-1000)) mustBe s"${encodedMinusSign}£1,000.00"
      }
    }
  }

  "withPoundPrefix" must {
    "return the string representation of the provided MoneyPounds with a pound symbol prefix" when {
      "the value is zero" in {
        MonetaryUtil.withPoundPrefix(MoneyPounds(0)) mustBe "£0.00"
      }
      "the value is positive" in {
        MonetaryUtil.withPoundPrefix(MoneyPounds(1000)) mustBe "£1,000.00"
      }
      "the value is negative" in {
        MonetaryUtil.withPoundPrefix(MoneyPounds(-1000)) mustBe "£1,000.00"
      }
    }
  }
}