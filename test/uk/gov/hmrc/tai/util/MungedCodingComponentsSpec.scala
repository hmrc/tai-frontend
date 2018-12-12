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
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{DividendTax, JobExpenses, MarriageAllowanceReceived}


class MungedCodingComponentsSpec extends PlaySpec {




  "#apply" should {
    "return a MungedCodingComponent" in {
      val expected = MungedCodingComponents(Seq.empty, Seq.empty)
      val actual = MungedCodingComponents(Seq.empty)

      expected mustBe actual
    }

    "apply additions" when {
      "a single current allowance component type is passed" in {
        val currentCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 5885))
        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 5885))

        val actual = MungedCodingComponents(Seq.empty, currentCodingComponents)

        pairs mustBe actual.munged
      }

      "multiple current allowance component types are passed" in {
        val currentCodingComponents = Seq(
          CodingComponent(MarriageAllowanceReceived, Some(123), 5885),
          CodingComponent(JobExpenses, Some(456), 1000)
        )

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 5885),
          CodingComponentPair(JobExpenses, Some(456), 0, 1000)
        )

        val actual = MungedCodingComponents(Seq.empty, currentCodingComponents)

        pairs mustBe actual.munged
      }

      "matches the current allowance components and previous allowance components where type and employment is the same" in {
        val currentCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 2000))
        val previousCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 1000))

        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, Some(123), 1000, 2000))

        val actual = MungedCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.munged
      }

      "matches the current allowance components and previous allowance components where type is the same and employment id is none" in {
        val currentCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, None, 2000))
        val previousCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, None, 1000))

        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, None, 1000, 2000))

        val actual = MungedCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.munged
      }

      "does not create pairs when component types in current and previous are different" in {
        val currentCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 2000))
        val previousCodingComponents = Seq(CodingComponent(JobExpenses, Some(123), 1000))

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 2000),
          CodingComponentPair(JobExpenses, Some(123), 1000, 0)
        )

        val actual = MungedCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.munged
      }

      "does not create pairs when employment id in current and previous are different" in {
        val currentCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 2000))
        val previousCodingComponents = Seq(CodingComponent(MarriageAllowanceReceived, Some(456), 1000))

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), 0, 2000),
          CodingComponentPair(MarriageAllowanceReceived, Some(456), 1000, 0)
        )

        val actual = MungedCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.munged
      }
    }
  }
}
