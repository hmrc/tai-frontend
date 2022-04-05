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

import java.time.LocalDate
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import uk.gov.hmrc.tai.model
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.Live
import utils.BaseSpec

class NoCYIncomeTaxErrorViewModelSpec extends BaseSpec {

  "NoCYIncomeTaxErrorViewModel" should {

    "return no end date" when {
      "no employments are supplied" in {
        val sut = createSut(Nil)
        sut.endDate mustBe None
      }
      "only one employment is present in the seq but without an end date" in {
        val employment =
          Employment("test employment", Live, Some("111111"), empStartDateOne, None, Nil, "", "", 2, None, false, false)

        val sut = createSut(Seq(employment))
        sut.endDate mustBe None
      }
      "multiple employments are present in the seq with no end dates" in {
        val employment =
          Employment("test employment", Live, Some("111111"), empStartDateOne, None, Nil, "", "", 2, None, false, false)

        val employment1 =
          Employment(
            "test employment1",
            Live,
            Some("222222"),
            empStartDateTwo,
            None,
            Nil,
            "",
            "",
            2,
            None,
            false,
            false)

        val employment2 =
          Employment(
            "test employment2",
            Live,
            Some("333333"),
            empStartDateThree,
            None,
            Nil,
            "",
            "",
            2,
            None,
            false,
            false)

        val sut = createSut(Seq(employment, employment1, employment2))
        sut.endDate mustBe None
      }
    }

    "return date of employment" when {
      "only one employment is present in the seq" in {
        val employment = Employment(
          "test employment",
          Live,
          Some("111111"),
          empStartDateOne,
          Some(empEndDateOne),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)

        val sut = createSut(Seq(employment))
        sut.endDate mustBe Some(Dates.formatDate(empEndDateOne))
      }

    }

    "return the most recent employment's end date" when {
      "multiple employments are present in the seq with one of them doesnt have end date" in {
        val employment =
          Employment("test employment", Live, Some("111111"), empStartDateOne, None, Nil, "", "", 2, None, false, false)

        val employment1 = Employment(
          "test employment1",
          Live,
          Some("222222"),
          empStartDateTwo,
          Some(empEndDateTwo),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)

        val employment2 = Employment(
          "test employment2",
          Live,
          Some("333333"),
          empStartDateThree,
          Some(empEndDateThree),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)

        val sut = createSut(Seq(employment, employment1, employment2))
        sut.endDate mustBe Some(Dates.formatDate(empEndDateThree))
      }
      "multiple employments are present in the seq with all of them having end dates" in {
        val employment = Employment(
          "test employment",
          Live,
          Some("111111"),
          empStartDateOne,
          Some(empEndDateOne),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)

        val employment1 = Employment(
          "test employment1",
          Live,
          Some("222222"),
          empStartDateTwo,
          Some(empEndDateTwo),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)

        val employment2 = Employment(
          "test employment2",
          Live,
          Some("333333"),
          empStartDateThree,
          Some(empEndDateThree),
          Nil,
          "",
          "",
          2,
          None,
          false,
          false)

        val sut = createSut(Seq(employment, employment1, employment2))
        sut.endDate mustBe Some(Dates.formatDate(empEndDateThree))
      }
    }
  }

  private val currentYear: Int = LocalDate.now().getYear
  private val cyMinusOneTaxYear: TaxYear = model.TaxYear(currentYear - 1)

  private val empStartDateOne = cyMinusOneTaxYear.start.plusMonths(2)
  private val empEndDateOne = cyMinusOneTaxYear.start.plusMonths(7)

  private val empStartDateTwo = cyMinusOneTaxYear.start.plusMonths(1)
  private val empEndDateTwo = cyMinusOneTaxYear.start.plusMonths(4)

  private val empStartDateThree = cyMinusOneTaxYear.start.plusMonths(5)
  private val empEndDateThree = cyMinusOneTaxYear.start.plusMonths(9)

  private def createSut(employments: Seq[Employment]) = NoCYIncomeTaxErrorViewModel(employments)
}
