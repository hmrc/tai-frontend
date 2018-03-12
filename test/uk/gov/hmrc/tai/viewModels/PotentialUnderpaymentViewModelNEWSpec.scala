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
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain.{DividendTax, EstimatedTaxYouOweThisYear, MarriageAllowanceTransferred, TaxAccountSummary}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

class PotentialUnderpaymentViewModelNEWSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "PotentialUnderpaymentViewModel apply method" must {

    "return an instance with an iyaCYAmount drawn from the totalInYearAdjustmentIntoCY value of the supplied TaxAccountSummary" in {
      PotentialUnderpaymentViewModelNEW(tas, Nil).iyaCYAmount mustBe BigDecimal(123.45)
      PotentialUnderpaymentViewModelNEW(tasZero, Nil).iyaCYAmount mustBe BigDecimal(0)
    }

    "return an instance with an iyaCYPlusOneAmount drawn from the totalInYearAdjustmentIntoCYPlusOne value of the supplied TaxAccountSummary" in {
      PotentialUnderpaymentViewModelNEW(tas, Nil).iyaCYPlusOneAmount mustBe BigDecimal(10.01)
      PotentialUnderpaymentViewModelNEW(tasZero, Nil).iyaCYPlusOneAmount mustBe BigDecimal(0)
    }

    "return an instance with an iyaTotalAmount drawn from the totalInYearAdjustment value of the supplied TaxAccountSummary" in {
      PotentialUnderpaymentViewModelNEW(tas, Nil).iyaTotalAmount mustBe BigDecimal(133.46)
      PotentialUnderpaymentViewModelNEW(tasZero, Nil).iyaTotalAmount mustBe BigDecimal(0)
    }

    "return an instance with a iyaTaxCodeChangeAmount drawn from the 'EstimatedTaxYouOweThisYear' coding componenet where present" in {
      PotentialUnderpaymentViewModelNEW(tas, ccs).iyaTaxCodeChangeAmount mustBe BigDecimal(33.44)
    }

    "return an instance with a iyaTaxCodeChangeAmount drawn from the first 'EstimatedTaxYouOweThisYear' coding componenet where more than one is present" in {
      val twoMatchCCs = ccs :+ CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 66.66, "EstimatedTaxYouOweThisYear")
      PotentialUnderpaymentViewModelNEW(tas, twoMatchCCs).iyaTaxCodeChangeAmount mustBe BigDecimal(33.44)
    }

    "return an instance with a iyaTaxCodeChangeAmount of zero where no 'EstimatedTaxYouOweThisYear' coding componenet is present" in {
      val noneMatchCCs = Seq(
        CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
        CodingComponent(DividendTax, Some(1), 33.44, "DividendTax")
      )
      PotentialUnderpaymentViewModelNEW(tas, noneMatchCCs).iyaTaxCodeChangeAmount mustBe BigDecimal(0)
      PotentialUnderpaymentViewModelNEW(tas, Nil).iyaTaxCodeChangeAmount mustBe BigDecimal(0)
    }
  }

  val tas = TaxAccountSummary(333.22, 14500, 123.45, 133.46, 10.01)
  val tasZero = TaxAccountSummary(0, 0, 0, 0, 0)
  val ccs = Seq(
    CodingComponent(MarriageAllowanceTransferred, Some(1), 1400.86, "MarriageAllowanceTransfererd"),
    CodingComponent(EstimatedTaxYouOweThisYear, Some(1), 33.44, "EstimatedTaxYouOweThisYear")
  )
}
