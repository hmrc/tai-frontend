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

package uk.gov.hmrc.tai.model.domain.income

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, JsString, Json}
import uk.gov.hmrc.tai.model.domain._

class NonTaxCodeIncomeComponentTypeSpec extends PlaySpec {

  "Income component format" must {
    "create a valid object" when {
      "given a valid json value" in {
        JsString("NonCodedIncome").as[NonTaxCodeIncomeComponentType] mustBe NonCodedIncome
        JsString("Commission").as[NonTaxCodeIncomeComponentType] mustBe Commission
        JsString("OtherIncomeEarned").as[NonTaxCodeIncomeComponentType] mustBe OtherIncomeEarned
        JsString("OtherIncomeNotEarned").as[NonTaxCodeIncomeComponentType] mustBe OtherIncomeNotEarned
        JsString("PartTimeEarnings").as[NonTaxCodeIncomeComponentType] mustBe PartTimeEarnings
        JsString("Tips").as[NonTaxCodeIncomeComponentType] mustBe Tips
        JsString("OtherEarnings").as[NonTaxCodeIncomeComponentType] mustBe OtherEarnings
        JsString("CasualEarnings").as[NonTaxCodeIncomeComponentType] mustBe CasualEarnings
        JsString("ForeignDividendIncome").as[NonTaxCodeIncomeComponentType] mustBe ForeignDividendIncome
        JsString("ForeignPropertyIncome").as[NonTaxCodeIncomeComponentType] mustBe ForeignPropertyIncome
        JsString("ForeignInterestAndOtherSavings").as[NonTaxCodeIncomeComponentType] mustBe ForeignInterestAndOtherSavings
        JsString("ForeignPensionsAndOtherIncome").as[NonTaxCodeIncomeComponentType] mustBe ForeignPensionsAndOtherIncome
        JsString("StatePension").as[NonTaxCodeIncomeComponentType] mustBe StatePension
        JsString("OccupationalPension").as[NonTaxCodeIncomeComponentType] mustBe OccupationalPension
        JsString("PublicServicesPension").as[NonTaxCodeIncomeComponentType] mustBe PublicServicesPension
        JsString("ForcesPension").as[NonTaxCodeIncomeComponentType] mustBe ForcesPension
        JsString("PersonalPensionAnnuity").as[NonTaxCodeIncomeComponentType] mustBe PersonalPensionAnnuity
        JsString("Profit").as[NonTaxCodeIncomeComponentType] mustBe Profit
        JsString("BankOrBuildingSocietyInterest").as[NonTaxCodeIncomeComponentType] mustBe BankOrBuildingSocietyInterest
        JsString("UkDividend").as[NonTaxCodeIncomeComponentType] mustBe UkDividend
        JsString("UnitTrust").as[NonTaxCodeIncomeComponentType] mustBe UnitTrust
        JsString("StockDividend").as[NonTaxCodeIncomeComponentType] mustBe StockDividend
        JsString("NationalSavings").as[NonTaxCodeIncomeComponentType] mustBe NationalSavings
        JsString("SavingsBond").as[NonTaxCodeIncomeComponentType] mustBe SavingsBond
        JsString("PurchasedLifeAnnuities").as[NonTaxCodeIncomeComponentType] mustBe PurchasedLifeAnnuities
        JsString("UntaxedInterestIncome").as[NonTaxCodeIncomeComponentType] mustBe UntaxedInterestIncome
        JsString("IncapacityBenefit").as[NonTaxCodeIncomeComponentType] mustBe IncapacityBenefit
        JsString("JobSeekersAllowance").as[NonTaxCodeIncomeComponentType] mustBe JobSeekersAllowance
        JsString("EmploymentAndSupportAllowance").as[NonTaxCodeIncomeComponentType] mustBe EmploymentAndSupportAllowance
      }

      "throw an exception" when {
        "give an invalid json value" in {
          val exception = the[JsResultException] thrownBy JsString("Wrong").as[NonTaxCodeIncomeComponentType]
          exception.getMessage must include("Invalid Non tax code component type")
        }
      }

      "create a valid json value" when {
        "given a Income Component Type" in {
          Json.toJson(NonCodedIncome) mustBe JsString("NonCodedIncome")
          Json.toJson(Commission) mustBe JsString("Commission")
          Json.toJson(OtherIncomeEarned) mustBe JsString("OtherIncomeEarned")
          Json.toJson(OtherIncomeNotEarned) mustBe JsString("OtherIncomeNotEarned")
          Json.toJson(PartTimeEarnings) mustBe JsString("PartTimeEarnings")
          Json.toJson(Tips) mustBe JsString("Tips")
          Json.toJson(OtherEarnings) mustBe JsString("OtherEarnings")
          Json.toJson(CasualEarnings) mustBe JsString("CasualEarnings")
          Json.toJson(ForeignDividendIncome) mustBe JsString("ForeignDividendIncome")
          Json.toJson(ForeignPropertyIncome) mustBe JsString("ForeignPropertyIncome")
          Json.toJson(ForeignInterestAndOtherSavings) mustBe JsString("ForeignInterestAndOtherSavings")
          Json.toJson(ForeignPensionsAndOtherIncome) mustBe JsString("ForeignPensionsAndOtherIncome")
          Json.toJson(StatePension) mustBe JsString("StatePension")
          Json.toJson(OccupationalPension) mustBe JsString("OccupationalPension")
          Json.toJson(PublicServicesPension) mustBe JsString("PublicServicesPension")
          Json.toJson(ForcesPension) mustBe JsString("ForcesPension")
          Json.toJson(PersonalPensionAnnuity) mustBe JsString("PersonalPensionAnnuity")
          Json.toJson(Profit) mustBe JsString("Profit")
          Json.toJson(BankOrBuildingSocietyInterest) mustBe JsString("BankOrBuildingSocietyInterest")
          Json.toJson(UkDividend) mustBe JsString("UkDividend")
          Json.toJson(UnitTrust) mustBe JsString("UnitTrust")
          Json.toJson(StockDividend) mustBe JsString("StockDividend")
          Json.toJson(NationalSavings) mustBe JsString("NationalSavings")
          Json.toJson(SavingsBond) mustBe JsString("SavingsBond")
          Json.toJson(PurchasedLifeAnnuities) mustBe JsString("PurchasedLifeAnnuities")
          Json.toJson(UntaxedInterestIncome) mustBe JsString("UntaxedInterestIncome")
          Json.toJson(IncapacityBenefit) mustBe JsString("IncapacityBenefit")
          Json.toJson(JobSeekersAllowance) mustBe JsString("JobSeekersAllowance")
          Json.toJson(EmploymentAndSupportAllowance) mustBe JsString("EmploymentAndSupportAllowance")
        }
      }
    }
  }

}
