/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.{FakeTaiPlayApplication, routes}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.tai.model.domain.{DividendTax, EstimatedTaxYouOweThisYear, MarriageAllowanceTransferred, TaxAccountSummary}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.urls.Link

class PotentialUnderpaymentViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "PotentialUnderpaymentViewModel apply method" must {


    "create a return link" when {
      "given a valid referer and resourceName" in {
        val referalPath = "http://somelocation/tax-free-allowance"
        val resourceName = "tax-free-allowance"
        PotentialUnderpaymentViewModel(tas, Nil, referalPath, resourceName).returnLink mustBe
          Link.toInternalPage(referalPath,Some(messagesApi("tai.iya.tax.free.amount.return.link"))).toHtml
      }

      "given an invalid referer and resourceName" in {
        val referalPath = "http://somelocation/someOtherResourceName"
        val resourceName = "someOtherResourceName"
        PotentialUnderpaymentViewModel(tas, Nil, referalPath, resourceName).returnLink mustBe
          Link.toInternalPage(referalPath,Some(messagesApi("tai.label.back"))).toHtml
      }

      "given a referer and resourceName from the underpayment-estimate page" in {
        val referalPath = "http://somelocation/underpayment-estimate"
        val resourceName = "underpayment-estimate"
        PotentialUnderpaymentViewModel(tas, Nil, referalPath, resourceName).returnLink mustBe
          Link.toInternalPage(routes.TaxAccountSummaryController.onPageLoad.toString,Some(messagesApi("tai.label.back"))).toHtml
      }
    }

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

    "return an instance with a iyaTaxCodeChangeAmount drawn from the 'EstimatedTaxYouOweThisYear' coding componenet where present" in {
      PotentialUnderpaymentViewModel(tas, ccs, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(33.44)
    }

    "return an instance with a iyaTaxCodeChangeAmount drawn from the first 'EstimatedTaxYouOweThisYear' coding componenet where more than one is present" in {
      val twoMatchCCs = ccs :+ CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 66.66, "EstimatedTaxYouOweThisYear")
      PotentialUnderpaymentViewModel(tas, twoMatchCCs, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(33.44)
    }

    "return an instance with a iyaTaxCodeChangeAmount of zero where no 'EstimatedTaxYouOweThisYear' coding componenet is present" in {
      val noneMatchCCs = Seq(
        CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
        CodingComponent(DividendTax, Some(1), 33.44, "DividendTax")
      )
      PotentialUnderpaymentViewModel(tas, noneMatchCCs, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(0)
      PotentialUnderpaymentViewModel(tas, Nil, "", "").iyaTaxCodeChangeAmount mustBe BigDecimal(0)
    }

    "return an instance with a title value" which {
      "is set to the current year value when no CY+1 ampount is present" in {
        PotentialUnderpaymentViewModel(tasNoCYPlusOne, Nil, "", "").pageTitle mustBe Messages("tai.iya.tax.you.owe.title")
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

  }

  val tas = TaxAccountSummary(333.22, 14500, 123.45, 133.46, 10.01)
  val tasZero = TaxAccountSummary(0, 0, 0, 0, 0)
  val tasNoCYPlusOne = TaxAccountSummary(333.22, 14500, 123.45, 133.46, 0)
  val tasCYPlusOneOnly = TaxAccountSummary(333.22, 14500, 0, 133.46, 10.01)
  val ccs = Seq(
    CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
    CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
  )
}