/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.routes
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{DividendTax, EstimatedTaxYouOweThisYear, MarriageAllowanceTransferred, TaxAccountSummary}
import utils.BaseSpec

class PotentialUnderpaymentViewModelSpec extends BaseSpec {

  "PotentialUnderpaymentViewModel apply method" must {

    "return an instance with an iyaCYAmount drawn from the totalInYearAdjustmentIntoCY value of the supplied TaxAccountSummary" in {
      PotentialUnderpaymentViewModel(tas, Nil, "", "").iyaCYAmount mustBe BigDecimal(123.45)
      PotentialUnderpaymentViewModel(tasZero, Nil, "", "").iyaCYAmount mustBe BigDecimal(0)
    }

    "return an instance with an iyaCYPlusOneAmount drawn from the totalInYearAdjustmentIntoCYPlusOne value of the supplied TaxAccountSummary" in {
      PotentialUnderpaymentViewModel(tas, Nil, "", "").iyaCYPlusOneAmount mustBe BigDecimal(10.01)
      PotentialUnderpaymentViewModel(tasZero, Nil, "", "").iyaCYPlusOneAmount mustBe BigDecimal(0)
    }

    "return an instance with an iyaTotalAmount drawn from the totalInYearAdjustment value of the supplied TaxAccountSummary" in {
      PotentialUnderpaymentViewModel(tas, Nil, "", "").iyaTotalAmount mustBe BigDecimal(133.46)
      PotentialUnderpaymentViewModel(tasZero, Nil, "", "").iyaTotalAmount mustBe BigDecimal(0)
    }

    "return an instance with a iyaTaxCodeChangeAmount drawn from the 'EstimatedTaxYouOweThisYear' coding component where present" in {
      PotentialUnderpaymentViewModel(tas, ccs, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(33.44)
    }

    "return an instance with a iyaTaxCodeChangeAmount drawn from the first 'EstimatedTaxYouOweThisYear' coding component where more than one is present" in {
      val twoMatchCCs = ccs :+ CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 66.66, "EstimatedTaxYouOweThisYear")
      PotentialUnderpaymentViewModel(tas, twoMatchCCs, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(33.44)
    }

    "return an instance with a iyaTaxCodeChangeAmount of zero where no 'EstimatedTaxYouOweThisYear' coding component is present" in {
      val noneMatchCCs = Seq(
        CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
        CodingComponent(DividendTax, Some(1), 33.44, "DividendTax")
      )
      PotentialUnderpaymentViewModel(tas, noneMatchCCs, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(0)
      PotentialUnderpaymentViewModel(tas, Nil, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(0)
    }

    "return an instance with a title value" which {
      "is set to the current year value when no CY+1 ampount is present" in {
        PotentialUnderpaymentViewModel(tasNoCYPlusOne, Nil, "", "").pageTitle mustBe Messages(
          "tai.iya.tax.you.owe.title"
        )
      }
      "is set to the general value when both CY and CY+1 amounts are present" in {
        PotentialUnderpaymentViewModel(tas, Nil, "", "").pageTitle mustBe Messages("tai.iya.tax.you.owe.title")
      }
    }

    "return an instance with a gaDimensions map value" which {
      "will set in year calc 'current year' google analytic dimensions when only CY values are present" in {
        PotentialUnderpaymentViewModel(tasNoCYPlusOne, Nil, "", "").gaDimensions mustBe
          Some(Map("valueOfIycdcPayment" -> "123.45", "iycdcReconciliationStatus" -> "Current Year"))
      }
      "will set in year calc 'next year' google analytic dimensions when only CY+1 values are present" in {
        PotentialUnderpaymentViewModel(tasCYPlusOneOnly, Nil, "", "").gaDimensions mustBe
          Some(Map("valueOfIycdcPayment" -> "10.01", "iycdcReconciliationStatus" -> "Next Year"))
      }
      "will set n year calc 'current and next year' google analytic dimensions when CY and CY+1 values are present" in {
        PotentialUnderpaymentViewModel(tas, Nil, "", "").gaDimensions mustBe
          Some(Map("valueOfIycdcPayment" -> "123.45", "iycdcReconciliationStatus" -> "Current and Next Year"))
      }
      "is set to None if neither CY nor CY+1 values are present" in {
        PotentialUnderpaymentViewModel(tasZero, Nil, "", "").gaDimensions mustBe None
      }
    }

    "return an instance with a return link" which {
      "includes tax-free-allowance link and link text" in {
        val returnLink =
          PotentialUnderpaymentViewModel(tas, Nil, "referrer", "tax-free-allowance").returnLink.toString()
        returnLink must include("href=\"referrer\"")
        returnLink must include(messages("tai.iya.tax.free.amount.return.link"))
      }
      "includes detailed-income-tax-estimate and link text" in {
        val returnLink =
          PotentialUnderpaymentViewModel(tas, Nil, "referrer", "detailed-income-tax-estimate").returnLink.toString()
        returnLink must include("href=\"referrer\"")
        returnLink must include(messages("tai.iya.detailed.paye.return.link"))
      }
      "includes your-tax-free-amount link and link text" in {
        val returnLink =
          PotentialUnderpaymentViewModel(tas, Nil, "referrer", "your-tax-free-amount").returnLink.toString()
        returnLink must include("href=\"referrer\"")
        returnLink must include(messages("tai.iya.tax.code.change.return.link"))
      }
      "includes default link to tax account summary and link text" in {

        val returnLink = PotentialUnderpaymentViewModel(tas, Nil, "referrer", "NA").returnLink.toString()
        returnLink must include(s"href=\"${routes.TaxAccountSummaryController.onPageLoad().url}\"")
        returnLink must include(messages("return.to.your.income.tax.summary"))

      }
    }

  }

  val tas              = TaxAccountSummary(333.22, 14500, 123.45, 133.46, 10.01)
  val tasZero          = TaxAccountSummary(0, 0, 0, 0, 0)
  val tasNoCYPlusOne   = TaxAccountSummary(333.22, 14500, 123.45, 133.46, 0)
  val tasCYPlusOneOnly = TaxAccountSummary(333.22, 14500, 0, 133.46, 10.01)
  val ccs              = Seq(
    CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
    CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
  )
}
