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

import builders.UserBuilder
import controllers.FakeTaiPlayApplication
import controllers.auth.TaiUser
import data.TaiData
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.nps2.IabdUpdateSource
import uk.gov.hmrc.tai.model.rti.{PayFrequency, RtiPayment}
import uk.gov.hmrc.tai.model.tai.{AnnualAccount, TaxYear}
import uk.gov.hmrc.tai.model.{IncomeExplanation, TaxSummaryDetails}
import uk.gov.hmrc.tai.util.TaiConstants.{EmploymentCeased, EmploymentLive, EmploymentPotentiallyCeased}
import uk.gov.hmrc.tai.util.YourIncomeCalculationHelper
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModel

class YourIncomeCalculationPageVMSpec
  extends PlaySpec
    with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val user: TaiUser = UserBuilder.apply()
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val nino: Nino = new Generator().nextNino

  "displayPayrollNumber " must {

    "return the correct payroll message for duplicate Employments " in {
      val message = YourIncomeCalculationHelper.displayPayrollNumber(true, Some("2"), false)
      message mustBe Some(Messages("tai.income.calculation.Income.PayrollNumberEmployment.text", "2"))
    }

    "return the correct payroll message for duplicate Pension " in {
      val message = YourIncomeCalculationHelper.displayPayrollNumber(true, Some("2"), true)
      message mustBe Some(Messages("tai.income.calculation.Income.PayrollNumberPension.text", "2"))
    }

    "return no payroll message for not duplicate employments " in {
      val message = YourIncomeCalculationHelper.displayPayrollNumber(false, Some("2"), false)
      message mustBe None
    }
  }

  "hasPrevious " must {
    "return true if the employment started before current Tax Year start date " in {
      val hasPrevious = YourIncomeCalculationHelper.hasPreviousEmployment(Some(new LocalDate(2016,5,6)))
      hasPrevious mustBe true
    }
    "return false if the employment started after current Tax Year start date " in {
      val taxYear = TaxYear().year
      val hasPrevious = YourIncomeCalculationHelper.hasPreviousEmployment(Some(new LocalDate(taxYear,5,6)))
      hasPrevious mustBe false
    }
  }

  "getNotMatchingTotalMessage" must {

    "return the correct message if the total don't match for taxable Pay of pension " in {
      val rti1 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 4, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 150, taxablePayYTD = 150,taxed = 0, taxedYTD = 0, isOccupationalPension = true)
      val rti2 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 5, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 200, taxablePayYTD = 350,taxed = 0, taxedYTD = 0, isOccupationalPension = true)
      val rti3 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 6, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 300, taxablePayYTD = 150,taxed = 0, taxedYTD = 0, isOccupationalPension = true)
      val rti4 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 7, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 5000, taxablePayYTD = 10000,taxed = 0, taxedYTD = 0, isOccupationalPension = true)

      val rtiPayments = List(rti1, rti2, rti3, rti4)
      val message = YourIncomeCalculationHelper.getNotMatchingTotalMessage(rtiPayments, Some(true))

      message mustBe Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
    }

    "return the correct message if the total don't match for taxable Pay of employment " in {
      val rti1 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 4, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 150, taxablePayYTD = 150,taxed = 0, taxedYTD = 0)
      val rti2 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 5, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 200, taxablePayYTD = 350,taxed = 0, taxedYTD = 0)
      val rti3 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 6, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 300, taxablePayYTD = 150,taxed = 0, taxedYTD = 0)
      val rti4 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 7, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 5000, taxablePayYTD = 10000,taxed = 0, taxedYTD = 0)
      val rtiPayments = List(rti1, rti2, rti3, rti4)

      val message = YourIncomeCalculationHelper.getNotMatchingTotalMessage(rtiPayments, Some(false))

      message mustBe Some(Messages("tai.income.calculation.totalNotMatching.emp.message"))
    }

    "return the correct message if the total don't match for tax of pension " in {
      val rti1 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 4, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 150, taxablePayYTD = 150,taxed = 100, taxedYTD = 100, isOccupationalPension = true)
      val rti2 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 5, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 200, taxablePayYTD = 350,taxed = 500, taxedYTD = 600, isOccupationalPension = true)
      val rti3 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 6, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 300, taxablePayYTD = 150,taxed = 400, taxedYTD = 1000, isOccupationalPension = true)
      val rti4 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 7, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 5000, taxablePayYTD = 5650,taxed = 500, taxedYTD = 1550, isOccupationalPension = true)

      val rtiPayments = List(rti1, rti2, rti3, rti4)

      val message = YourIncomeCalculationHelper.getNotMatchingTotalMessage(rtiPayments, Some(true))

      message mustBe Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
    }

    "return the correct message if the total don't match for tax of employment " in {
      val rti1 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 4, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 150, taxablePayYTD = 150,taxed = 100, taxedYTD = 100)
      val rti2 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 5, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 200, taxablePayYTD = 350,taxed = 400, taxedYTD = 500)
      val rti3 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 6, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 300, taxablePayYTD = 150,taxed = 300, taxedYTD = 800)
      val rti4 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 7, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 5000, taxablePayYTD = 5650,taxed = 200, taxedYTD = 1050)
      val rtiPayments = List(rti1, rti2, rti3, rti4)

      val message = YourIncomeCalculationHelper.getNotMatchingTotalMessage(rtiPayments, Some(false))

      message mustBe Some(Messages("tai.income.calculation.totalNotMatching.emp.message"))
    }

    "return the correct message if the total don't match for nic Paid of pension " in {
      val rti1 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 4, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 150, taxablePayYTD = 150,taxed = 100, taxedYTD = 100, isOccupationalPension = true, nicPaid = Some(50), nicPaidYTD = Some(50))
      val rti2 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 5, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 200, taxablePayYTD = 350,taxed = 500, taxedYTD = 600, isOccupationalPension = true, nicPaid = Some(100), nicPaidYTD = Some(150))
      val rti3 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 6, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 300, taxablePayYTD = 150,taxed = 400, taxedYTD = 1000, isOccupationalPension = true, nicPaid = Some(50), nicPaidYTD = Some(200))
      val rti4 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 7, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 5000, taxablePayYTD = 5650,taxed = 500, taxedYTD = 1500, isOccupationalPension = true, nicPaid = Some(100), nicPaidYTD = Some(350))

      val rtiPayments = List(rti1, rti2, rti3, rti4)

      val message = YourIncomeCalculationHelper.getNotMatchingTotalMessage(rtiPayments, Some(true))

      message mustBe Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
    }

    "return the correct message if the total don't match for nic Paid of employment " in {
      val rti1 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 4, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 150, taxablePayYTD = 150,taxed = 100, taxedYTD = 100, nicPaid = Some(100), nicPaidYTD = Some(100))
      val rti2 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 5, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 200, taxablePayYTD = 350,taxed = 400, taxedYTD = 500, nicPaid = Some(200), nicPaidYTD = Some(300))
      val rti3 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 6, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 300, taxablePayYTD = 150,taxed = 300, taxedYTD = 800, nicPaid = Some(250), nicPaidYTD = Some(550))
      val rti4 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 7, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 5000, taxablePayYTD = 5650,taxed = 200, taxedYTD = 1000, nicPaid = Some(100), nicPaidYTD = Some(600))
      val rtiPayments = List(rti1, rti2, rti3, rti4)

      val message = YourIncomeCalculationHelper.getNotMatchingTotalMessage(rtiPayments, Some(false))

      message mustBe Some(Messages("tai.income.calculation.totalNotMatching.emp.message"))
    }

    "return no message if the total matches for employement/Pension " in {
      val rti1 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 4, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 150, taxablePayYTD = 150,taxed = 100, taxedYTD = 100, nicPaid = Some(100), nicPaidYTD = Some(100))
      val rti2 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 5, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 200, taxablePayYTD = 350,taxed = 400, taxedYTD = 500, nicPaid = Some(200), nicPaidYTD = Some(300))
      val rti3 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 6, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 300, taxablePayYTD = 150,taxed = 300, taxedYTD = 800, nicPaid = Some(250), nicPaidYTD = Some(550))
      val rti4 = RtiPayment(payFrequency = PayFrequency.Monthly, paidOn = new LocalDate(2016, 7, 7),submittedOn = new LocalDate(2016, 4, 7),
        taxablePay = 5000, taxablePayYTD = 5650,taxed = 200, taxedYTD = 1000, nicPaid = Some(100), nicPaidYTD = Some(650))

      val rtiPayments = List(rti1, rti2, rti3, rti4)

      val message = YourIncomeCalculationHelper.getNotMatchingTotalMessage(rtiPayments, Some(false))

      message mustBe None
    }

  }

  "getCurrentYearPayments " must {
    val taxSummary = TaiData.getCurrentYearTaxSummaryDetails

    "return current year rti payments for the selected first pension " in {
      val accounts: Seq[AnnualAccount] = taxSummary.accounts.map(account => account.copy(year = TaxYear()))
      val currentYearTaxSummary = taxSummary.copy(accounts = accounts)
      val (rtiPayments, employerName, totalNotEqualMessage) = YourIncomeCalculationHelper.getCurrentYearPayments(currentYearTaxSummary, 1)

      rtiPayments.size mustBe 2
      rtiPayments.head.paidOn mustBe new LocalDate(2016, 4, 26)
      rtiPayments.head.taxed mustBe 269.75
      rtiPayments.head.taxablePay mustBe 2135.41
      rtiPayments.head.nicPaid mustBe Some(400.0)

      rtiPayments.last.paidOn mustBe new LocalDate(2017, 2, 28)
      rtiPayments.last.taxed mustBe 269.75
      rtiPayments.last.taxedYTD mustBe 539.5
      rtiPayments.last.taxablePayYTD mustBe 100000.00
      rtiPayments.last.taxablePay mustBe 2135.41
      rtiPayments.last.nicPaid mustBe None
      rtiPayments.last.nicPaidYTD mustBe Some(1000.00)

      employerName mustBe true
      totalNotEqualMessage mustBe Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
    }

    "return current year rti payments for the selected second pension " in {
      val accounts: Seq[AnnualAccount] = taxSummary.accounts.map(account => account.copy(year = TaxYear()))
      val currentYearTaxSummary = taxSummary.copy(accounts = accounts)
      val (rtiPayments, employerName, totalNotEqualMessage) = YourIncomeCalculationHelper.getCurrentYearPayments(currentYearTaxSummary, 8)

      rtiPayments.size mustBe 2
      rtiPayments.head.paidOn mustBe new LocalDate(2016, 4, 26)
      rtiPayments.head.taxed mustBe 427.8
      rtiPayments.head.taxablePay mustBe 2135.41
      rtiPayments.head.nicPaid mustBe Some(4800.0)

      rtiPayments.last.paidOn mustBe new LocalDate(2016, 5, 31)
      rtiPayments.last.taxed mustBe 166.66
      rtiPayments.last.taxedYTD mustBe 333.33
      rtiPayments.last.taxablePayYTD mustBe 999.66
      rtiPayments.last.taxablePay mustBe 833.33
      rtiPayments.last.nicPaid mustBe Some(200.00)
      rtiPayments.last.nicPaidYTD mustBe Some(5000.00)

      employerName mustBe true
      totalNotEqualMessage mustBe Some(Messages("tai.income.calculation.totalNotMatching.pension.message"))
    }
  }

  "getIncomeExplanationMessage" must {

    val json = """
        {"employerName": "",
                 	"incomeId": 1,
                 	"hasDuplicateEmploymentNames": false,
                 	"worksNumber": "100502",
                  "paymentDate": "2017-05-05",
                 	"notificationDate": "2016-10-05",
                 	"updateActionDate": "2016-05-08",
                 	"startDate": "2007-04-06",
                 	"employmentStatus": 1,
                 	"employmentType": 1,
                 	"isPension": true,
                 	"iabdSource": 46,
                 	"payToDate": 4020,
                 	"calcAmount": 20904,
                 	"grossAmount": 22573,
                 	"payFrequency": "W1",
                 	"editableDetails": {
                 		"isEditable": true,
                 		"payRollingBiks": false
                 	}
        }"""

    val continuousParsedJson: IncomeExplanation = Json.parse(json).as[IncomeExplanation].copy(startDate = Some(TaxYear().start.minusMonths(6)))

    val midYearParsedJson: IncomeExplanation = Json.parse(json).as[IncomeExplanation].copy(startDate = Some(TaxYear().start.plusMonths(3)), paymentDate = Some(TaxYear().start.plusMonths(6)))

    "return the correct income explanation message when the rti calculation amount is same as taxable pay to date on final FPS for continuous employment/pension" in {
      val incomeExplMod = continuousParsedJson.copy(calcAmount = Some(22573), payToDate = 22573)
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)
      val taxYear = TaxYear().year

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.pension.same", new LocalDate(taxYear, 4, 6).toString("d MMMM yyyy"), new LocalDate(taxYear - 1, 5, 5).toString("d MMMM yyyy"), MoneyPounds(22573, 0).quantity))
      incomeExpEstimateMsg mustBe None

      val incomeExplanationEmpMod = continuousParsedJson.copy(isPension = false, calcAmount = Some(22573), payToDate = 22573)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)

      incomeExpEmpMsg mustBe Some(Messages("tai.income.calculation.rti.emp.same", new LocalDate(taxYear, 4, 6).toString("d MMMM yyyy"), new LocalDate(taxYear - 1, 5, 5).toString("d MMMM yyyy"), MoneyPounds(22573, 0).quantity))
      incomeExpEmpEstimateMsg mustBe None
    }

    "return the correct income explanation message when the rti calculation amount is same as taxable pay to date on final FPS for mid Year employment/Pension" in {
      val incomeExplMod = midYearParsedJson.copy(calcAmount = Some(22573), payToDate = 22573)
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)
      val taxYear = TaxYear().year

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.pension.same", new LocalDate(taxYear, 7, 6).toString("d MMMM yyyy"), new LocalDate(taxYear, 10, 6).toString("d MMMM yyyy"), MoneyPounds(22573, 0).quantity))
      incomeExpEstimateMsg mustBe None

      val incomeExplanationEmpMod = midYearParsedJson.copy(isPension = false, calcAmount = Some(22573), payToDate = 22573)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)

      incomeExpEmpMsg mustBe Some(Messages("tai.income.calculation.rti.emp.same", new LocalDate(taxYear, 7, 6).toString("d MMMM yyyy"), new LocalDate(taxYear, 10, 6).toString("d MMMM yyyy"), MoneyPounds(22573, 0).quantity))
      incomeExpEmpEstimateMsg mustBe None
    }

    "return the correct income explanation message for continuous live employment/pension for Weekly(W1)/ Fortnightly (W2)/ Four weekly (W4)/ Monthly (M1)/ Quarterly (M3)/ Bi-annually (M6) pay Frequency " in {

      val payFreqList = List(Some(PayFrequency.Weekly), Some(PayFrequency.Fortnightly), Some(PayFrequency.FourWeekly), Some(PayFrequency.Monthly),
        Some(PayFrequency.Quarterly), Some(PayFrequency.BiAnnually))

      for (payFrequency <- payFreqList) {
        val incomeExplPensionMod = continuousParsedJson.copy(payFrequency = payFrequency)
        val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplPensionMod)

        incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.continuous.weekly.pension", MoneyPounds(4020, 2).quantity, new LocalDate(2017, 5, 5).toString("d MMMM yyyy")))
        incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.pension.estimate", MoneyPounds(20904, 0).quantity))

        val incomeExplanationEmpMod = continuousParsedJson.copy(payFrequency = payFrequency, isPension = false)
        val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)

        incomeExpEmpMsg mustBe Some(Messages("tai.income.calculation.rti.continuous.weekly.emp", MoneyPounds(4020, 2).quantity, new LocalDate(2017, 5, 5).toString("d MMMM yyyy")))
        incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.emp.estimate", MoneyPounds(20904, 0).quantity))
      }
    }

    "return the correct income explanation message for continuous live employment/pension for Annually (MA) pay Frequency " in {
      val incomeExplMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Annually))

      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.continuous.annually.pension", MoneyPounds(4020, 2).quantity))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.pension.estimate", MoneyPounds(20904, 0).quantity))

      val incomeExplanationEmpMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Annually), isPension = false)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)

      incomeExpEmpMsg mustBe Some(Messages("tai.income.calculation.rti.continuous.annually.emp", MoneyPounds(4020, 2).quantity))
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.emp.estimate", MoneyPounds(20904, 0).quantity))
    }

    "return the correct income explanation message for continuous live employment/pension for One Off (IO) pay Frequency " in {
      val incomeExplMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.OneOff))

      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.oneOff.pension", MoneyPounds(4020, 2).quantity))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.pension.estimate", MoneyPounds(20904, 0).quantity))

      val incomeExplanationEmpMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.OneOff), isPension = false)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)

      incomeExpEmpMsg mustBe Some(Messages("tai.income.calculation.rti.oneOff.emp", MoneyPounds(4020, 2).quantity))
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.emp.estimate", MoneyPounds(20904, 0).quantity))
    }

    "return the correct income explanation message for continuous live employments for Irregular (IR) primary default amount (15000)" in {
      val incomeExplMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), payToDate = 13000)

      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)
      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExplanationEmpMod = continuousParsedJson.copy(isPension = false, payFrequency = Some(PayFrequency.Irregular))

      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)
      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message for continuous live employments for Irregular (IR) primary " in {
      val incomeExplMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), payToDate = 18000)

      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)
      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExplanationEmpMod = continuousParsedJson.copy(isPension = false, payFrequency = Some(PayFrequency.Irregular), payToDate = 18000)

      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)
      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message for continuous live employments for Irregular (IR) secondary default amount(5000) " in {
      val incomeExplMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), payToDate = 4000)
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExpEmpMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), isPension = false)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExpEmpMod)

      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message for continuous live employments for Irregular (IR) secondary " in {
      val incomeExplMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), payToDate = 6000)
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExpEmpMod = continuousParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), isPension = false, payToDate = 6000)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExpEmpMod)

      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message for mid year live employment/pension for Weekly(W1)/ Fortnightly (W2)/ Four weekly (W4)/ Monthly (M1)/ Quarterly (M3)/ Bi-annually (M6)/ Annually (MA) pay Frequency " in {
      val payFreqList = List(Some(PayFrequency.Weekly), Some(PayFrequency.Fortnightly), Some(PayFrequency.FourWeekly), Some(PayFrequency.Monthly), Some(PayFrequency.Quarterly), Some(PayFrequency.BiAnnually), Some(PayFrequency.Annually))

      for (payFrequency <- payFreqList) {
        val incomeExplPensionMod = midYearParsedJson.copy(payFrequency = payFrequency)
        val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplPensionMod)
        val taxYear = TaxYear().year

        incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.midYear.weekly", new LocalDate(taxYear, 7, 6).toString("d MMMM yyyy"), new LocalDate(taxYear, 10, 6).toString("d MMMM yyyy"), MoneyPounds(4020, 2).quantity))
        incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.pension.estimate", MoneyPounds(20904, 0).quantity))

        val incomeExplanationEmpMod = midYearParsedJson.copy(payFrequency = payFrequency, isPension = false)
        val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)

        incomeExpEmpMsg mustBe Some(Messages("tai.income.calculation.rti.midYear.weekly", new LocalDate(taxYear, 7, 6).toString("d MMMM yyyy"), new LocalDate(taxYear, 10, 6).toString("d MMMM yyyy"), MoneyPounds(4020, 2).quantity))
        incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.emp.estimate", MoneyPounds(20904, 0).quantity))
      }
    }

    "return the correct income explanation message for mid year live employment/pension for One Off (IO) pay Frequency " in {
      val incomeExplMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.OneOff))

      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.oneOff.pension", MoneyPounds(4020, 2).quantity))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.pension.estimate", MoneyPounds(20904, 0).quantity))

      val incomeExplanationEmpMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.OneOff), isPension = false)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)

      incomeExpEmpMsg mustBe Some(Messages("tai.income.calculation.rti.oneOff.emp", MoneyPounds(4020, 2).quantity))
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.emp.estimate", MoneyPounds(20904, 0).quantity))
    }

    "return the correct income explanation message for mid year live employments for Irregular (IR) primary default amount(15000) " in {
      val incomeExplMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), payToDate = 10000)

      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)
      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExplanationEmpMod = midYearParsedJson.copy(isPension = false, payFrequency = Some(PayFrequency.Irregular), payToDate = 10000)

      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)
      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message for mid year live employments for Irregular (IR) primary " in {
      val incomeExplMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), payToDate = 18000)

      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)
      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExplanationEmpMod = midYearParsedJson.copy(isPension = false, payFrequency = Some(PayFrequency.Irregular), payToDate = 18000)

      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplanationEmpMod)
      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message for mid year live employments for Irregular (IR) secondary  default amount(5000) " in {
      val incomeExplMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), payToDate = 4000)
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExpEmpMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), isPension = false, payToDate = 4000)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExpEmpMod)

      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message for mid year live employments for Irregular (IR) secondary " in {
      val incomeExplMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), payToDate = 6000)
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe None
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.pension", MoneyPounds(22573, 0).quantity))

      val incomeExpEmpMod = midYearParsedJson.copy(payFrequency = Some(PayFrequency.Irregular), employmentType = Some(2), isPension = false, payToDate = 6000)
      val (incomeExpEmpMsg, incomeExpEmpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExpEmpMod)

      incomeExpEmpMsg mustBe None
      incomeExpEmpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.irregular.emp", MoneyPounds(22573, 0).quantity))
    }

    "return the correct income explanation message when the agent updated the income " in {
      val incomeExplMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.AgentContact.code), grossAmount = Some(27000))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.agent"))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.agent.estimate", MoneyPounds(27000, 0).quantity))
    }

    "return the correct income explanation message when income was updated via email " in {
      val incomeExplMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.Email.code), grossAmount = Some(27000))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.manual.update.email", new LocalDate(2016, 5, 8).toString("d MMMM yyyy"), new LocalDate(2016, 10, 5).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))

      val incomeExplNoDatesMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.Email.code), grossAmount = Some(27000), notificationDate = None, updateActionDate = None)
      val (incomeExpNoDatesMsg, incomeExpNoDatesEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplNoDatesMod)

      incomeExpNoDatesMsg mustBe Some(Messages("tai.income.calculation.manual.update.email.withoutDate"))
      incomeExpNoDatesEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))
    }

    "return the correct income explanation message when income was updated via phone " in {
      val incomeExplMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.ManualTelephone.code), grossAmount = Some(27000))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.manual.update.phone", new LocalDate(2016, 5, 8).toString("d MMMM yyyy"), new LocalDate(2016, 10, 5).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))

      val incomeExplNoDatesMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.ManualTelephone.code), grossAmount = Some(27000), notificationDate = None, updateActionDate = None)
      val (incomeExpNoDatesMsg, incomeExpNoDatesEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplNoDatesMod)

      incomeExpNoDatesMsg mustBe Some(Messages("tai.income.calculation.manual.update.phone.withoutDate"))
      incomeExpNoDatesEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))
    }

    "return the correct income explanation message when income was updated via information letter " in {
      val incomeExplMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.InformationLetter.code), grossAmount = Some(27000))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.manual.update.informationLetter", new LocalDate(2016, 5, 8).toString("d MMMM yyyy"), new LocalDate(2016, 10, 5).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))

      val incomeExplNoDatesMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.InformationLetter.code), grossAmount = Some(27000), notificationDate = None, updateActionDate = None)
      val (incomeExpNoDatesMsg, incomeExpNoDatesEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplNoDatesMod)

      incomeExpNoDatesMsg mustBe Some(Messages("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      incomeExpNoDatesEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))
    }

    "return the correct income explanation message when income was updated via other forms " in {
      val incomeExplMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.OtherForm.code), grossAmount = Some(27000))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.manual.update.informationLetter", new LocalDate(2016, 5, 8).toString("d MMMM yyyy"), new LocalDate(2016, 10, 5).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))

      val incomeExplNoDatesMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.OtherForm.code), grossAmount = Some(27000), notificationDate = None, updateActionDate = None)
      val (incomeExpNoDatesMsg, incomeExpNoDatesEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplNoDatesMod)

      incomeExpNoDatesMsg mustBe Some(Messages("tai.income.calculation.manual.update.informationLetter.withoutDate"))
      incomeExpNoDatesEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))
    }

    "return the correct income explanation message when income was updated via letter " in {
      val incomeExplMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.Letter.code), grossAmount = Some(27000))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.manual.update.letter", new LocalDate(2016, 5, 8).toString("d MMMM yyyy"), new LocalDate(2016, 10, 5).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))

      val incomeExplNoDatesMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.Letter.code), grossAmount = Some(27000), notificationDate = None, updateActionDate = None)
      val (incomeExpNoDatesMsg, incomeExpNoDatesEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplNoDatesMod)

      incomeExpNoDatesMsg mustBe Some(Messages("tai.income.calculation.manual.update.letter.withoutDate"))
      incomeExpNoDatesEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))
    }

    "return the correct income explanation message when income was updated via internet " in {
      val incomeExplMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.Internet.code), grossAmount = Some(27000))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.manual.update.internet", new LocalDate(2016, 10, 5).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))

      val incomeExplNoDatesMod = continuousParsedJson.copy(iabdSource = Some(IabdUpdateSource.Internet.code), grossAmount = Some(27000), notificationDate = None)
      val (incomeExpNoDatesMsg, incomeExpNoDatesEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplNoDatesMod)

      incomeExpNoDatesMsg mustBe Some(Messages("tai.income.calculation.manual.update.internet.withoutDate"))
      incomeExpNoDatesEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.manual.update.estimate", MoneyPounds(27000, 0).quantity))
    }

    "return the correct income explanation message for Ceased Employment with End date and final pay to date present " in {
      val incomeExplMod = continuousParsedJson.copy(employmentStatus = Some(3), endDate = Some(new LocalDate(2016, 3, 3)), cessationPay = Some(5000), isPension = false)
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.emp", new LocalDate(2016, 3, 3).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe None

      val incomeExplPensionMod = continuousParsedJson.copy(employmentStatus = Some(3), endDate = Some(new LocalDate(2016, 3, 3)), cessationPay = Some(5000), isPension = true)
      val (incomeExpPensionMsg, incomeExpPensionEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplPensionMod)

      incomeExpPensionMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.pension", new LocalDate(2016, 3, 3).toString("d MMMM yyyy")))
      incomeExpPensionEstimateMsg mustBe None
    }

    "return the correct income explanation message for Ceased Employment with End date and no final pay to date present " in {
      val incomeExplMod = continuousParsedJson.copy(employmentStatus = Some(3), endDate = Some(new LocalDate(2016, 3, 3)), cessationPay = None, isPension = false, grossAmount = Some(BigDecimal(23000)))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.emp.noFinalPay"))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(23000, 0).quantity))

      val incomeExplPensionMod = continuousParsedJson.copy(employmentStatus = Some(3), endDate = Some(new LocalDate(2016, 3, 3)), cessationPay = None, isPension = true, grossAmount = Some(BigDecimal(23000)))
      val (incomeExpPensionMsg, incomeExpPensionEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplPensionMod)

      incomeExpPensionMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.pension.noFinalPay"))
      incomeExpPensionEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(23000, 0).quantity))
    }

    "return the correct income explanation message for Potentially Ceased Employment with End date not present " in {
      val incomeExplMod = continuousParsedJson.copy(employmentStatus = Some(2), endDate = None, cessationPay = None, isPension = false, grossAmount = Some(BigDecimal(23000)))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.emp.noFinalPay"))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(23000, 0).quantity))

      val incomeExplPensionMod = continuousParsedJson.copy(employmentStatus = Some(2), endDate = None, cessationPay = None, isPension = true, grossAmount = Some(BigDecimal(23000)))
      val (incomeExpPensionMsg, incomeExpPensionEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplPensionMod)

      incomeExpPensionMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.pension.noFinalPay"))
      incomeExpPensionEstimateMsg mustBe Some(Messages("tai.income.calculation.rti.ceased.noFinalPay.estimate", MoneyPounds(23000, 0).quantity))
    }


    "return the correct default income explanation message when there is no Rti data and no manual updates " in {
      val incomeExplMod = continuousParsedJson.copy(isPension = false, grossAmount = Some(BigDecimal(23000)), calcAmount = None, iabdSource = Some(26))
      val (incomeExpMsg, incomeExpEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplMod)
      val year: Int = TaxYear().next.year

      incomeExpMsg mustBe Some(Messages("tai.income.calculation.default.emp", new LocalDate(year, 4, 5).toString("d MMMM yyyy")))
      incomeExpEstimateMsg mustBe Some(Messages("tai.income.calculation.default.estimate.emp", MoneyPounds(23000, 0).quantity))

      val incomeExplPensionMod = continuousParsedJson.copy(isPension = true, grossAmount = Some(BigDecimal(23000)), calcAmount = None, iabdSource = Some(26))
      val (incomeExpPensionMsg, incomeExpPensionEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(incomeExplPensionMod)

      incomeExpPensionMsg mustBe Some(Messages("tai.income.calculation.default.pension",new LocalDate(year, 4, 5).toString("d MMMM yyyy")))
      incomeExpPensionEstimateMsg mustBe Some(Messages("tai.income.calculation.default.estimate.pension", MoneyPounds(23000, 0).quantity))
    }
  }

}
