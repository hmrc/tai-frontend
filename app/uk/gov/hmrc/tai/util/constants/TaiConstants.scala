/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util.constants

object TaiConstants {

  val ServiceIdentifier = "TES"

  val IncomeTaxPage = 1
  val IncomePage = 2
  val TaxFreeAmountPage = 3
  val TaxCodePage = 4
  val MaxCompareTaxCodes = 4
  val MaxTaxCodes = 4
  val MaxVisibleTaxCodes = 3
  val NextTaxYearDetails = 1
  val CurrentTaxYearDetails = 2
  val LastTaxYearDetails = 3
  val ClaimATaxRefund = 4

  val NotApplicable = "Not applicable"

  val IabdTypeUKDividends = 76
  val DividendZeroBandType = "SDR"

  val MciGatekeeperType = 6
  val MciGatekeeperID = 6
  val MciGatekeeperDescr = "Manual Correspondence Indicator"

  val DefaultPrimaryPay = 15000
  val DefaultSecondaryPay = 5000

  val TotalTaxDelta = "TotalTaxDelta"
  val TaxablePayDelta = "TaxablePayDelta"
  val EmpeeContribnsDelta = "EmpeeContribnsDelta"
  val EyuDateFormat = "dd/MM/yyyy"
  val TaxDateWordMonthFormat = "d MMMM yyyy"
  val MonthAndYear = "MMMM yyyy"

  val CeasedMinusOne = "CY-1"
  val CeasedMinusTwo = "CY-2"
  val CeasedMinusThree = "CY-3"

  val EmploymentLive = 1
  val EmploymentPotentiallyCeased = 2
  val EmploymentCeased = 3

  val AuthProviderGG = "GovernmentGateway"
  val AuthProvider = "AuthProvider"

  val SessionPostLogoutPage = "postLogoutPage"

  val TaxableIncomeCurrentYearPage = 1
  val TaxableIncomeCurrentYearMinusOnePage = 2

  val Origin = "origin"
  val ConfidenceLevel = "confidenceLevel"
  val CompletionUrl = "completionURL"
  val FailureUrl = "failureURL"

  val Continue = "continue"
  val Failure = "failure"

  val NinoLength = 9
  val NinoWithoutSuffixLength: Int = NinoLength - 1
  val TaxAmountFactor = 10
  val EmergencyTaxCode = "X"

  val IncomeTypeDummy = "99"
  val IncomeTypeEmployment = "0"
  val IncomeTypePension = "1"

  val EmployeePensionIForm = "employment-pension"
  val CompanyBenefitsIform = "company-benefits"
  val CompanyCarsIform = "company-cars"
  val MedicalBenefitsIform = "medical-benefits"
  val OtherIncomeIform = "other-income"
  val InvestIncomeIform = "invest-income"
  val StateBenefitsIform = "state-benefits"
  val MarriageAllowanceService = "marriage-allowance"

  val NpsTaxAccountDeceasedMsg = "deceased"
  val NpsTaxAccountCYDataAbsentMsg = "no tax account information found"
  val NpsTaxAccountDataAbsentMsg = "cannot complete a coding calculation without a primary employment"
  val RtiPaymentDataAbsentMsg = "no data"
  val NpsNoEmploymentsRecorded = "no employments recorded for this individual"
  val NpsNoEmploymentForCurrentTaxYear = "no employments recorded for current tax year"
  val NpsAppStatusMsg = "appstatusmessage"

  val EmergencyTaxCodeSuffix = "X"
  val ScottishTaxCodePrefix = "S"

  val EncodedMinusSign = "\u2212"

  val CurrentTaxYear = "currentTaxYear"
  val CurrentTaxYearPlusOne = "currentTaxYearPlusOne"

  val HigherRateBandIncome = 150000

  val UpdateIncomeConfirmedAmountKey = "updateIncomeConfirmedAmountKey"

  val LondonEuropeTimezone = "Europe/London"
}
