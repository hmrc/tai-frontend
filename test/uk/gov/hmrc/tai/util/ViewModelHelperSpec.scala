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
import TaiConstants.encodedMinusSign
import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.time.TaxYearResolver
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.language.LanguageUtils.Dates

class ViewModelHelperSpec extends PlaySpec with ViewModelHelper with FakeTaiPlayApplication {

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
        withPoundPrefixAndSign(MoneyPounds(-1000)) mustBe s"${encodedMinusSign}£1,000.00"
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
      currentTaxYearHeaderHtmlNonBreak mustBe TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM y").replace(" ", "\u00A0")
    }
  }

  "nextTaxYearHeaderForHtml" must {
    "return the date in passed format" in {
      nextTaxYearHeaderHtmlNonBreak mustBe TaxYearResolver.startOfNextTaxYear.toString("d MMMM y").replace(" ", "\u00A0")
    }
  }

  "calling isTrue" must {
    "return true, if string is 'true'" in {
      isTrue("true") mustBe true
    }

    "return false, if string is 'false'" in {
      isTrue("false") mustBe false
    }

    "return false, if string is anything else" in {
      isTrue("hello") mustBe false
    }
  }

  "calling url encoder" must {
    "encode the url" in {
      urlEncode("http://foo") mustBe "http%3A%2F%2Ffoo"
    }
  }

  "dynamicDateRangeHtmlNonBreak " must {
    "given two dates return a formatted string" in {
      val now = new LocalDate()
      val endOfTaxYear = TaxYearResolver.endOfCurrentTaxYear
      val expectedNow = htmlNonBroken(Dates.formatDate(now))
      val expectedEnd = htmlNonBroken(Dates.formatDate(endOfTaxYear))

      dynamicDateRangeHtmlNonBreak(now,endOfTaxYear) mustBe s"${expectedNow} to ${expectedEnd}"
    }

    "throw an exception if 'from' date is after the 'to' date" in {
      val now = new LocalDate()
      val yesterday = now.minusDays(1)

      val caught = intercept[IllegalArgumentException]{
        dynamicDateRangeHtmlNonBreak(now, yesterday)
      }

      caught.getMessage mustBe Messages("viewModelHelper.invalidDateRange")
    }
  }
}
