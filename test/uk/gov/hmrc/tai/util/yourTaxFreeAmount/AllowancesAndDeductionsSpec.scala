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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{AllowanceComponentType, JobExpenses, MarriageAllowanceReceived}


class AllowancesAndDeductionsSpec extends PlaySpec {

  private def createCodingComponent(allowance: AllowanceComponentType,employmentId: Option[Int], allowanceAmount: BigDecimal): CodingComponent = {
    CodingComponent(allowance, employmentId, allowanceAmount, allowance.toString)
  }

  "#fromCodingComponents" should {
    "return a MungedCodingComponent" in {
      val expected = AllowancesAndDeductions.fromCodingComponents(Seq.empty, Seq.empty)
      val actual = AllowancesAndDeductions.fromCodingComponents(Seq.empty[CodingComponent], Seq.empty[CodingComponent])

      expected mustBe actual
    }

    "apply additions" when {
      "a single current allowance component type is passed" in {
        val currentCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 5885))
        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 5885))

        val actual = AllowancesAndDeductions.fromCodingComponents(Seq.empty, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "multiple current allowance component types are passed" in {
        val currentCodingComponents = Seq(
          createCodingComponent(MarriageAllowanceReceived, Some(123), 5885),
          createCodingComponent(JobExpenses, Some(456), 1000)
        )

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 5885),
          CodingComponentPair(JobExpenses, Some(456), 0, 1000)
        )

        val actual = AllowancesAndDeductions.fromCodingComponents(Seq.empty[CodingComponent], currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "matches the current allowance components and previous allowance components where type and employment is the same" in {
        val currentCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 2000))
        val previousCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 1000))

        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, Some(123), 1000, 2000))

        val actual = AllowancesAndDeductions.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "matches the current allowance components and previous allowance components where type is the same and employment id is none" in {
        val currentCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, None, 2000))
        val previousCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, None, 1000))

        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, None, 1000, 2000))

        val actual = AllowancesAndDeductions.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "does not create pairs when component types in current and previous are different" in {
        val currentCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 2000))
        val previousCodingComponents = Seq(createCodingComponent(JobExpenses, Some(123), 1000))

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 2000),
          CodingComponentPair(JobExpenses, Some(123), 1000, 0)
        )

        val actual = AllowancesAndDeductions.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "does not create pairs when employment id in current and previous are different" in {
        val currentCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 2000))
        val previousCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, Some(456), 1000))

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 2000),
          CodingComponentPair(MarriageAllowanceReceived, Some(456), 1000, 0)
        )

        val actual = AllowancesAndDeductions.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }
    }

    "apply deductions" when {

    }
  }
}
