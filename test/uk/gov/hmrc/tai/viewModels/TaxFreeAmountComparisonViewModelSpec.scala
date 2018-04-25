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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import play.api.i18n.Messages.Implicits._

class TaxFreeAmountComparisonViewModelSpec extends PlaySpec with FakeTaiPlayApplication {

  "Tax Free Amount comparison view model" must {
    "return empty model" when {
      "no coding components and taxSummary is present" in {
        val model = TaxFreeAmountComparisonViewModel(Seq.empty[CodingComponentForYear], Seq.empty[TaxAccountSummaryForYear])
        model.personalAllowance.values mustBe Seq(0, 0)
        model.deductions.deductions mustBe Seq.empty[Row]
        model.additions.additions mustBe Seq.empty[Row]
        model.footer.values mustBe Seq.empty[BigDecimal]
      }
    }

    "return personal allowance" when {
      "valid coding components(Personal Allowance) are passed" in {
        val component = CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance")
        val deduction = CodingComponent(Tips, None, 11500, "Tips")
        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(component, deduction))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(component.copy(amount = 11850), deduction))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.personalAllowance mustBe PersonalAllowance(Seq(11500, 11850))
      }

      "personal allowance is not present" in {
        val component = CodingComponent(GiftAidAdjustment, None, 11500, "Gift Adjustment")
        val deduction = CodingComponent(Tips, None, 11500, "Tips")
        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(component, deduction))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(component.copy(amount = 11850), deduction))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.personalAllowance mustBe PersonalAllowance(Seq(0, 0))
      }

      "personal allowance is not present for current year but available for next year" in {
        val component = CodingComponent(GiftAidAdjustment, None, 11500, "Gift Adjustment")
        val deduction = CodingComponent(Tips, None, 11500, "Tips")
        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(component, deduction))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(component.copy(componentType = PersonalAllowancePA, amount = 11850), deduction))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.personalAllowance mustBe PersonalAllowance(Seq(0, 11850))
      }

      "personal allowance is present for current year but not available for next year" in {
        val component = CodingComponent(PersonalAllowancePA, None, 11500, "Gift Adjustment")
        val deduction = CodingComponent(Tips, None, 11500, "Tips")
        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(component, deduction))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(component.copy(componentType = GiftAidAdjustment, amount = 11850), deduction))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.personalAllowance mustBe PersonalAllowance(Seq(11500, 0))
      }
    }


    "return additions" when {
      "personal allowance is present" in {
        val paAllowance = CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance")
        val giftAidAllowance = CodingComponent(ProfessionalSubscriptions, None, 100, "ProfessionalSubscriptions")
        val marriageAllowance = CodingComponent(MarriageAllowanceReceived, None, 1000, "Marriage Allowance Received")

        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(paAllowance, giftAidAllowance, marriageAllowance))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(paAllowance, giftAidAllowance.copy(amount = 150)))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.additions.additions mustBe Seq(
          Row("ProfessionalSubscriptions", Seq(Some(100), Some(150))),
          Row("MarriageAllowanceReceived", Seq(Some(1000), None))
        )

        model.additions.totalRow mustBe Total(Seq(1100, 150))
      }

      "personal allowance is not present" in {
        val giftAidAllowance = CodingComponent(ProfessionalSubscriptions, None, 100, "ProfessionalSubscriptions")
        val marriageAllowance = CodingComponent(MarriageAllowanceReceived, None, 1000, "Marriage Allowance Received")

        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(giftAidAllowance, marriageAllowance))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(giftAidAllowance.copy(amount = 150)))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.additions.additions mustBe Seq(
          Row("ProfessionalSubscriptions", Seq(Some(100), Some(150))),
          Row("MarriageAllowanceReceived", Seq(Some(1000), None))
        )
      }
    }

    "return addition total row" when {
      "there are additions" in {
        val giftAidAllowance = CodingComponent(ProfessionalSubscriptions, None, 100, "ProfessionalSubscriptions")
        val marriageAllowance = CodingComponent(MarriageAllowanceReceived, None, 1000, "Marriage Allowance Received")

        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(giftAidAllowance, marriageAllowance))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(giftAidAllowance.copy(amount = 150)))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.additions.totalRow.totals mustBe Seq(1100, 150)
        model.hasAdditions mustBe true
      }

      "there are no additions" in {
        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq.empty[CodingComponent])
        val nextYearComponents = CodingComponentForYear(nextTaxYear, Seq.empty[CodingComponent])

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.additions.totalRow.totals mustBe Seq(0, 0)
        model.hasAdditions mustBe false
      }
    }

    "return deductions" in {
      val giftAidDeduction = CodingComponent(GiftAidAdjustment, None, 100, "Gift Adjustment")
      val marriageDeduction = CodingComponent(MarriageAllowanceTransferred, None, 1000, "Marriage Allowance Received")

      val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(giftAidDeduction, marriageDeduction))
      val nextYearComponents = CodingComponentForYear(nextTaxYear,
        Seq(giftAidDeduction.copy(amount = 150)))

      val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
        Seq.empty[TaxAccountSummaryForYear])

      model.deductions.deductions mustBe Seq(
        Row("GiftAidAdjustment", Seq(Some(100), Some(150))),
        Row("MarriageAllowanceTransferred", Seq(Some(1000), None))
      )
    }

    "return deduction total row" when {
      "there are deductions" in {
        val giftAidDeduction = CodingComponent(GiftAidAdjustment, None, 100, "Gift Adjustment")
        val marriageDeduction = CodingComponent(MarriageAllowanceTransferred, None, 1000, "Marriage Allowance Received")

        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq(giftAidDeduction, marriageDeduction))
        val nextYearComponents = CodingComponentForYear(nextTaxYear,
          Seq(giftAidDeduction.copy(amount = 150)))

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.deductions.totalRow mustBe Total(Seq(1100, 150))
        model.hasDeductions mustBe true
      }

      "there are no deductions" in {
        val currentYearComponents = CodingComponentForYear(currentTaxYear, Seq.empty[CodingComponent])
        val nextYearComponents = CodingComponentForYear(nextTaxYear, Seq.empty[CodingComponent])

        val model = TaxFreeAmountComparisonViewModel(Seq(nextYearComponents, currentYearComponents),
          Seq.empty[TaxAccountSummaryForYear])

        model.deductions.totalRow mustBe Total(Seq(0, 0))
        model.hasDeductions mustBe false
      }
    }

    "return footer" in {
      val currentYearComponents = TaxAccountSummaryForYear(currentTaxYear, TaxAccountSummary(100, 100, 100, 300, 200))
      val nextYearComponents = TaxAccountSummaryForYear(nextTaxYear, TaxAccountSummary(200, 200, 200, 200, 0))

      val model = TaxFreeAmountComparisonViewModel(Seq.empty[CodingComponentForYear],
        Seq(currentYearComponents, nextYearComponents))

      model.footer mustBe Footer(Seq(100, 200))
    }
  }

  private val currentTaxYear = uk.gov.hmrc.tai.model.TaxYear()
  private val nextTaxYear = currentTaxYear.next

}
