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

package controllers

import cats.data.EitherT
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.addEmployment._
import pages.addPensionProvider._
import pages.benefits._
import pages.endEmployment._
import pages.income._
import pages.updateEmployment._
import pages.updatePensionProvider._
import play.api.Application
import play.api.http.ContentTypes
import play.api.http.Status.{LOCKED, OK, SEE_OTHER}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, GET, contentAsString, defaultAwaitTimeout, redirectLocation, route, status, writeableOf_AnyContentAsEmpty}
import repository.JourneyCacheRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongoFeatureToggles.model.FeatureFlag
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import uk.gov.hmrc.sca.models.{MenuItemConfig, PtaMinMenuConfig, WrapperDataResponse}
import uk.gov.hmrc.tai.model.admin.{CyPlusOneToggle, DesignatoryDetailsCheck, IncomeTaxHistoryToggle}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.Week1Month1BasisOfOperation
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.model.{CalculatedPay, TaxYear, UserAnswers}
import uk.gov.hmrc.tai.util.constants.PayPeriodConstants.Monthly
import uk.gov.hmrc.tai.util.constants.{EditIncomeIrregularPayConstants, FormValuesConstants, TaiConstants}
import utils.JsonGenerator.{taxCodeChangeJson, taxCodeIncomesJson}
import utils.{FileHelper, IntegrationSpec}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Random

class ContentsCheckSpec extends IntegrationSpec with MockitoSugar with Matchers {
  private val fandfDelegationUrl = s"/delegation/get"
  private val mockFeatureFlagService = mock[FeatureFlagService]
  private val mockJourneyCacheRepository = mock[JourneyCacheRepository]
  private val startTaxYear = TaxYear().start.getYear

  def randomNino(): Nino = new Generator(new Random()).nextNino

  case class ExpectedData(
    title: String,
    navBarExpected: Boolean,
    httpStatus: Int = OK,
    headerTitle: String = "Check your Income Tax"
  )

  def getExpectedData(key: String): ExpectedData =
    key match {
      case "what-to-do" =>
        ExpectedData(
          "PAYE Income Tax overview - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "no-info" =>
        ExpectedData(
          "Your PAYE Income Tax - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "historic-paye-year" =>
        ExpectedData(
          s"Your taxable income for 6 April ${startTaxYear - 1} to 5 April $startTaxYear - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "income-tax-history" =>
        ExpectedData("Income tax history - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "timeout" => ExpectedData("Log In - Check your Income Tax - GOV.UK", navBarExpected = false)
      case "tax-estimate-unavailable" =>
        ExpectedData("We cannot access your details - Check your Income Tax - GOV.UK", navBarExpected = false, LOCKED)

      case "session-expired" =>
        ExpectedData("For your security, we signed you out - Check your Income Tax - GOV.UK", navBarExpected = false)
      case "add-employment-name" =>
        ExpectedData(
          "What is the name of the employer you want to add? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-employment-start-date" =>
        ExpectedData(
          "When did you start working for this employer? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-employment-first-pay" =>
        ExpectedData(
          "Have you received your first pay from H M Revenue and Customs? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-employment-six-weeks" =>
        ExpectedData(
          "We cannot add this employer yet - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-employment-payroll-number" =>
        ExpectedData(
          "Do you know your payroll number for this employer? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-employment-telephone-number" | "add-pension-telephone-number" | "end-employment-telephone-number" |
          "update-employment-telephone-number" | "update-income-details-number" | "remove-telephone-number" |
          "incorrect-pension-telephone-number" =>
        ExpectedData(
          "Can we call you if we need more information? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-employment-cya" | "add-pension-cya" | "end-employment-cya" | "update-employment-cya" |
          "update-income-cya" | "update-income-details-cya" | "remove-cya" | "incorrect-pension-cya" =>
        ExpectedData("Check your answers - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "add-employment-success" =>
        ExpectedData(
          "Your update about an employment has been received - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-pension-name" =>
        ExpectedData(
          "What is the name of the pension provider you want to add? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-pension-first-payment" =>
        ExpectedData(
          "Have you received your first pension payment from this employer? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-pension-number" =>
        ExpectedData(
          "Do you know your pension number from your this employer? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "add-pension-success" =>
        ExpectedData(
          "Your update about a pension has been received - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "remove-employment-warning" =>
        ExpectedData(
          "You have already sent an update about this employment - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "end-employment-decision" =>
        ExpectedData("Do you currently work for company name? - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "end-employment-six-weeks" =>
        ExpectedData("We cannot update your details yet - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "end-employment-irregular-payment" =>
        ExpectedData("End employment - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "end-employment-date" =>
        ExpectedData(
          "When did you finish working for this employer? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-employment-tell-us" =>
        ExpectedData(
          "What do you want to tell us about this employer? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-employment-success" =>
        ExpectedData(
          "Your update about an employment has been received - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "previous-underpayment" =>
        ExpectedData("What is a previous year underpayment? - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "underpayment-estimate" =>
        ExpectedData("Estimated tax you owe - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "tax-free-allowance" =>
        ExpectedData(
          s"Your tax-free amount for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "tax-code" =>
        ExpectedData(
          s"Your tax code for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "year-tax-codes" =>
        ExpectedData(
          s"Your last tax code for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "paye-income-tax-estimate" =>
        ExpectedData(
          "Your PAYE Income Tax estimate - Check your Income Tax - GOV.UK",
          navBarExpected = true,
          headerTitle = "Your PAYE Income Tax estimate"
        )
      case "detailed-income-tax-estimate" =>
        ExpectedData("Your detailed PAYE Income Tax estimate - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "income-tax-comparison" =>
        ExpectedData(
          "Income Tax comparison: current tax year and next tax year - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "your-income-calculation-details" =>
        ExpectedData("Taxable income from company name - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "update-income-warning" =>
        ExpectedData(
          "You have already sent a new estimated income - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-start" =>
        ExpectedData("Update your estimated income - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "update-income-estimated-pay" =>
        ExpectedData("There is an error with your calculation - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "how-to-update-income" =>
        ExpectedData(
          "How do you want to update your estimated income - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-working-hours" =>
        ExpectedData(
          "What are your working hours through the year? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "edit-income-irregular-hours" =>
        ExpectedData(
          "We cannot calculate your annual income as you have irregular working hours - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "edit-income-irregular-hours-confirm" | "update-income-check-save" =>
        ExpectedData(
          s"Confirm your estimated income for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "edit-income-irregular-hours-submit" =>
        ExpectedData(
          "Your taxable income has been updated - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-pay-period" =>
        ExpectedData(
          "How often do you get paid? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-payslip-amount" =>
        ExpectedData("Enter your gross pay for the month - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "update-income-payslip-deductions" =>
        ExpectedData(
          "Does your payslip show deductions before tax and National Insurance? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-taxable-payslip-amount" =>
        ExpectedData("Enter your taxable pay for the month - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "update-income-bonus-payments" =>
        ExpectedData(
          s"Will you get any bonus, commission or overtime between 6 April $startTaxYear and 5 April ${startTaxYear + 1}? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-bonus-overtime-amount" =>
        ExpectedData(
          s"How much do you think you will get in bonus, commission or overtime between 6 April $startTaxYear and 5 April ${startTaxYear + 1}? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-edit-taxable-pay" | "update-income-edit-pension" =>
        ExpectedData(
          s"Update your estimated income for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-same-estimated-pay" =>
        ExpectedData(
          s"Your estimated income for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-success-page" =>
        ExpectedData("Your taxable income has been updated - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "get-help" =>
        ExpectedData("Cannot pay the tax you owe this year - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "update-income-details-decision" =>
        ExpectedData(
          s"Update income details for 6 April ${startTaxYear - 1} to 5 April $startTaxYear - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-what-to-tell" =>
        ExpectedData(
          "What do you want to tell us about your income details? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-income-details-success" =>
        ExpectedData("Your update has been received - Check your Income Tax - GOV.UK", navBarExpected = true)
      case "income" =>
        ExpectedData(
          s"Your tax-free amount for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "income-tax-refresh" =>
        ExpectedData(
          s"Your PAYE Income Tax summary for 6 April $startTaxYear to 5 April ${startTaxYear + 1} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "remove-stop-date" =>
        ExpectedData(
          "When did you stop getting benefitName benefit from employmentName? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "remove-total-value-of-benefit" =>
        ExpectedData(
          "What was the total value of your benefitName benefit from employmentName? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "remove-success" =>
        ExpectedData(
          "Your update has been received - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "company-benefit-decision" =>
        ExpectedData(
          "Do you currently get Telephone benefit from company name? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "incorrect-pension-decision" =>
        ExpectedData(
          "Confirm your pension provider - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "incorrect-pension-what-to-tell" =>
        ExpectedData(
          "What do you want to tell us about your pension provider? - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "incorrect-pension-success" =>
        ExpectedData(
          "Your update about a pension has been received - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "incorrect-pension-warning" =>
        ExpectedData(
          "You have already sent an update about this pension - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "tax-code-comparison" =>
        ExpectedData(
          "Your tax code change - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "your-tax-free-amount" =>
        ExpectedData(
          "How we worked out your tax code - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "what-happens-next" =>
        ExpectedData(
          "What happens next - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "unauthorised" =>
        ExpectedData(
          "You have been signed out for your security - Check your Income Tax - GOV.UK",
          navBarExpected = false
        )
      case "update-next-income-warning" =>
        ExpectedData(
          "You have already sent a new estimated income - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-next-income-start" =>
        ExpectedData(
          "Update your estimated income from <span class=\"carry-over\">company name</span> for next tax year - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-next-income-edit" =>
        ExpectedData(
          s"Update your estimated income for 6 April ${startTaxYear + 1} to 5 April ${startTaxYear + 2} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-next-income-no-change" =>
        ExpectedData(
          s"Your estimated income for 6 April ${startTaxYear + 1} to 5 April ${startTaxYear + 2} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-next-income-confirm" =>
        ExpectedData(
          s"Confirm your estimated income for 6 April ${startTaxYear + 1} to 5 April ${startTaxYear + 2} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
      case "update-next-income-success" =>
        ExpectedData(
          s"Your updated estimated income for 6 April ${startTaxYear + 1} to 5 April ${startTaxYear + 2} - Check your Income Tax - GOV.UK",
          navBarExpected = true
        )
    }

  val urls: Map[String, ExpectedData] = Map(
    "/check-income-tax/what-do-you-want-to-do"                   -> getExpectedData("what-to-do"),
    "/check-income-tax/income-tax/no-info"                       -> getExpectedData("no-info"),
    s"/check-income-tax/historic-paye/${startTaxYear - 1}"       -> getExpectedData("historic-paye-year"),
    "/check-income-tax/income-tax-history"                       -> getExpectedData("income-tax-history"),
    "/check-income-tax/timeout"                                  -> getExpectedData("timeout"),
    "/check-income-tax/tax-estimate-unavailable"                 -> getExpectedData("tax-estimate-unavailable"),
    "/check-income-tax/session-expired"                          -> getExpectedData("session-expired"),
    "/check-income-tax/add-employment/employment-name"           -> getExpectedData("add-employment-name"),
    "/check-income-tax/add-employment/employment-start-date"     -> getExpectedData("add-employment-start-date"),
    "/check-income-tax/add-employment/employment-first-pay"      -> getExpectedData("add-employment-first-pay"),
    "/check-income-tax/add-employment/six-weeks"                 -> getExpectedData("add-employment-six-weeks"),
    "/check-income-tax/add-employment/employment-payroll-number" -> getExpectedData("add-employment-payroll-number"),
    "/check-income-tax/add-employment/telephone-number"          -> getExpectedData("add-employment-telephone-number"),
    "/check-income-tax/add-employment/check-your-answers"        -> getExpectedData("add-employment-cya"),
    "/check-income-tax/add-employment/employment-success"        -> getExpectedData("add-employment-success"),
    "/check-income-tax/add-pension-provider/name"                -> getExpectedData("add-pension-name"),
    "/check-income-tax/add-pension-provider/received-first-payment" -> getExpectedData("add-pension-first-payment"),
    "/check-income-tax/add-pension-provider/pension-number"         -> getExpectedData("add-pension-number"),
    "/check-income-tax/add-pension-provider/telephone-number"       -> getExpectedData("add-pension-telephone-number"),
    "/check-income-tax/add-pension-provider/check-your-answers"     -> getExpectedData("add-pension-cya"),
    "/check-income-tax/add-pension-provider/success"                -> getExpectedData("add-pension-success"),
    "/check-income-tax/update-remove-employment/warning"            -> getExpectedData("remove-employment-warning"),
    "/check-income-tax/update-remove-employment/decision-page"      -> getExpectedData("end-employment-decision"),
    "/check-income-tax/end-employment/six-weeks"                    -> getExpectedData("end-employment-six-weeks"),
    "/check-income-tax/end-employment/irregular-payment"  -> getExpectedData("end-employment-irregular-payment"),
    "/check-income-tax/end-employment/telephone-number"   -> getExpectedData("end-employment-telephone-number"),
    "/check-income-tax/end-employment/date"               -> getExpectedData("end-employment-date"),
    "/check-income-tax/end-employment/check-your-answers" -> getExpectedData("end-employment-cya"),
    "/check-income-tax/update-employment/what-do-you-want-to-tell-us/1" -> getExpectedData("update-employment-tell-us"),
    "/check-income-tax/update-employment/telephone-number"   -> getExpectedData("update-employment-telephone-number"),
    "/check-income-tax/update-employment/check-your-answers" -> getExpectedData("update-employment-cya"),
    "/check-income-tax/update-employment/success"            -> getExpectedData("update-employment-success"),
    "/check-income-tax/previous-underpayment"                -> getExpectedData("previous-underpayment"),
    "/check-income-tax/underpayment-estimate"                -> getExpectedData("underpayment-estimate"),
    "/check-income-tax/tax-free-allowance"                   -> getExpectedData("tax-free-allowance"),
    "/check-income-tax/tax-code/1"                           -> getExpectedData("tax-code"),
    s"/check-income-tax/tax-codes/$startTaxYear"             -> getExpectedData("year-tax-codes"),
    "/check-income-tax/paye-income-tax-estimate"             -> getExpectedData("paye-income-tax-estimate"),
    "/check-income-tax/detailed-income-tax-estimate"         -> getExpectedData("detailed-income-tax-estimate"),
    "/check-income-tax/income-tax-comparison"                -> getExpectedData("income-tax-comparison"),
    "/check-income-tax/your-income-calculation-details/1"    -> getExpectedData("your-income-calculation-details"),
    "/check-income-tax/update-income/warning/1"              -> getExpectedData("update-income-warning"),
    "/check-income-tax/update-income/check-your-answers/1"   -> getExpectedData("update-income-cya"),
    "/check-income-tax/update-income/start/1"                -> getExpectedData("update-income-start"),
    "/check-income-tax/update-income/estimated-pay/1"        -> getExpectedData("update-income-estimated-pay"),
    "/check-income-tax/update-income/how-to-update-income/1" -> getExpectedData("how-to-update-income"),
    "/check-income-tax/update-income/working-hours"          -> getExpectedData("update-income-working-hours"),
    "/check-income-tax/update-income/edit-income-irregular-hours/1" -> getExpectedData("edit-income-irregular-hours"),
    "/check-income-tax/update-income/edit-income-irregular-hours/1/confirm" -> getExpectedData(
      "edit-income-irregular-hours-confirm"
    ),
    "/check-income-tax/update-income/edit-income-irregular-hours/1/submit" -> getExpectedData(
      "edit-income-irregular-hours-submit"
    ),
    "/check-income-tax/update-income/pay-period"             -> getExpectedData("update-income-pay-period"),
    "/check-income-tax/update-income/payslip-amount"         -> getExpectedData("update-income-payslip-amount"),
    "/check-income-tax/update-income/payslip-deductions"     -> getExpectedData("update-income-payslip-deductions"),
    "/check-income-tax/update-income/taxable-payslip-amount" -> getExpectedData("update-income-taxable-payslip-amount"),
    "/check-income-tax/update-income/bonus-payments"         -> getExpectedData("update-income-bonus-payments"),
    "/check-income-tax/update-income/bonus-overtime-amount"  -> getExpectedData("update-income-bonus-overtime-amount"),
    "/check-income-tax/update-income/edit-taxable-pay/1"     -> getExpectedData("update-income-edit-taxable-pay"),
    "/check-income-tax/update-income/edit-pension/1"         -> getExpectedData("update-income-edit-pension"),
    "/check-income-tax/update-income/income/1/check-save"    -> getExpectedData("update-income-check-save"),
    "/check-income-tax/update-income/income/same-estimated-pay/1" -> getExpectedData(
      "update-income-same-estimated-pay"
    ),
    "/check-income-tax/update-income/success-page/1" -> getExpectedData("update-income-success-page"),
    "/check-income-tax/get-help"                     -> getExpectedData("get-help"),
    s"/check-income-tax/update-income-details/decision/${TaxYear().prev.year}" -> getExpectedData(
      "update-income-details-decision"
    ),
    "/check-income-tax/update-income-details/what-do-you-want-to-tell-us" -> getExpectedData(
      "update-income-what-to-tell"
    ),
    "/check-income-tax/update-income-details/telephone-number" -> getExpectedData(
      "update-income-details-number"
    ),
    "/check-income-tax/update-income-details/success" -> getExpectedData(
      "update-income-details-success"
    ),
    "/check-income-tax/income"                           -> getExpectedData("income"),
    "/check-income-tax/income-tax-refresh"               -> getExpectedData("income-tax-refresh"),
    "/check-income-tax/remove-company-benefit/stop-date" -> getExpectedData("remove-stop-date"),
    "/check-income-tax/remove-company-benefit/total-value-of-benefit" -> getExpectedData(
      "remove-total-value-of-benefit"
    ),
    "/check-income-tax/remove-company-benefit/telephone-number"   -> getExpectedData("remove-telephone-number"),
    "/check-income-tax/remove-company-benefit/check-your-answers" -> getExpectedData("remove-cya"),
    "/check-income-tax/remove-company-benefit/success"            -> getExpectedData("remove-success"),
    "/check-income-tax/company-benefit/decision"                  -> getExpectedData("company-benefit-decision"),
    "/check-income-tax/incorrect-pension/decision"                -> getExpectedData("incorrect-pension-decision"),
    "/check-income-tax/incorrect-pension/what-do-you-want-to-tell-us" -> getExpectedData(
      "incorrect-pension-what-to-tell"
    ),
    "/check-income-tax/incorrect-pension/telephone-number"   -> getExpectedData("incorrect-pension-telephone-number"),
    "/check-income-tax/incorrect-pension/check-your-answers" -> getExpectedData("incorrect-pension-cya"),
    "/check-income-tax/incorrect-pension/success"            -> getExpectedData("incorrect-pension-success"),
    "/check-income-tax/incorrect-pension/warning"            -> getExpectedData("incorrect-pension-warning"),
    "/check-income-tax/tax-code-change/tax-code-comparison"  -> getExpectedData("tax-code-comparison"),
    "/check-income-tax/tax-code-change/your-tax-free-amount" -> getExpectedData("your-tax-free-amount"),
    "/check-income-tax/tax-code-change/what-happens-next"    -> getExpectedData("what-happens-next"),
    "/check-income-tax/unauthorised"                         -> getExpectedData("unauthorised"),
    "/check-income-tax/update-income/next-year/income/1/warning"   -> getExpectedData("update-next-income-warning"),
    "/check-income-tax/update-income/next-year/income/1/start"     -> getExpectedData("update-next-income-start"),
    "/check-income-tax/update-income/next-year/income/1/edit"      -> getExpectedData("update-next-income-edit"),
    "/check-income-tax/update-income/next-year/income/1/no-change" -> getExpectedData("update-next-income-no-change"),
    "/check-income-tax/update-income/next-year/income/1/confirm"   -> getExpectedData("update-next-income-confirm"),
    "/check-income-tax/update-income/next-year/income/1/success"   -> getExpectedData("update-next-income-success")
  )

  // TODO SHOULD I RUN THESE WITH TOGGLE ON OR OFF????
  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(
      bind[FeatureFlagService].toInstance(mockFeatureFlagService),
      bind[JourneyCacheRepository].toInstance(mockJourneyCacheRepository)
    )
    .configure(
      "microservice.services.auth.port"                               -> server.port(),
      "microservice.services.pertax.port"                             -> server.port(),
      "microservice.services.fandf.port"                              -> server.port(),
      "microservice.services.cachable.session-cache.port"             -> server.port(),
      "sca-wrapper.services.single-customer-account-wrapper-data.url" -> s"http://localhost:${server.port()}",
      "microservice.services.tai.port"                                -> server.port(),
      "microservice.services.citizen-details.port"                    -> server.port()
    )
    .build()

  val uuid: String = UUID.randomUUID().toString

  def request(url: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, url)
      .withSession(SessionKeys.sessionId -> uuid, SessionKeys.authToken -> "Bearer 1")
      .withHeaders("Referer" -> "")

  val wrapperDataResponse: String = Json
    .toJson(
      WrapperDataResponse(
        Seq(
          MenuItemConfig("id", "NewLayout Item", "link", leftAligned = false, 0, None, None),
          MenuItemConfig("signout", "Sign out", "link", leftAligned = false, 0, None, None)
        ),
        PtaMinMenuConfig("MenuName", "BackName"),
        List.empty,
        List.empty
      )
    )
    .toString

  val person: Person = Person(
    generatedNino,
    "Firstname",
    "Surname",
    isDeceased = false,
    Address("", "", "", "", "")
  )
  val employments: JsObject = Json.obj("data" -> Json.obj("employments" -> Seq.empty[JsValue]))
  val taxAccountSummary: JsObject = Json.obj("data" -> Json.toJson(TaxAccountSummary(0, 0, 0, 0, 0)))

  val taxBand: TaxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
  val incomeCategories: IncomeCategory = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
  val totalTax: TotalTax = TotalTax(1000, Seq(incomeCategories), None, None, None)

  val taxCodeRecordJson =
    """[{"taxCodeId":2,"taxCode":"1100L","basisOfOperation":"Week 1 Month 1","startDate":"2023-09-14","endDate":"2024-04-05","employerName":"Asda","payrollNumber":"NPSQAR-62","pensionIndicator":false,"primary":true}]"""

  val incomeJson: JsValue = Json.obj(
    "data" -> Json.obj(
      "taxCodeIncomes" -> JsArray(),
      "nonTaxCodeIncomes" -> Json.obj(
        "otherNonTaxCodeIncomes" -> Json.arr(
          Json.obj(
            "incomeComponentType" -> "Profit",
            "amount"              -> 100,
            "description"         -> "Profit"
          )
        )
      )
    ),
    "links" -> Json.arr()
  )

  private val oneEmployment =
    """{
          "data" : {
            "name": "company name",
            "employmentStatus" : "Live",
            "payrollNumber": "123",
            "startDate": "2016-05-26",
            "endDate": "2016-05-26",
            "annualAccounts": [],
            "taxDistrictNumber": "123",
            "payeNumber": "321",
            "sequenceNumber": 2,
            "isPrimary": true,
            "hasPayrolledBenefit" : false,
            "receivingOccupationalPension": false,
            "employmentType": "EmploymentIncome"
          }
        }"""

  val startYear = 2023
  val numberOfYears: Int = Random.between(2, 10)

  def taxCodeRecord(year: Int): TaxCodeRecord = TaxCodeRecord(
    s"${year}X",
    TaxYear.apply(year).start,
    TaxYear.apply(year).end,
    Week1Month1BasisOfOperation,
    s"employer$year",
    pensionIndicator = false,
    None,
    primary = true
  )

  lazy val taxCodeChange: TaxCodeChange = {
    val previousYears = (startYear - numberOfYears until startYear).map(taxCodeRecord).toList
    val currentYears = (startYear - numberOfYears to startYear).map(taxCodeRecord).toList
    TaxCodeChange(previousYears, currentYears)
  }

  val taxCodeComparisonJson: JsObject = Json.obj(
    "data" -> Json.obj(
      "previous" -> Json.arr(
        Json.obj(
          "componentType" -> Json.toJson[TaxComponentType](CarBenefit),
          "employmentId"  -> 1,
          "amount"        -> 1,
          "description"   -> "Car Benefit",
          "iabdCategory"  -> "Benefit",
          "inputAmount"   -> 1
        )
      ),
      "current" -> Json.arr(
        Json.obj(
          "componentType" -> Json.toJson[TaxComponentType](CarBenefit),
          "employmentId"  -> 1,
          "amount"        -> 1,
          "description"   -> "Car Benefit",
          "iabdCategory"  -> "Benefit",
          "inputAmount"   -> 1
        )
      )
    )
  )

  private val userAnswers = UserAnswers("testSessionId", "testNino")
    .setOrException(UpdateIncomeIdPage, 1)
    .setOrException(UpdateIncomeNamePage, "employer 1")
    .setOrException(UpdateIncomePayslipDeductionsPage, "Yes")
    .setOrException(UpdateIncomeWorkingHoursPage, EditIncomeIrregularPayConstants.RegularHours)
    .setOrException(UpdateIncomeTypePage, TaiConstants.IncomeTypePension)
    .setOrException(UpdateIncomePayPeriodPage, Monthly)
    .setOrException(UpdateIncomeBonusPaymentsPage, "yes")
    .setOrException(UpdateIncomeTotalSalaryPage, "£1000")
    .setOrException(UpdateIncomeTaxablePayPage, "£100")
    .setOrException(UpdateIncomeOtherInDaysPage, "12")
    .setOrException(UpdateIncomeBonusOvertimeAmountPage, "50")
    .setOrException(UpdateIncomeNewAmountPage, "100")
    .setOrException(UpdateIncomeConfirmedNewAmountPage(1), "150")
    .setOrException(UpdateIncomeIrregularAnnualPayPage, "123")
    .setOrException(UpdateIncomePayToDatePage, "1000")
    .setOrException(UpdatedIncomeDatePage, LocalDate.of(2017, 2, 1).toString)
    .setOrException(UpdateNextYearsIncomeNewAmountPage(1), "2000")
    .setOrException(UpdatePreviousYearsIncomeTaxYearPage, "2021")
    .setOrException(UpdatePreviousYearsIncomePage, "whatYouToldUs")
    .setOrException(UpdatePreviousYearsIncomeTelephoneQuestionPage, FormValuesConstants.YesValue)
    .setOrException(UpdatePreviousYearsIncomeTelephoneNumberPage, "123456789")
    .setOrException(EndEmploymentIdPage, 1)
    .setOrException(EmploymentDecisionPage, "company name")
    .setOrException(EndEmploymentLatestPaymentPage, LocalDate.of(2022, 2, 2))
    .setOrException(EndEmploymentEndDatePage, LocalDate.of(2022, 2, 2))
    .setOrException(EndEmploymentTelephoneQuestionPage, "999")
    .setOrException(EndEmploymentTelephoneNumberPage, "999")
    .setOrException(AddEmploymentNamePage, "H M Revenue and Customs")
    .setOrException(AddEmploymentPayrollNumberPage, "1234")
    .setOrException(AddEmploymentPayrollQuestionPage, "I don't know")
    .setOrException(AddEmploymentPayrollNumberPage, "")
    .setOrException(AddEmploymentReceivedFirstPayPage, "Yes")
    .setOrException(AddEmploymentStartDatePage, LocalDate.of(2022, 7, 10))
    .setOrException(AddEmploymentStartDateWithinSixWeeksPage, "Yes")
    .setOrException(AddEmploymentTelephoneQuestionPage, "No")
    .setOrException(UpdateEmploymentIdPage, 1)
    .setOrException(UpdateEmploymentNamePage, "H M Revenue and Customs")
    .setOrException(UpdateEmploymentTelephoneQuestionPage, "No")
    .setOrException(UpdateEmploymentDetailsPage, "Details")
    .setOrException(EndCompanyBenefitsIdPage, 1)
    .setOrException(EndCompanyBenefitsNamePage, "benefitName")
    .setOrException(EndCompanyBenefitsTypePage, "Telephone")
    .setOrException(EndCompanyBenefitsRefererPage, "referer")
    .setOrException(EndCompanyBenefitsValuePage, "1234")
    .setOrException(EndCompanyBenefitsStopDatePage, LocalDate.of(2022, 2, 2).toString)
    .setOrException(EndCompanyBenefitsTelephoneNumberPage, "999")
    .setOrException(EndCompanyBenefitsTelephoneQuestionPage, "999")
    .setOrException(EndCompanyBenefitsEmploymentNamePage, "employmentName")
    .setOrException(AddPensionProviderNamePage, "Pension Provider")
    .setOrException(AddPensionProviderStartDatePage, "2017-06-09")
    .setOrException(AddPensionProviderPayrollNumberPage, "pension-ref-1234")
    .setOrException(AddPensionProviderTelephoneQuestionPage, "Yes")
    .setOrException(AddPensionProviderTelephoneNumberPage, "123456789")
    .setOrException(UpdatePensionProviderIdPage, 1)
    .setOrException(UpdatePensionProviderNamePage, "Pension 1")
    .setOrException(UpdatePensionProviderReceivePensionPage, "Yes")
    .setOrException(UpdatePensionProviderDetailsPage, "some random info")
    .setOrException(UpdatePensionProviderPhoneQuestionPage, "Yes")
    .setOrException(UpdatePensionProviderPhoneNumberPage, "123456789")

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(mockFeatureFlagService.get(CyPlusOneToggle))
      .thenReturn(Future.successful(FeatureFlag(CyPlusOneToggle, isEnabled = true)))
    when(mockFeatureFlagService.get(IncomeTaxHistoryToggle))
      .thenReturn(Future.successful(FeatureFlag(IncomeTaxHistoryToggle, isEnabled = true)))
    when(mockFeatureFlagService.get(DesignatoryDetailsCheck))
      .thenReturn(Future.successful(FeatureFlag(DesignatoryDetailsCheck, isEnabled = true)))
    when(mockFeatureFlagService.getAsEitherT(CyPlusOneToggle))
      .thenReturn(EitherT.rightT(FeatureFlag(CyPlusOneToggle, isEnabled = true)))
    when(mockFeatureFlagService.getAsEitherT(IncomeTaxHistoryToggle))
      .thenReturn(EitherT.rightT(FeatureFlag(IncomeTaxHistoryToggle, isEnabled = true)))
    when(mockFeatureFlagService.getAsEitherT(DesignatoryDetailsCheck))
      .thenReturn(EitherT.rightT(FeatureFlag(DesignatoryDetailsCheck, isEnabled = true)))
    when(mockJourneyCacheRepository.get(any(), any())).thenReturn(Future.successful(Some(userAnswers)))
    when(mockJourneyCacheRepository.set(any())).thenReturn(Future.successful(true))
    when(mockJourneyCacheRepository.clear(any(), any())).thenReturn(Future.successful(true))

    server.stubFor(
      get(urlEqualTo(s"/citizen-details/$generatedNino/designatory-details"))
        .willReturn(ok(FileHelper.loadFile("./it/resources/personDetails.json")))
    )

    server.stubFor(
      get(urlEqualTo(s"/tai/$generatedNino/tax-account/${startTaxYear + 1}/summary"))
        .willReturn(ok(Json.toJson(taxAccountSummary).toString))
    )

    server.stubFor(
      get(urlEqualTo(s"/tai/$generatedNino/tax-account/tax-code-change/exists"))
        .willReturn(ok("false"))
    )

    server.stubFor(
      get(urlEqualTo(fandfDelegationUrl))
        .willReturn(notFound())
    )

    for (year <- startTaxYear - 5 to startTaxYear + 1) {
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/employments/years/$year"))
          .willReturn(ok(Json.toJson(employments).toString))
      )
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/$year/summary"))
          .willReturn(ok(Json.toJson(taxAccountSummary).toString))
      )
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/$year/income/tax-code-incomes"))
          .willReturn(
            ok(taxCodeIncomesJson)
          )
      )
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/$year/total-tax"))
          .willReturn(ok(s"""{"data": ${Json.toJson(totalTax).toString()}}"""))
      )

    }

    server.stubFor(
      get(urlEqualTo(s"/tai/$generatedNino/employments/1"))
        .willReturn(ok(oneEmployment))
    )
    case class stubValuesData(journeyName: String, keyName: String, valueReturned: String)

    val nameValueUrls = List(
      stubValuesData("add-employment", "employmentName", "H M Revenue and Customs"),
      stubValuesData("add-pension-provider", "pensionProviderName", "H M Revenue and Customs"),
      stubValuesData("end-employment", "employmentId", "1"),
      stubValuesData("update-employment", "employmentId", "1"),
      stubValuesData("update-employment", "employmentDetails", "Details"),
      stubValuesData("update-income", "updateIncomeHowToUpdate", "1"),
      stubValuesData("update-income", "updateIncomeWorkingHours", EditIncomeIrregularPayConstants.RegularHours),
      stubValuesData("update-income", "updateIncomePayslipDeductionsKey", "1"),
      stubValuesData("update-income", "updateIncomeBonusPaymentsKey", "4000"),
      stubValuesData("update-income", "updateIncomeTaxablePayKey", "4000"),
      stubValuesData("update-income", "updateIncomeBonusOvertimeAmountKey", "4000"),
      stubValuesData("update-income", "updateIncomeEmploymentIdKey", "1"),
      stubValuesData("update-income", "updateIncomeConfirmedAmountKey-1", "1000"),
      stubValuesData("update-income", "updateIncomeNewAmountKey", "1000"),
      stubValuesData("update-previous-years-income", "incomeDetails", "Details"),
      stubValuesData("update-previous-years-income", "updateIncomeTelephoneContactAllowed", "No"),
      stubValuesData("update-previous-years-income", "updateIncomeTelephoneNumber", ""),
      stubValuesData("end-company-benefit", "benefitType", "Telephone"),
      stubValuesData("end-company-benefit", "Telephone%20decisionChoice", "No"),
      stubValuesData("update-pension-provider", "pensionProviderId", "1"),
      stubValuesData("update-next-years-income", "update-next-years-new-amount-1", "1")
    )
    nameValueUrls.foreach { stubData =>
      server.stubFor(
        get(urlEqualTo(s"/tai/journey-cache/${stubData.journeyName}/values/${stubData.keyName}"))
          .willReturn(
            ok(Json.toJson(stubData.valueReturned).toString())
          )
      )
    }
    server.stubFor(
      post(s"/tai/journey-cache/update-employment")
        .willReturn(ok("""{
                         |"employmentName":"H M Revenue and Customs",
                         |"employmentDetails":"",
                         |"employmentId":"1",
                         |"employmentTelephoneContactAllowed":"No",
                         |"employmentTelephoneNumber":""}""".stripMargin))
    )
    server.stubFor(
      post(s"/tai/journey-cache/update-previous-years-income")
        .willReturn(ok(s"""{
                          |"taxYear":"${TaxYear().prev.year}",
                          |"employmentDetails":"",
                          |"employmentId":"1",
                          |"employmentTelephoneContactAllowed":"No",
                          |"employmentTelephoneNumber":""}""".stripMargin))
    )

    server.stubFor(
      get(s"/tai/journey-cache/update-income")
        .willReturn(ok(s"""{
                          |"updateIncomeEmploymentName":"H M Revenue and Customs",
                          |"updateIncomeEmploymentIdKey":"1",
                          |"updateIncomeConfirmedAmountKey":"1000",
                          |"updateIncomeIncomeTypeKey":"0",
                          |"updateIncomeConfirmedAmountKey-1":"100",
                          |"updateIncomePayPeriodKey":"monthly",
                          |"updateIncomeTotalSalaryKey":"1000",
                          |"updateIncomePayslipDeductionsKey":"key",
                          |"updateIncomeBonusPaymentsKey":"200",
                          |"updateIncomeIrregularAnnualPayKey":"50000",
                          |"updateIncomePayToDateKey":"60000",
                          |"updateIncomeNewAmountKey":"5000"}""".stripMargin))
    )

    server.stubFor(
      post(s"/tai/journey-cache/update-income")
        .willReturn(ok(s"""{
                          |"updateIncomeEmploymentName":"H M Revenue and Customs",
                          |"updateIncomeEmploymentIdKey":"1",
                          |"updateIncomeConfirmedAmountKey":"1000",
                          |"updateIncomeIncomeTypeKey":"0",
                          |"updateIncomeConfirmedAmountKey-1":"100",
                          |"updateIncomePayPeriodKey":"monthly",
                          |"updateIncomeTotalSalaryKey":"1000",
                          |"updateIncomePayslipDeductionsKey":"key",
                          |"updateIncomeBonusPaymentsKey":"200",
                          |"updateIncomeIrregularAnnualPayKey":"50000",
                          |"updateIncomePayToDateKey":"60000"}""".stripMargin))
    )

    server.stubFor(
      get(s"/tai/journey-cache/update-pension-provider")
        .willReturn(ok(s"""{
                          |"pensionProviderName":"pensionProviderName",
                          |"pensionProviderId":"1",
                          |"receivePension":"Yes",
                          |"telephoneContactAllowed":"No",
                          |"telephoneNumber":"",
                          |"pensionDetails":"pensionDetails"}""".stripMargin))
    )

    server.stubFor(
      delete(s"/tai/journey-cache/update-income")
        .willReturn(ok)
    )

    server.stubFor(
      get(urlEqualTo("/tai/journey-cache/add-employment")).willReturn(
        ok(
          """{
            |"employmentName":"H M Revenue and Customs",
            |"employmentStartDate":"2022-07-10",
            |"employmentStartDateWithinSixWeeks":"No",
            |"employmentFirstPayReceived":"2022-08-10",
            |"employmentPayrollNumberKnown":"No",
            |"employmentPayrollNumber":"I do not know",
            |"employmentTelephoneContactAllowed":"No",
            |"employmentTelephoneNumber":""}""".stripMargin
        )
      )
    )

    server.stubFor(
      get(urlEqualTo("/tai/journey-cache/add-pension-provider")).willReturn(
        ok(
          """{
            |"pensionProviderName":"H M Revenue and Customs",
            |"pensionProviderStartDate":"2022-07-10",
            |"pensionProviderStartDateWithinSixWeeks":"No",
            |"pensionFirstPayment":"2022-08-10",
            |"pensionProviderPayrollChoice":"No",
            |"pensionProviderPayrollNumber":"I do not know",
            |"pensionProviderTelephoneContactAllowed":"No",
            |"pensionProviderTelephoneNumber":""}""".stripMargin
        )
      )
    )

    server.stubFor(
      get(urlEqualTo("/tai/journey-cache/end-employment")).willReturn(
        ok(
          """{
            |"employmentName":"H M Revenue and Customs",
            |"employmentEndDate":"2022-07-10",
            |"employmentLatestPaymentDate":"2022-08-10",
            |"employmentTelephoneQuestion":"No",
            |"employmentTelephoneNumber":"",
            |"employmentId":"1",
            |"employmentDecision":"No"}""".stripMargin
        )
      )
    )

    server.stubFor(
      get(urlEqualTo("/tai/journey-cache/update-employment")).willReturn(
        ok(
          """{
            |"employmentId":"1",
            |"employmentName":"H M Revenue and Customs",
            |"employmentDetails":"Details",
            |"employmentTelephoneContactAllowed":"No",
            |"employmentTelephoneNumber":""}""".stripMargin
        )
      )
    )

    server.stubFor(
      get(urlEqualTo("/tai/journey-cache/successful-journey")).willReturn(
        ok(
          """{
            |"update-next-years-successful":"Yes",
            |"employmentStartDate":"2022-07-10",
            |"employmentStartDateWithinSixWeeks":"No",
            |"employmentFirstPayReceived":"2022-08-10",
            |"employmentPayrollNumberKnown":"No",
            |"employmentPayrollNumber":"I do not know",
            |"employmentTelephoneContactAllowed":"No",
            |"employmentTelephoneNumber":""}""".stripMargin
        )
      )
    )

    server.stubFor(
      get(s"/tai/journey-cache/update-previous-years-income")
        .willReturn(ok(s"""{
                          |"taxYear":"$startTaxYear",
                          |"incomeDetails":"details",
                          |"updateIncomeTelephoneContactAllowed":"No",
                          |"updateIncomeTelephoneNumber":""}""".stripMargin))
    )

    server.stubFor(
      get(s"/tai/journey-cache/end-company-benefit")
        .willReturn(ok(s"""{
                          |"employmentId":"1",
                          |"employmentName":"employmentName",
                          |"benefitType":"Telephone",
                          |"stopDate":"${LocalDate.now()}",
                          |"benefitValue":"1000",
                          |"telephoneContactAllowed":"No",
                          |"telephoneNumber":"",
                          |"benefitName":"benefitName",
                          |"referer":"referer"}""".stripMargin))
    )

    server.stubFor(
      post(s"/tai/journey-cache/end-company-benefit")
        .willReturn(ok(s"""{
                          |"employmentId":"1",
                          |"employmentName":"employmentName",
                          |"benefitType":"Telephone",
                          |"stopDate":"${LocalDate.now()}",
                          |"benefitValue":"1000",
                          |"telephoneContactAllowed":"No",
                          |"telephoneNumber":"",
                          |"benefitName":"benefitName",
                          |"referer":"referer"}""".stripMargin))
    )

    server.stubFor(
      get(s"/tai/journey-cache/update-next-years-income")
        .willReturn(ok(s"""{
                          |"update-next-years-employment-id":"1",
                          |"update-next-years-employment-name":"employmentName",
                          |"update-next-years-pension-indicator":"No",
                          |"update-next-years-current-amount":"1000",
                          |"update-next-years-new-amount":"1000",
                          |"update-next-years-successful":"Yes"}""".stripMargin))
    )

    (startTaxYear - 2 to startTaxYear + 1).foreach { year =>
      server.stubFor(
        get(urlEqualTo(s"/tai/$generatedNino/tax-account/${year + 1}/tax-code/latest"))
          .willReturn(ok(s"""{"data":$taxCodeRecordJson}"""))
      )
    }

    server.stubFor(
      get(urlEqualTo(s"/tai/$generatedNino/tax-account/$startTaxYear/income"))
        .willReturn(ok(incomeJson.toString))
    )

    server.stubFor(
      post(urlEqualTo("/tai/calculator/calculate-estimated-pay"))
        .willReturn(ok(Json.toJson(CalculatedPay(None, None)).toString()))
    )

    server.stubFor(
      put(
        s"/tai/$generatedNino/tax-account/snapshots/$startTaxYear/incomes/tax-code-incomes/1/estimated-pay"
      )
        .withHeader(CONTENT_TYPE, matching(ContentTypes.JSON))
        .willReturn(
          ok
        )
    )

    server.stubFor(
      get(s"/tai/$generatedNino/employments/year/$startTaxYear/status/ceased")
        .willReturn(ok("""{"data": []}"""))
    )

    server.stubFor(
      get(s"/tai/$generatedNino/tax-account/year/$startTaxYear/income/EmploymentIncome/status/Live")
        .willReturn(ok("""{"data": []}"""))
    )

    server.stubFor(
      get(s"/tai/$generatedNino/employments-only/years/2020")
        .willReturn(ok("""
                         |{
                         |  "data": {
                         |    "employments": [
                         |      {
                         |        "employmentType": "EmploymentIncome",
                         |        "name": "HM Revenue & Customs Building 9 (Benton Park View)",
                         |        "annualAccounts": [],
                         |        "employmentStatus": "Live",
                         |        "receivingOccupationalPension": false,
                         |        "payrollNumber": "EMP/EMP0000001",
                         |        "payeNumber": "MA83247",
                         |        "hasPayrolledBenefit": false,
                         |        "sequenceNumber": 1,
                         |        "startDate": "2013-03-18",
                         |        "taxDistrictNumber": "120"
                         |      }
                         |    ],
                         |    "etag": 1
                         |  },
                         |  "links": []
                         |}
                         |""".stripMargin))
    )
    server.stubFor(
      get(s"/tai/$generatedNino/tax-account/year/$startTaxYear/income/EmploymentIncome/status/NotLive")
        .willReturn(ok("""{"data": []}"""))
    )
    server.stubFor(
      get(s"/tai/$generatedNino/tax-account/year/$startTaxYear/income/PensionIncome/status/Live")
        .willReturn(ok("""{"data": []}"""))
    )
    server.stubFor(
      get(s"/tai/$generatedNino/tax-account/tax-code-change")
        .willReturn(ok(taxCodeChangeJson(taxCodeChange)))
    )
    server.stubFor(
      get(s"/tai/$generatedNino/tax-account/tax-free-amount-comparison")
        .willReturn(ok(taxCodeComparisonJson.toString()))
    )
  }

  server.stubFor(
    get(urlMatching("/messages/count.*"))
      .willReturn(ok(s"""{"count": 0}"""))
  )

  "/check-income-tax/" must {
    urls.foreach { case (url, expectedData: ExpectedData) =>
      s"pass content checks at url $url" in {

        server.stubFor(
          get(urlMatching("/messages/count.*"))
            .willReturn(ok(s"""{"count": 0}"""))
        )

        server.stubFor(
          WireMock
            .get(urlMatching("/single-customer-account-wrapper-data/message-data.*"))
            .willReturn(ok(s"""0"""))
        )

        server.stubFor(
          WireMock
            .get(urlMatching("/single-customer-account-wrapper-data/wrapper-data.*"))
            .willReturn(ok(wrapperDataResponse))
        )

        val result: Future[Result] = route(app, request(url)).get
        val content = Jsoup.parse(contentAsString(result))

        if (expectedData.httpStatus != SEE_OTHER) {
          redirectLocation(result) mustBe None
        }
        status(result) mustBe expectedData.httpStatus

        content.title() mustBe expectedData.title

        val govUkBanner = content.getElementsByClass("govuk-phase-banner")
        govUkBanner.size() mustBe 1
        govUkBanner.get(0).getElementsByClass("govuk-link").get(0).attr("href") must include(
          "http://localhost:9250/contact/beta-feedback?service=TES"
        )

        val accessibilityStatement = content
          .getElementsByClass("govuk-footer__link")
          .asScala
          .toList
          .map(_.attr("href"))
          .filter(_.contains("accessibility-statement"))
          .head
        accessibilityStatement must include(
          "http://localhost:12346/accessibility-statement/check-income-tax"
        )

        if (expectedData.navBarExpected) {
          val signoutLink = content
            .getElementsByClass("hmrc-account-menu__link")
            .asScala
            .toList
            .find(_.html().contains("Sign out"))
            .get
            .attr("href")
          signoutLink mustBe "/check-income-tax/signout"
        } else {
          content
            .getElementsByClass("hmrc-account-menu__link")
            .asScala
            .toList
            .find(_.html().contains("Sign out")) mustBe None
        }

        val languageToggle = content.getElementsByClass("hmrc-language-select__list")
        languageToggle.text() must include("English")
        languageToggle.text() must include("Cymraeg")

        val reportIssueText = content.getElementsByClass("hmrc-report-technical-issue").get(0).text()
        val reportIssueLink = content.getElementsByClass("hmrc-report-technical-issue").get(0).attr("href")
        reportIssueText must include("Is this page not working properly? (opens in new tab)")
        reportIssueLink must include("/contact/report-technical-problem")

        val serviceName = content.getElementsByClass("govuk-header__service-name").get(0).text()
        serviceName mustBe expectedData.headerTitle
      }
    }
  }
}
