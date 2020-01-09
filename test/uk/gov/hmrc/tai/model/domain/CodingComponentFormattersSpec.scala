/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model.domain

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.formatters.CodingComponentFormatters
import uk.gov.hmrc.tai.model.domain.income._

class CodingComponentFormattersSpec extends PlaySpec with CodingComponentFormatters {

  "codingComponentReads" must {
    "read a coding component correctly" when {
      "all fields are provided and iabd type is an allowance" in {
        val json = Json.obj(
          "componentType" -> "EmployerProvidedServices",
          "employmentId"  -> 12,
          "amount"        -> 12345,
          "description"   -> "Some Description",
          "iabdCategory"  -> "Benefit",
          "inputAmount"   -> BigDecimal("125000.02")
        )
        json.as[CodingComponent](codingComponentReads) mustBe CodingComponent(
          componentType = EmployerProvidedServices,
          employmentId = Some(12),
          amount = 12345,
          description = "Some Description",
          inputAmount = Some(BigDecimal("125000.02")))
      }

      "only mandatory fields are provided and iabd type is an benefit" in {
        val json = Json.obj(
          "componentType" -> "EmployerProvidedServices",
          "amount"        -> 12345,
          "description"   -> "",
          "iabdCategory"  -> "Benefit")
        json.as[CodingComponent](codingComponentReads) mustBe CodingComponent(
          componentType = EmployerProvidedServices,
          employmentId = None,
          amount = 12345,
          description = "")
      }
    }
  }

  "taxComponentTypeReads" must {
    "read tax component type correctly" when {
      "given a valid IabdType" in {
        JsString("GiftAidPayments").as[TaxComponentType](taxComponentTypeReads) mustBe GiftAidPayments
        JsString("PersonalPensionPayments").as[TaxComponentType](taxComponentTypeReads) mustBe PersonalPensionPayments
        JsString("MaintenancePayments").as[TaxComponentType](taxComponentTypeReads) mustBe MaintenancePayments
        JsString("EmployerProvidedServices").as[TaxComponentType](taxComponentTypeReads) mustBe EmployerProvidedServices
        JsString("BalancingCharge").as[TaxComponentType](taxComponentTypeReads) mustBe BalancingCharge
        JsString("LoanInterestAmount").as[TaxComponentType](taxComponentTypeReads) mustBe LoanInterestAmount
        JsString("BlindPersonsAllowance").as[TaxComponentType](taxComponentTypeReads) mustBe BlindPersonsAllowance
        JsString("BpaReceivedFromSpouseOrCivilPartner")
          .as[TaxComponentType](taxComponentTypeReads) mustBe BpaReceivedFromSpouseOrCivilPartner
        JsString("CommunityInvestmentTaxCredit")
          .as[TaxComponentType](taxComponentTypeReads) mustBe CommunityInvestmentTaxCredit
        JsString("GiftsSharesCharity").as[TaxComponentType](taxComponentTypeReads) mustBe GiftsSharesCharity
        JsString("RetirementAnnuityPayments")
          .as[TaxComponentType](taxComponentTypeReads) mustBe RetirementAnnuityPayments
        JsString("NonCodedIncome").as[TaxComponentType](taxComponentTypeReads) mustBe NonCodedIncome
        JsString("Commission").as[TaxComponentType](taxComponentTypeReads) mustBe Commission
        JsString("OtherIncomeEarned").as[TaxComponentType](taxComponentTypeReads) mustBe OtherIncomeEarned
        JsString("OtherIncomeNotEarned").as[TaxComponentType](taxComponentTypeReads) mustBe OtherIncomeNotEarned
        JsString("PartTimeEarnings").as[TaxComponentType](taxComponentTypeReads) mustBe PartTimeEarnings
        JsString("Tips").as[TaxComponentType](taxComponentTypeReads) mustBe Tips
        JsString("OtherEarnings").as[TaxComponentType](taxComponentTypeReads) mustBe OtherEarnings
        JsString("CasualEarnings").as[TaxComponentType](taxComponentTypeReads) mustBe CasualEarnings
        JsString("BenefitInKind").as[TaxComponentType](taxComponentTypeReads) mustBe BenefitInKind
        JsString("CarFuelBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe CarFuelBenefit
        JsString("MedicalInsurance").as[TaxComponentType](taxComponentTypeReads) mustBe MedicalInsurance
        JsString("CarBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe CarBenefit
        JsString("Telephone").as[TaxComponentType](taxComponentTypeReads) mustBe Telephone
        JsString("ServiceBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe ServiceBenefit
        JsString("TaxableExpensesBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe TaxableExpensesBenefit
        JsString("VanBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe VanBenefit
        JsString("VanFuelBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe VanFuelBenefit
        JsString("BeneficialLoan").as[TaxComponentType](taxComponentTypeReads) mustBe BeneficialLoan
        JsString("Accommodation").as[TaxComponentType](taxComponentTypeReads) mustBe Accommodation
        JsString("Assets").as[TaxComponentType](taxComponentTypeReads) mustBe Assets
        JsString("AssetTransfer").as[TaxComponentType](taxComponentTypeReads) mustBe AssetTransfer
        JsString("EducationalServices").as[TaxComponentType](taxComponentTypeReads) mustBe EducationalServices
        JsString("Entertaining").as[TaxComponentType](taxComponentTypeReads) mustBe Entertaining
        JsString("Expenses").as[TaxComponentType](taxComponentTypeReads) mustBe Expenses
        JsString("Mileage").as[TaxComponentType](taxComponentTypeReads) mustBe Mileage
        JsString("NonQualifyingRelocationExpenses")
          .as[TaxComponentType](taxComponentTypeReads) mustBe NonQualifyingRelocationExpenses
        JsString("NurseryPlaces").as[TaxComponentType](taxComponentTypeReads) mustBe NurseryPlaces
        JsString("OtherItems").as[TaxComponentType](taxComponentTypeReads) mustBe OtherItems
        JsString("PaymentsOnEmployeesBehalf")
          .as[TaxComponentType](taxComponentTypeReads) mustBe PaymentsOnEmployeesBehalf
        JsString("PersonalIncidentalExpenses")
          .as[TaxComponentType](taxComponentTypeReads) mustBe PersonalIncidentalExpenses
        JsString("QualifyingRelocationExpenses")
          .as[TaxComponentType](taxComponentTypeReads) mustBe QualifyingRelocationExpenses
        JsString("EmployerProvidedProfessionalSubscription")
          .as[TaxComponentType](taxComponentTypeReads) mustBe EmployerProvidedProfessionalSubscription
        JsString("IncomeTaxPaidButNotDeductedFromDirectorsRemuneration")
          .as[TaxComponentType](taxComponentTypeReads) mustBe IncomeTaxPaidButNotDeductedFromDirectorsRemuneration
        JsString("TravelAndSubsistence").as[TaxComponentType](taxComponentTypeReads) mustBe TravelAndSubsistence
        JsString("VouchersAndCreditCards").as[TaxComponentType](taxComponentTypeReads) mustBe VouchersAndCreditCards
        JsString("JobExpenses").as[TaxComponentType](taxComponentTypeReads) mustBe JobExpenses
        JsString("FlatRateJobExpenses").as[TaxComponentType](taxComponentTypeReads) mustBe FlatRateJobExpenses
        JsString("ProfessionalSubscriptions")
          .as[TaxComponentType](taxComponentTypeReads) mustBe ProfessionalSubscriptions
        JsString("HotelAndMealExpenses").as[TaxComponentType](taxComponentTypeReads) mustBe HotelAndMealExpenses
        JsString("OtherExpenses").as[TaxComponentType](taxComponentTypeReads) mustBe OtherExpenses
        JsString("VehicleExpenses").as[TaxComponentType](taxComponentTypeReads) mustBe VehicleExpenses
        JsString("MileageAllowanceRelief").as[TaxComponentType](taxComponentTypeReads) mustBe MileageAllowanceRelief
        JsString("ForeignDividendIncome").as[TaxComponentType](taxComponentTypeReads) mustBe ForeignDividendIncome
        JsString("ForeignPropertyIncome").as[TaxComponentType](taxComponentTypeReads) mustBe ForeignPropertyIncome
        JsString("ForeignInterestAndOtherSavings")
          .as[TaxComponentType](taxComponentTypeReads) mustBe ForeignInterestAndOtherSavings
        JsString("ForeignPensionsAndOtherIncome")
          .as[TaxComponentType](taxComponentTypeReads) mustBe ForeignPensionsAndOtherIncome
        JsString("StatePension").as[TaxComponentType](taxComponentTypeReads) mustBe StatePension
        JsString("OccupationalPension").as[TaxComponentType](taxComponentTypeReads) mustBe OccupationalPension
        JsString("PublicServicesPension").as[TaxComponentType](taxComponentTypeReads) mustBe PublicServicesPension
        JsString("ForcesPension").as[TaxComponentType](taxComponentTypeReads) mustBe ForcesPension
        JsString("PersonalPensionAnnuity").as[TaxComponentType](taxComponentTypeReads) mustBe PersonalPensionAnnuity
        JsString("Profit").as[TaxComponentType](taxComponentTypeReads) mustBe Profit
        JsString("BankOrBuildingSocietyInterest")
          .as[TaxComponentType](taxComponentTypeReads) mustBe BankOrBuildingSocietyInterest
        JsString("UkDividend").as[TaxComponentType](taxComponentTypeReads) mustBe UkDividend
        JsString("UnitTrust").as[TaxComponentType](taxComponentTypeReads) mustBe UnitTrust
        JsString("StockDividend").as[TaxComponentType](taxComponentTypeReads) mustBe StockDividend
        JsString("NationalSavings").as[TaxComponentType](taxComponentTypeReads) mustBe NationalSavings
        JsString("SavingsBond").as[TaxComponentType](taxComponentTypeReads) mustBe SavingsBond
        JsString("PurchasedLifeAnnuities").as[TaxComponentType](taxComponentTypeReads) mustBe PurchasedLifeAnnuities
        JsString("UntaxedInterestIncome").as[TaxComponentType](taxComponentTypeReads) mustBe UntaxedInterestIncome
        JsString("IncapacityBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe IncapacityBenefit
        JsString("JobSeekersAllowance").as[TaxComponentType](taxComponentTypeReads) mustBe JobSeekersAllowance
        JsString("VentureCapitalTrust").as[TaxComponentType](taxComponentTypeReads) mustBe VentureCapitalTrust
        JsString("GiftAidAdjustment").as[TaxComponentType](taxComponentTypeReads) mustBe GiftAidAdjustment
        JsString("MarriedCouplesAllowanceToWifeMAW")
          .as[TaxComponentType](taxComponentTypeReads) mustBe MarriedCouplesAllowanceToWifeMAW
        JsString("DoubleTaxationRelief").as[TaxComponentType](taxComponentTypeReads) mustBe DoubleTaxationRelief
        JsString("ConcessionRelief").as[TaxComponentType](taxComponentTypeReads) mustBe ConcessionRelief
        JsString("EnterpriseInvestmentScheme")
          .as[TaxComponentType](taxComponentTypeReads) mustBe EnterpriseInvestmentScheme
        JsString("EarlyYearsAdjustment").as[TaxComponentType](taxComponentTypeReads) mustBe EarlyYearsAdjustment
        JsString("LossRelief").as[TaxComponentType](taxComponentTypeReads) mustBe LossRelief
        JsString("ForeignPensionAllowance").as[TaxComponentType](taxComponentTypeReads) mustBe ForeignPensionAllowance
        JsString("MarriedCouplesAllowanceMAE")
          .as[TaxComponentType](taxComponentTypeReads) mustBe MarriedCouplesAllowanceMAE
        JsString("MarriedCouplesAllowanceMCCP")
          .as[TaxComponentType](taxComponentTypeReads) mustBe MarriedCouplesAllowanceMCCP
        JsString("SurplusMarriedCouplesAllowanceToWifeWAE")
          .as[TaxComponentType](taxComponentTypeReads) mustBe SurplusMarriedCouplesAllowanceToWifeWAE
        JsString("MarriedCouplesAllowanceToWifeWMA")
          .as[TaxComponentType](taxComponentTypeReads) mustBe MarriedCouplesAllowanceToWifeWMA
        JsString("NonCashBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe NonCashBenefit
        JsString("PersonalAllowancePA").as[TaxComponentType](taxComponentTypeReads) mustBe PersonalAllowancePA
        JsString("PersonalAllowanceAgedPAA").as[TaxComponentType](taxComponentTypeReads) mustBe PersonalAllowanceAgedPAA
        JsString("PersonalAllowanceElderlyPAE")
          .as[TaxComponentType](taxComponentTypeReads) mustBe PersonalAllowanceElderlyPAE
        JsString("EmploymentAndSupportAllowance")
          .as[TaxComponentType](taxComponentTypeReads) mustBe EmploymentAndSupportAllowance
        JsString("ChildBenefit").as[TaxComponentType](taxComponentTypeReads) mustBe ChildBenefit
        JsString("MarriageAllowanceTransferred")
          .as[TaxComponentType](taxComponentTypeReads) mustBe MarriageAllowanceTransferred
        JsString("MarriageAllowanceReceived")
          .as[TaxComponentType](taxComponentTypeReads) mustBe MarriageAllowanceReceived
        JsString("DividendTax").as[TaxComponentType](taxComponentTypeReads) mustBe DividendTax

        JsString("UnderPaymentFromPreviousYear")
          .as[TaxComponentType](taxComponentTypeReads) mustBe UnderPaymentFromPreviousYear
        JsString("OutstandingDebt").as[TaxComponentType](taxComponentTypeReads) mustBe OutstandingDebt
        JsString("EstimatedTaxYouOweThisYear")
          .as[TaxComponentType](taxComponentTypeReads) mustBe EstimatedTaxYouOweThisYear

        JsString("EmploymentIncome").as[TaxComponentType](taxComponentTypeReads) mustBe EmploymentIncome
        JsString("PensionIncome").as[TaxComponentType](taxComponentTypeReads) mustBe PensionIncome
        JsString("JobSeekerAllowanceIncome").as[TaxComponentType](taxComponentTypeReads) mustBe JobSeekerAllowanceIncome
        JsString("OtherIncome").as[TaxComponentType](taxComponentTypeReads) mustBe OtherIncome

      }
    }
  }

  "taxCodeIncomeSourceReads" must {
    "read the values correctly" when {
      "only mandatory fields are provided" in {
        val json = Json.obj(
          "componentType"  -> "EmploymentIncome",
          "amount"         -> 12345,
          "description"    -> "Some Description",
          "taxCode"        -> "1150L",
          "name"           -> "employment1",
          "basisOperation" -> "OtherBasisOperation",
          "status"         -> "Live"
        )
        json.as[TaxCodeIncome](taxCodeIncomeSourceReads) mustBe TaxCodeIncome(
          EmploymentIncome,
          None,
          12345,
          "Some Description",
          "1150L",
          "employment1",
          OtherBasisOfOperation,
          Live)
      }
      "all the fields are provided" in {
        val json = Json.obj(
          "componentType"  -> "PensionIncome",
          "employmentId"   -> 123,
          "amount"         -> 333,
          "description"    -> "Some Description 1",
          "taxCode"        -> "S1150L",
          "name"           -> "employment2",
          "basisOperation" -> "Week1Month1BasisOperation",
          "status"         -> "Ceased"
        )
        json.as[TaxCodeIncome](taxCodeIncomeSourceReads) mustBe TaxCodeIncome(
          PensionIncome,
          Some(123),
          333,
          "Some Description 1",
          "S1150L",
          "employment2",
          Week1Month1BasisOfOperation,
          Ceased)
      }
    }
  }

  "taxCodeIncomeSourceStatusReads" must {
    "read the field correctly" when {
      "json string is Live" in {
        JsString("Live").as[TaxCodeIncomeSourceStatus](taxCodeIncomeSourceStatusReads) mustBe Live
      }
      "json string is PotentiallyCeased" in {
        JsString("PotentiallyCeased")
          .as[TaxCodeIncomeSourceStatus](taxCodeIncomeSourceStatusReads) mustBe PotentiallyCeased
      }
      "json string is Ceased" in {
        JsString("Ceased").as[TaxCodeIncomeSourceStatus](taxCodeIncomeSourceStatusReads) mustBe Ceased
      }
    }
    "throw runtime exception" when {
      "provided with unrecognized status" in {
        val ex = the[IllegalArgumentException] thrownBy JsString("Some Status")
          .as[TaxCodeIncomeSourceStatus](taxCodeIncomeSourceStatusReads)
        ex.getMessage must include("Invalid TaxCodeIncomeSourceStatus type")
      }
    }
  }

}
