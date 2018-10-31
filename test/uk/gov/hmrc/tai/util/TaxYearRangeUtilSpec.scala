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

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.time.TaxYearResolver
import play.api.i18n.Messages.Implicits._


class TaxYearRangeUtilSpec extends PlaySpec with FakeTaiPlayApplication {

  "dynamicDateRangeHtmlNonBreak " must {
    "given two dates return a formatted string" in {
      val now = new LocalDate()
      val endOfTaxYear = TaxYearResolver.endOfCurrentTaxYear
      val expectedNow = HtmlFormatter.htmlNonBroken(Dates.formatDate(now))
      val expectedEnd = HtmlFormatter.htmlNonBroken(Dates.formatDate(endOfTaxYear))

      TaxYearRangeUtil.dynamicDateRangeHtmlNonBreak(now,endOfTaxYear) mustBe s"${expectedNow} to ${expectedEnd}"
    }

    "throw an exception if 'from' date is after the 'to' date" in {
      val now = new LocalDate()
      val yesterday = now.minusDays(1)

      val caught = intercept[IllegalArgumentException]{
        TaxYearRangeUtil.dynamicDateRangeHtmlNonBreak(now, yesterday)
      }

      caught.getMessage mustBe s"From date:$now cannot be after To date:$yesterday"
    }
  }
}