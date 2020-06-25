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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain._
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.constants.TaiConstants.EYU_DATE_FORMAT
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.Live

class HistoricIncomeCalculationViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "HistoricIncomeCalculationViewModel" should {
    "have employment name" in {
      val sut = createSUT()
      sut.employerName mustBe Some(empName)
    }

    "have employee id" in {
      val sut = createSUT()
      sut.employmentId mustBe sampleEmployeeId
    }

    "have payments details" in {
      val sut = createSUT()
      sut.payments mustBe Nil
    }

    "have eyu messages" in {
      val sut = createSUT()
      sut.endOfTaxYearUpdateMessages mustBe Nil
    }

    "have real time status" in {
      val sut = createSUT()
      sut.realTimeStatus mustBe sampleRealTimeStatus
    }
  }

  "Given tax year, employee id and sequence of employments, HistoricIncomeCalculationViewModel" should {
    "be able to create view model" when {
      "sequence of employment is provided" in {
        val sut = createSUT()
        sut.employerName mustBe Some(empName)
      }

      "requested employment has no payments data" in {
        val sut = createSUT()
        sut.payments mustBe Nil
      }

      "requested employment has payments available" in {
        val sut = createSUT(employmentId = 2)
        sut.payments mustBe List(samplePayment)
      }

      "requested employment has no end of tax year update messages" in {
        val sut = createSUT()
        sut.endOfTaxYearUpdateMessages mustBe Nil
      }

      "requested employment has end of tax year update details" in {
        val date = new LocalDate("2017-06-09")
        val sampleEndOfTaxYearUpdate = EndOfTaxYearUpdate(date, Seq(Adjustment(NationalInsuranceAdjustment, -10.0)))
        val sampleAnnualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, List(sampleEndOfTaxYearUpdate))
        val sampleEmployment = Employment(
          "emp1",
          Live,
          None,
          new LocalDate(2017, 6, 10),
          None,
          Seq(sampleAnnualAccount),
          "taxNumber",
          "payeNumber",
          1,
          None,
          false,
          false)

        val sut = createSUT(employments = List(sampleEmployment))
        sut.endOfTaxYearUpdateMessages mustBe Seq(
          Messages("tai.income.calculation.eyu.single.nationalInsurance", date.toString(EYU_DATE_FORMAT), "10.0 less"))
      }
    }
  }

  "fetchRealTimeStatus" should {
    "return the real time status" when {
      "there is no annual account provided" in {
        val sut = HistoricIncomeCalculationViewModel.fetchRealTimeStatus(None)
        sut mustBe TemporarilyUnavailable
      }

      "given an annual account" in {
        val sut = HistoricIncomeCalculationViewModel.fetchRealTimeStatus(Some(sampleAnnualAccount))
        sut mustBe Available
      }
    }
  }

  "fetchEmploymentAndAnnualAccount" should {
    "return matching annual account and employment" when {
      "matching employment sequence number is provided and employment has no annual account" in {
        val (sutEmployment, sutAnnualAccount) =
          HistoricIncomeCalculationViewModel.fetchEmploymentAndAnnualAccount(sampleEmployments, previousYear, 1)
        sutEmployment mustBe Some(sampleEmployment1)
        sutAnnualAccount mustBe None
      }

      "matching employment sequence number is provided with annual account" in {
        val (sutEmployment, sutAnnualAccount) =
          HistoricIncomeCalculationViewModel.fetchEmploymentAndAnnualAccount(sampleEmployments, previousYear, 2)
        sutEmployment mustBe Some(sampleEmployment2)
        sutAnnualAccount mustBe Some(sampleAnnualAccount)
      }
    }
  }

  "filterEndOfYearUpdateAdjustments" should {
    "return the valid adjustments to form messages" when {
      "no endOfYearTaxUpdates in annual account" in {
        val sut = HistoricIncomeCalculationViewModel.filterEndOfYearUpdateAdjustments(sampleAnnualAccount)
        sut mustBe Nil
      }

      "only one endOfYearTaxUpdate is provided with one valid adjustment type" in {
        val eyu = EndOfTaxYearUpdate(new LocalDate("2017-05-26"), Seq(Adjustment(NationalInsuranceAdjustment, 10)))
        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu))
        val sut = HistoricIncomeCalculationViewModel.filterEndOfYearUpdateAdjustments(annualAccount)
        sut mustBe Seq((Adjustment(NationalInsuranceAdjustment, 10), new LocalDate("2017-05-26")))
      }

      "only one endOfYearTaxUpdate is provided with multiple valid adjustment type" in {
        val adj1 = Adjustment(NationalInsuranceAdjustment, 10)
        val adj2 = Adjustment(TaxAdjustment, 0)
        val adj3 = Adjustment(IncomeAdjustment, 100)
        val date = new LocalDate("2017-05-26")

        val eyu = EndOfTaxYearUpdate(date, Seq(adj1, adj2, adj3))
        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu))
        val sut = HistoricIncomeCalculationViewModel.filterEndOfYearUpdateAdjustments(annualAccount)
        sut mustBe Seq((adj1, date), (adj3, date))
      }

      "multiple endOfYearTaxUpdate is provided with one valid adjustment type" in {
        val adj1 = Adjustment(NationalInsuranceAdjustment, 0)
        val adj2 = Adjustment(TaxAdjustment, 0)
        val date1 = new LocalDate("2017-05-26")
        val eyu1 = EndOfTaxYearUpdate(date1, Seq(adj1, adj2))

        val adj3 = Adjustment(IncomeAdjustment, 0)
        val adj4 = Adjustment(IncomeAdjustment, 10)
        val date2 = new LocalDate("2017-06-09")
        val eyu2 = EndOfTaxYearUpdate(date2, Seq(adj3, adj4))

        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu1, eyu2))
        val sut = HistoricIncomeCalculationViewModel.filterEndOfYearUpdateAdjustments(annualAccount)
        sut mustBe Seq((adj4, date2))
      }

      "multiple endOfYearTaxUpdate is provided with multiple valid adjustment type" in {
        val adj1 = Adjustment(NationalInsuranceAdjustment, 10)
        val adj2 = Adjustment(TaxAdjustment, 0)
        val date1 = new LocalDate("2017-05-26")
        val eyu1 = EndOfTaxYearUpdate(date1, Seq(adj1, adj2))

        val adj3 = Adjustment(IncomeAdjustment, 0)
        val adj4 = Adjustment(IncomeAdjustment, 10)
        val date2 = new LocalDate("2017-06-09")
        val eyu2 = EndOfTaxYearUpdate(date2, Seq(adj3, adj4))

        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu1, eyu2))
        val sut = HistoricIncomeCalculationViewModel.filterEndOfYearUpdateAdjustments(annualAccount)
        sut mustBe Seq((adj1, date1), (adj4, date2))
      }

      "multiple endOfYearTaxUpdate is provided with no valid adjustment type" in {
        val adj1 = Adjustment(NationalInsuranceAdjustment, 0)
        val date1 = new LocalDate("2017-05-26")
        val eyu1 = EndOfTaxYearUpdate(date1, Seq(adj1))

        val adj3 = Adjustment(IncomeAdjustment, 0)
        val date2 = new LocalDate("2017-06-09")
        val eyu2 = EndOfTaxYearUpdate(date2, Seq(adj3))

        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu1, eyu2))
        val sut = HistoricIncomeCalculationViewModel.filterEndOfYearUpdateAdjustments(annualAccount)
        sut mustBe Seq()
      }
    }
  }

  "createEndOfYearTaxUpdateMessages" should {
    "form valid messages" when {
      val date = new LocalDate("2017-05-26")

      "there are no valid EndOfYearTaxUpdate object" in {
        val sut = HistoricIncomeCalculationViewModel.createEndOfYearTaxUpdateMessages(sampleAnnualAccount)
        sut mustBe Nil
      }

      "have only TaxAdjustment message" in {
        val eyu = EndOfTaxYearUpdate(date, Seq(Adjustment(TaxAdjustment, 100.0)))
        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu))
        val sut = HistoricIncomeCalculationViewModel.createEndOfYearTaxUpdateMessages(annualAccount)
        sut mustBe Seq(
          Messages("tai.income.calculation.eyu.single.taxPaid", date.toString(EYU_DATE_FORMAT), "100.0 more"))
      }

      "have only NationalInsuranceAdjustment message" in {
        val eyu = EndOfTaxYearUpdate(date, Seq(Adjustment(NationalInsuranceAdjustment, 100.0)))
        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu))
        val sut = HistoricIncomeCalculationViewModel.createEndOfYearTaxUpdateMessages(annualAccount)
        sut mustBe Seq(
          Messages("tai.income.calculation.eyu.single.nationalInsurance", date.toString(EYU_DATE_FORMAT), "100.0 more"))
      }

      "have only IncomeAdjustment message" in {
        val eyu = EndOfTaxYearUpdate(date, Seq(Adjustment(IncomeAdjustment, -100.0)))
        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu))
        val sut = HistoricIncomeCalculationViewModel.createEndOfYearTaxUpdateMessages(annualAccount)
        sut mustBe Seq(
          Messages("tai.income.calculation.eyu.single.taxableincome", date.toString(EYU_DATE_FORMAT), "100.0 less"))
      }

      "there are multiple valid EndOfYearTaxUpdate object with multiple received dates" in {
        val adj1 = Adjustment(NationalInsuranceAdjustment, -10.0)
        val adj2 = Adjustment(TaxAdjustment, 0)
        val date1 = new LocalDate("2017-05-26")
        val eyu1 = EndOfTaxYearUpdate(date1, Seq(adj1, adj2))

        val adj3 = Adjustment(IncomeAdjustment, 0)
        val adj4 = Adjustment(TaxAdjustment, 100.0)
        val date2 = new LocalDate("2017-06-09")
        val eyu2 = EndOfTaxYearUpdate(date2, Seq(adj3, adj4))

        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu1, eyu2))
        val sut = HistoricIncomeCalculationViewModel.createEndOfYearTaxUpdateMessages(annualAccount)

        sut mustBe Seq(
          Messages("tai.income.calculation.eyu.multi.nationalInsurance", date1.toString(EYU_DATE_FORMAT), "10.0 less"),
          Messages("tai.income.calculation.eyu.multi.taxPaid", date2.toString(EYU_DATE_FORMAT), "100.0 more")
        )
      }

      "have multiple messages with same received date" in {
        val adj1 = Adjustment(NationalInsuranceAdjustment, -10.0)
        val adj2 = Adjustment(TaxAdjustment, 0)
        val adj3 = Adjustment(IncomeAdjustment, 100.0)

        val eyu = EndOfTaxYearUpdate(date, Seq(adj1, adj2, adj3))
        val annualAccount = AnnualAccount("1-2-3", previousYear, Available, Nil, Seq(eyu))
        val sut = HistoricIncomeCalculationViewModel.createEndOfYearTaxUpdateMessages(annualAccount)
        sut mustBe Seq(
          Messages("tai.income.calculation.eyu.multi.nationalInsurance", date.toString(EYU_DATE_FORMAT), "10.0 less"),
          Messages("tai.income.calculation.eyu.multi.taxableincome", date.toString(EYU_DATE_FORMAT), "100.0 more")
        )
      }
    }
  }

  val empName = "employer name"
  val sampleEndOfTaxYearUpdateMessages = Seq("EyuMessage1", "EyuMessage2")
  val sampleEmployeeId = 1
  val sampleRealTimeStatus = TemporarilyUnavailable
  val previousYear = uk.gov.hmrc.tai.model.TaxYear().prev
  val samplePayment = Payment(
    date = new LocalDate(2017, 5, 26),
    amountYearToDate = 2000,
    taxAmountYearToDate = 1200,
    nationalInsuranceAmountYearToDate = 1500,
    amount = 200,
    taxAmount = 100,
    nationalInsuranceAmount = 150,
    payFrequency = Monthly
  )

  val sampleAnnualAccount = AnnualAccount("1-2-3", previousYear, Available, List(samplePayment), Nil)

  val sampleEmployment1 =
    Employment(
      empName,
      Live,
      None,
      new LocalDate(2017, 6, 9),
      None,
      Nil,
      "taxNumber",
      "payeNumber",
      1,
      None,
      false,
      false)
  val sampleEmployment2 = Employment(
    "emp2",
    Live,
    None,
    new LocalDate(2017, 6, 10),
    None,
    Seq(sampleAnnualAccount),
    "taxNumber",
    "payeNumber",
    2,
    None,
    false,
    false)
  val sampleEmployments = List(sampleEmployment1, sampleEmployment2)

  def createSUT(
    employments: Seq[Employment] = sampleEmployments,
    employmentId: Int = 1,
    taxYear: TaxYear = previousYear) =
    HistoricIncomeCalculationViewModel(employments, employmentId, taxYear)

}
