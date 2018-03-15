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

class ViewModelHelperSpec extends PlaySpec with ViewModelHelper with DateFormatConstants {

  "withPoundPrefixAndSign" must {
    "return the string representation of the provided MoneyPounds with a pound symbol prefix" when {
      "the value is zero" in {
        withPoundPrefixAndSign(MoneyPounds(0)) mustBe "£0.00"
      }
    }

    "return the string representation of the provided MoneyPounds with a pound symbol prefix" when {
      "the value is positive" in {
        withPoundPrefixAndSign(MoneyPounds(1000)) mustBe "£1,000.00"
      }
    }
    "return the string representation of the provided MoneyPounds with a pound symbol prefix and negative sign" when {
      "the value is negative" in {
        withPoundPrefixAndSign(MoneyPounds(-1000)) mustBe s"${minusSign}£1,000.00"
      }
    }
  }

  "withPoundPrefix" must {
    "return the string representation of the provided MoneyPounds with a pound symbol prefix" when {
      "the value is zero" in {
        withPoundPrefix(MoneyPounds(0)) mustBe "£0.00"
      }
      "the value is positive" in {
        withPoundPrefix(MoneyPounds(1000)) mustBe "£1,000.00"
      }
      "the value is negative" in {
        withPoundPrefix(MoneyPounds(-1000)) mustBe "£1,000.00"
      }
    }
  }

  "currentTaxYearHeaderForHtml" must {
    "return the date in passed format" in {
      currentTaxYearHeaderHtmlNonBreak(DateWithoutYearFormat) mustBe "5 April".replace(" ", "\u00A0")
    }
  }

  "nextTaxYearHeaderForHtml" must {
    "return the date in passed format" in {
      nextTaxYearHeaderHtmlNonBreak(DateWithoutYearFormat) mustBe "6 April".replace(" ", "\u00A0")
    }
  }

  "unescapeNonBreakingSpaceOnly" must {
    "substitute a non breaking space unicode character '\u00A0', in place of any text '&nbsp' instances" in {
      val replaced = unescapeNonBreakingSpaceOnly("something&nbsp;with&nbsp;non&nbsp;breaks")
      replaced mustBe "something\u00A0with\u00A0non\u00A0breaks"
    }
  }

  val minusSign = "\u2212"
}
