/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.tai.model.CodingComponentPair
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain._

class AllowancesAndDeductionPairsSpec extends PlaySpec {

  val inputAmount = Some(BigDecimal(456))

  private def createCodingComponent(
    allowance: TaxComponentType,
    employmentId: Option[Int],
    allowanceAmount: BigDecimal,
    inputAmount: Option[BigDecimal]): CodingComponent =
    CodingComponent(allowance, employmentId, allowanceAmount, allowance.toString, inputAmount)

  "#fromCodingComponents" should {
    "return a AllowancesAndDeductionPairs" in {
      val expected = AllowancesAndDeductionPairs(Seq.empty[CodingComponentPair], Seq.empty[CodingComponentPair])
      val actual =
        AllowancesAndDeductionPairs.fromCodingComponents(Seq.empty[CodingComponent], Seq.empty[CodingComponent])

      expected mustBe actual
    }

    "create coding component pairs" when {
      "a single current component type is passed" in {
        val currentCodingComponents =
          Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 5885, inputAmount))
        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, Some(123), None, Some(5885), inputAmount))

        val actual = AllowancesAndDeductionPairs.fromCodingComponents(Seq.empty, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "the current components and previous components where type and employment is the same" in {
        val currentCodingComponents =
          Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 2000, inputAmount))
        val previousCodingComponents =
          Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 1000, inputAmount))

        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, Some(123), Some(1000), Some(2000), inputAmount))

        val actual = AllowancesAndDeductionPairs.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "the current components and previous components where type is the same and employment id is none" in {
        val currentCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, None, 2000, inputAmount))
        val previousCodingComponents = Seq(createCodingComponent(MarriageAllowanceReceived, None, 1000, None))

        val pairs = Seq(CodingComponentPair(MarriageAllowanceReceived, None, Some(1000), Some(2000), inputAmount))

        val actual = AllowancesAndDeductionPairs.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "does not create pairs when component types in current and previous are different" in {
        val currentCodingComponents =
          Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 2000, inputAmount))
        val previousCodingComponents = Seq(createCodingComponent(JobExpenses, Some(123), 1000, None))

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), None, Some(2000), inputAmount),
          CodingComponentPair(JobExpenses, Some(123), Some(1000), None, None)
        )

        val actual = AllowancesAndDeductionPairs.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }

      "does not create pairs when employment id in current and previous are different" in {
        val currentCodingComponents =
          Seq(createCodingComponent(MarriageAllowanceReceived, Some(123), 2000, inputAmount))
        val previousCodingComponents =
          Seq(createCodingComponent(MarriageAllowanceReceived, Some(456), 1000, inputAmount))

        val pairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), None, Some(2000), inputAmount),
          CodingComponentPair(MarriageAllowanceReceived, Some(456), Some(1000), None, inputAmount)
        )

        val actual = AllowancesAndDeductionPairs.fromCodingComponents(previousCodingComponents, currentCodingComponents)

        pairs mustBe actual.allowances
      }
    }

    "partitions allowances and deductions" when {
      "multiple current allowance component types are passed" in {
        val currentCodingComponents = Seq(
          createCodingComponent(MarriageAllowanceReceived, Some(123), 5885, inputAmount),
          createCodingComponent(JobExpenses, Some(456), 1000, None),
          createCodingComponent(ChildBenefit, Some(123), 5885, inputAmount),
          createCodingComponent(OutstandingDebt, Some(456), 1000, None)
        )

        val allowancePairs = Seq(
          CodingComponentPair(MarriageAllowanceReceived, Some(123), None, Some(5885), inputAmount),
          CodingComponentPair(JobExpenses, Some(456), None, Some(1000), None)
        )

        val deductionPairs = Seq(
          CodingComponentPair(ChildBenefit, Some(123), None, Some(5885), inputAmount),
          CodingComponentPair(OutstandingDebt, Some(456), None, Some(1000), None)
        )

        val actual =
          AllowancesAndDeductionPairs.fromCodingComponents(Seq.empty[CodingComponent], currentCodingComponents)

        allowancePairs mustBe actual.allowances
        deductionPairs mustBe actual.deductions
      }
    }

    "filters out PersonalAllowance component types" in {
      val currentCodingComponents = Seq(
        createCodingComponent(PersonalAllowancePA, None, 5885, None),
        createCodingComponent(PersonalAllowanceAgedPAA, None, 5885, None),
        createCodingComponent(PersonalAllowanceElderlyPAE, None, 5885, None),
        createCodingComponent(JobExpenses, None, 1000, None)
      )

      val pairs = Seq(
        CodingComponentPair(JobExpenses, None, None, Some(1000), None)
      )

      val actual = AllowancesAndDeductionPairs.fromCodingComponents(Seq.empty[CodingComponent], currentCodingComponents)

      pairs mustBe actual.allowances
    }

    "include AllowanceComponentTypes for allowances" in {
      val currentCodingComponents = Seq(
        createCodingComponent(BenefitInKind, None, 5885, inputAmount),
        createCodingComponent(MarriedCouplesAllowanceToWifeMAW, None, 5885, None),
        createCodingComponent(NonCodedIncome, None, 5885, None),
        createCodingComponent(GiftAidPayments, None, 1000, inputAmount)
      )

      val pairs = Seq(
        CodingComponentPair(GiftAidPayments, None, None, Some(1000), inputAmount)
      )

      val actual = AllowancesAndDeductionPairs.fromCodingComponents(Seq.empty[CodingComponent], currentCodingComponents)

      pairs mustBe actual.allowances
    }
  }
}
