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

  val SERVICE_IDENTIFIER = "TES"

  val incomeTaxPage = 1
  val incomePage = 2
  val taxFreeAmountPage = 3
  val taxCodePage = 4
  val maxCompareTaxCodes = 4
  val maxTaxCodes = 4
  val maxVisibleTaxCodes = 3
  val nextTaxYearDetails = 1
  val currentTaxYearDetails = 2
  val lastTaxYearDetails = 3
  val claimATaxRefund = 4

  val notApplicable = "Not applicable"

  val IABD_TYPE_UKDIVIDENDS = 76
  val DividendZeroBandType = "SDR"

  val MCI_GATEKEEPER_TYPE = 6
  val MCI_GATEKEEPER_ID = 6
  val MCI_GATEKEEPER_DESCR = "Manual Correspondence Indicator"

  val defaultPrimaryPay = 15000
  val defaultSecondaryPay = 5000

  val TOTAL_TAX_DELTA = "TotalTaxDelta"
  val TAXABLE_PAY_DELTA = "TaxablePayDelta"
  val EMPEE_CONTRIBNS_DELTA = "EmpeeContribnsDelta"
  val EYU_DATE_FORMAT = "dd/MM/yyyy"
  val TAX_DATE_WORD_MONTH_FORMAT = "d MMMM yyyy"
  val MONTH_AND_YEAR = "MMMM yyyy"

  val CEASED_MINUS_ONE = "CY-1"
  val CEASED_MINUS_TWO = "CY-2"
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
  val NinoWithoutSuffixLength = NinoLength - 1
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

  val encodedMinusSign = "\u2212"

  val CurrentTaxYear = "currentTaxYear"
  val CurrentTaxYearPlusOne = "currentTaxYearPlusOne"

  val HigherRateBandIncome = 150000

  val updateIncomeConfirmedAmountKey = "updateIncomeConfirmedAmountKey"
}
