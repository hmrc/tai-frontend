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

package uk.gov.hmrc.tai.util.constants

object TaiConstants {

  val NotApplicable = "Not applicable"
  val EyuDateFormat = "dd/MM/yyyy"
  val TaxDateWordMonthFormat = "d MMMM yyyy"
  val MonthAndYear = "MMMM yyyy"

  val AuthProviderGG = "GovernmentGateway"
  val AuthProvider = "AuthProvider"
  val Origin = "origin"
  val ConfidenceLevel = "confidenceLevel"
  val CompletionUrl = "completionURL"
  val FailureUrl = "failureURL"

  val TaxAmountFactor = 10
  val EmergencyTaxCode = "X"

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
