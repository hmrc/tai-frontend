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

package uk.gov.hmrc.tai.util

import java.time.LocalDate
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.model.TaxYear
import utils.BaseSpec

import java.time.format.DateTimeFormatter

class TaxYearRangeUtilSpec extends BaseSpec {

  "Tax year range util " must {

    "return the current tax year as a range delimited with the word 'to' " in {
      val expectedTaxYear = messages(
        "tai.taxYear",
        HtmlFormatter.htmlNonBroken(TaxYear().start.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
        HtmlFormatter.htmlNonBroken(TaxYear().end.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))
      )

      TaxYearRangeUtil.currentTaxYearRange mustBe expectedTaxYear
    }

    "return the current tax year as a range delimited with the word 'and' " in {
      val expectedTaxYear = messages(
        "tai.taxYear.between",
        HtmlFormatter.htmlNonBroken(TaxYear().start.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
        HtmlFormatter.htmlNonBroken(TaxYear().end.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))
      )

      TaxYearRangeUtil.currentTaxYearRangeBetweenDelimited mustBe expectedTaxYear
    }

    "return the current tax year as a range that only contains the year" in {
      val expectedTaxYear = messages(
        "tai.taxYear",
        TaxYear().start.format(DateTimeFormatter.ofPattern("yyyy")),
        TaxYear().end.format(DateTimeFormatter.ofPattern("yyyy")))

      TaxYearRangeUtil.currentTaxYearRangeYearOnly mustBe expectedTaxYear
    }

    "given two dates return a formatted string" in {

      implicit lazy val lang: Lang = Lang("en")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      val now = LocalDate.now
      val endOfTaxYear = TaxYear().end
      val expectedNow = HtmlFormatter.htmlNonBroken(langUtils.Dates.formatDate(now))
      val expectedEnd = HtmlFormatter.htmlNonBroken(langUtils.Dates.formatDate(endOfTaxYear))

      TaxYearRangeUtil.dynamicDateRange(now, endOfTaxYear) mustBe s"$expectedNow to $expectedEnd"
    }

    "given two dates return a formatted string in welsh" in {

      implicit lazy val lang: Lang = Lang("cy")
      implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang))

      val now = LocalDate.now
      val endOfTaxYear = TaxYear().end
      val expectedNow = HtmlFormatter.htmlNonBroken(langUtils.Dates.formatDate(now))
      val expectedEnd = HtmlFormatter.htmlNonBroken(langUtils.Dates.formatDate(endOfTaxYear))

      TaxYearRangeUtil.dynamicDateRange(now, endOfTaxYear) mustBe s"$expectedNow i $expectedEnd"
    }

    "throw an exception if 'from' date is after the 'to' date" in {
      val now = LocalDate.now
      val yesterday = now.minusDays(1)

      val caught = intercept[IllegalArgumentException] {
        TaxYearRangeUtil.dynamicDateRange(now, yesterday)
      }

      caught.getMessage mustBe s"From date:$now cannot be after To date:$yesterday"
    }
  }
}
