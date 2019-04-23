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

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.TaxFreeAmountDetails
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.model.domain.{CarBenefit, EstimatedTaxYouOweThisYear, GiftAidPayments, UnderPaymentFromPreviousYear}

class TaxSummaryLabelSpec extends PlaySpec with FakeTaiPlayApplication {
    implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
  val incomeCatergories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
  val totalTax : TotalTax = TotalTax(1000, Seq(incomeCatergories), None, None, None)
  val taxFreeAmountDetails = TaxFreeAmountDetails(Map.empty, Seq.empty, totalTax)
    "#TaxSummaryLabel" should {
      "return a human readable coding component and pass through the current and previous amounts" in {
        val actual = TaxSummaryLabel(GiftAidPayments, employmentId = None, taxFreeAmountDetails, amount = 1000)
        actual mustBe TaxSummaryLabel("Gift Aid Payments", None)
      }

      "return a human readable coding component" when {
        "employment id is found in the employment names map" in {
          val id = 123
          val employmentIds = Map(id -> "Employer")
          val taxFreeAmountDetails = TaxFreeAmountDetails(employmentIds, Seq.empty, totalTax)
          val actual = TaxSummaryLabel(GiftAidPayments, Some(id), taxFreeAmountDetails, amount = 1000)

          actual mustBe TaxSummaryLabel("Gift Aid Payments from Employer", None)
        }

        "there is a car benefit" should {
          "display a generic car benefit message when there is no ID" in {
            val companyCarBenefits = Seq(CompanyCarBenefit(456, 123, Seq.empty))
            val taxFreeAmountDetails = TaxFreeAmountDetails(Map.empty, companyCarBenefits, totalTax)

            val actual = {
              val unmatchedEmploymentId = None
              TaxSummaryLabel(CarBenefit, unmatchedEmploymentId, taxFreeAmountDetails, amount = 1000)
            }

            actual mustBe TaxSummaryLabel("Car benefit", None)
          }

          "display a generic car benefit message when there is no matching ID" in {
            val companyCarBenefits = Seq(CompanyCarBenefit(456, 123, Seq.empty))
            val taxFreeAmountDetails = TaxFreeAmountDetails(Map.empty, companyCarBenefits, totalTax)

            val actual = {
              val unmatchedEmploymentId = Some(999)
              TaxSummaryLabel(CarBenefit, unmatchedEmploymentId, taxFreeAmountDetails, amount = 1000)
            }

            actual mustBe TaxSummaryLabel("Car benefit", None)
          }

          "display the car make model when there is a matching ID" in {
            val id = 123

            val companyCarBenefits = {
              val companyCar = CompanyCar(98, "Make Model", false, None, None, None)
              Seq(CompanyCarBenefit(id, 45678, Seq(companyCar)))
            }
            val employmentIds = Map(id -> "Employer Name")

            val taxFreeAmountDetails = TaxFreeAmountDetails(employmentIds, companyCarBenefits, totalTax)

            val actual = TaxSummaryLabel(CarBenefit, Some(id), taxFreeAmountDetails, amount = 1000)

            actual mustBe TaxSummaryLabel("Make Model from Employer Name", None)
          }
        }
      }

      "show the underpayment explanation link" when {
        "tax component type is an underPaymentFromPreviousYear" in {
          val href = controllers.routes.UnderpaymentFromPreviousYearController.underpaymentExplanation.url.toString
          val id = "underPaymentFromPreviousYear"
          val link = Some(HelpLink(Messages("tai.taxFreeAmount.table.underpaymentFromPreviousYear.link", "£200"), href, id))

          val actual = TaxSummaryLabel(UnderPaymentFromPreviousYear, employmentId = None, taxFreeAmountDetails, amount = 1000)
          actual mustBe TaxSummaryLabel("Underpayment from previous year", link)
        }

        "tax component type is an EstimatedTaxYouOweThisYear" in {
          val href = controllers.routes.PotentialUnderpaymentController.potentialUnderpaymentPage.url.toString
          val id = "estimatedTaxOwedLink"
          val link = Some(HelpLink(Messages("tai.taxFreeAmount.table.underpaymentFromCurrentYear.link", "£200"), href, id))

          val actual = TaxSummaryLabel(EstimatedTaxYouOweThisYear, employmentId = None, taxFreeAmountDetails, amount = 1000)
          actual mustBe TaxSummaryLabel("Estimated tax you owe this year", link)
        }
      }

      "do not show the underpayment explanation link" when {
        "there are missing incomeCatergories" in {
          val totalTax : TotalTax = TotalTax(1000, Seq.empty, None, None, None)
          val taxFreeAmountDetails = TaxFreeAmountDetails(Map.empty, Seq.empty, totalTax)

          val actual = TaxSummaryLabel(UnderPaymentFromPreviousYear, employmentId = None, taxFreeAmountDetails, amount = 1000)
          actual mustBe TaxSummaryLabel("Underpayment from previous year", None)
        }
      }
    }
}
