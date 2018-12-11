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

package uk.gov.hmrc.tai.util

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.{DividendTax, MarriageAllowanceReceived, PersonalAllowancePA}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountSummaryViewModel
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver

class YourTaxFreeAmountSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  trait TaxAccountCalculatorMock {
    this: TaxAccountCalculator =>
    override def taxFreeAmount(codingComponents: Seq[CodingComponent]): BigDecimal = {
      42
    }
  }

  val previousDate = new LocalDate(2017, 12, 12)
  val currentDate = new LocalDate(2018, 6, 5)

  def createYourTaxFreeAmountViewModel(currentCodingComponents: Seq[CodingComponent],
                                       previousDeductions: Seq[CodingComponent],
                                       previousAdditions: Seq[CodingComponent],
                                       currentDeductions: Seq[CodingComponent],
                                       currentAdditions: Seq[CodingComponent],
                                       previousPersonalAllowance: BigDecimal = 0,
                                       currentPersonalAllowance: BigDecimal = 0): YourTaxFreeAmountViewModel = {
    val formattedPreviousDate = Dates.formatDate(previousDate)
    val formattedCurrentDate = createFormattedDate(currentDate)
    val previousTaxFreeAmount = 42
    val currentTaxFreeAmount = 42
    val taxFreeAmountSummary = TaxFreeAmountSummaryViewModel(currentCodingComponents, Map.empty, Seq.empty, 42)

    YourTaxFreeAmountViewModel(formattedPreviousDate,
      formattedCurrentDate,
      previousTaxFreeAmount,
      currentTaxFreeAmount,
      taxFreeAmountSummary,
      new MungedCodingComponents(previousDeductions, previousAdditions, currentDeductions, currentAdditions),
      previousPersonalAllowance,
      currentPersonalAllowance)
  }

  def createFormattedDate(date: LocalDate): String = {
    TaxYearRangeUtil.dynamicDateRange(date, TaxYearResolver.endOfCurrentTaxYear)
  }

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val yourTaxFreeAmount = new YourTaxFreeAmount() with TaxAccountCalculatorMock

  "buildTaxFreeAmount" should {
    "builds personal allowance" in {
      val expected = createYourTaxFreeAmountViewModel(Seq.empty, Seq.empty, Seq.empty, Seq.empty, Seq.empty)
      yourTaxFreeAmount.buildTaxFreeAmount(previousDate, currentDate, Seq.empty, Seq.empty, Seq.empty, Map.empty) mustBe expected
    }

    "apply deductions and additions" in {
      val deduction = Seq(CodingComponent(DividendTax, Some(123), 2500, "DividendTax"));
      val addition = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 5885, "MarriageAllowanceReceived"));
      val currentCodingComponents = deduction ++ addition

      val expected = createYourTaxFreeAmountViewModel(currentCodingComponents, Seq.empty, Seq.empty, deduction, addition)
      yourTaxFreeAmount.buildTaxFreeAmount(previousDate, currentDate, Seq.empty, currentCodingComponents, Seq.empty, Map.empty) mustBe expected
    }

    "previous deductions and additions" in {
      val deduction = Seq(CodingComponent(DividendTax, Some(123), 2500, "DividendTax"));
      val addition = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 5885, "MarriageAllowanceReceived"));
      val previousCodingComponents = deduction ++ addition

      val expected = createYourTaxFreeAmountViewModel(Seq.empty, deduction, addition, Seq.empty, Seq.empty)
      yourTaxFreeAmount.buildTaxFreeAmount(previousDate, currentDate, previousCodingComponents, Seq.empty, Seq.empty, Map.empty) mustBe expected
    }

    "calculate personal allowance" in {
      val currentCodingComponent = Seq(CodingComponent(PersonalAllowancePA, Some(123), 5885, "PersonalAllowancePA"));

      val expected = createYourTaxFreeAmountViewModel(currentCodingComponent, Seq.empty, Seq.empty, Seq.empty, currentCodingComponent, 0, 5885)
      yourTaxFreeAmount.buildTaxFreeAmount(previousDate, currentDate, Seq.empty, currentCodingComponent, Seq.empty, Map.empty) mustBe expected
    }
  }

}
