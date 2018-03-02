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

package controllers.viewModels

import hmrc.nps2.IabdType
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.domain.BankAccount
import uk.gov.hmrc.tai.model.{IabdSummary, IncreasesTax, TaxComponent, TaxSummaryDetails}
import uk.gov.hmrc.tai.viewModels.YourTaxableIncomeViewModelV2

import scala.util.Random

class YourTaxableIncomeViewModelV2Spec extends PlaySpec with MockitoSugar {

  "YourTaxableIncomeViewModelV2 apply method" should {
    "return the model with company car field as false" when {
      "there is no record of an IABD" in {
        val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1)
        YourTaxableIncomeViewModelV2.apply(taxSummaryDetails, List.empty[BankAccount]) mustBe YourTaxableIncomeViewModelV2(false, false)
      }

      "there are IABDs but none for car benefit" in {
        val empBenefitIabdList = List(medicalInsuranceIabdSummary)
        val codingComponent = TaxComponent(300, 0, "Employer Benefits", empBenefitIabdList)
        val increasesTax = IncreasesTax(None, Some(codingComponent), 3000)
        val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1, increasesTax = Some(increasesTax))

        YourTaxableIncomeViewModelV2.apply(taxSummaryDetails, List.empty[BankAccount]) mustBe YourTaxableIncomeViewModelV2(false, false)
      }
    }

    "return the model with company car field as true" when {
      "there is a record of a company car" in {
        val empBenefitIabdList = List(carBenefitIabdSummary1)
        val codingComponent = TaxComponent(300, 0, "Employer Benefits", empBenefitIabdList)
        val increasesTax = IncreasesTax(None, Some(codingComponent), 3000)
        val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1, increasesTax = Some(increasesTax))

        YourTaxableIncomeViewModelV2.apply(taxSummaryDetails, List.empty[BankAccount]) mustBe YourTaxableIncomeViewModelV2(true, false)
      }

      "there are multiple IABD records, some of which are for company car" in {
        val empBenefitIabdList = List(carBenefitIabdSummary1,
          carBenefitIabdSummary2,
          medicalInsuranceIabdSummary)
        val codingComponent = TaxComponent(300, 0, "Employer Benefits", empBenefitIabdList)
        val increasesTax = IncreasesTax(None, Some(codingComponent), 3000)
        val taxSummaryDetails = TaxSummaryDetails(nino.nino, 1, increasesTax = Some(increasesTax))

        YourTaxableIncomeViewModelV2.apply(taxSummaryDetails, List.empty[BankAccount]) mustBe YourTaxableIncomeViewModelV2(true, false)
      }
    }

    "return the model with bbsi accounts field as true" when {
      "there are bank accounts present" in {
        val bankAccount = BankAccount(id = 1, None, None, None, grossInterest = 123.45, None)
        val taxableIncomeModel = YourTaxableIncomeViewModelV2(taxSummaryDetailsDummy, List(bankAccount))

        taxableIncomeModel.hasBankAccount mustBe true
      }
    }

    "return the model with bbsi accounts field as false" when {
      "there are no accounts present" in {
        val taxableIncomeModel = YourTaxableIncomeViewModelV2(taxSummaryDetailsDummy, List.empty[BankAccount])
        taxableIncomeModel.hasBankAccount mustBe false
      }
    }

  }

  val nino: Nino = new Generator(new Random).nextNino
  private val taxSummaryDetailsDummy = TaxSummaryDetails(nino.nino, 1)
  val medicalInsuranceIabdSummary = IabdSummary(IabdType.MedicalInsurance.code, "Medical Insurance", 100, Some(1), Some(49), Some("employer1"))
  val carBenefitIabdSummary1 = IabdSummary(IabdType.CarBenefit.code, "Car Benefit", 100, Some(1), Some(49), Some("employer2"))
  val carBenefitIabdSummary2 = IabdSummary(IabdType.CarBenefit.code, "Car Benefit 2", 200, Some(1), Some(49), Some("employer1"))
}