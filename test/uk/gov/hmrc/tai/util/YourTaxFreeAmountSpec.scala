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
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.viewModels.TaxFreeAmountSummaryViewModel
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.time.TaxYearResolver

class YourTaxFreeAmountSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "builds tax free amount view model" in {
    val date = LocalDate.parse("2018-12-12")
    val dateFormatted = TaxYearRangeUtil.dynamicDateRange(date, TaxYearResolver.endOfCurrentTaxYear)
    val annualTaxFreeAmount = "£42"

    val taxFreeAmountSummary = TaxFreeAmountSummaryViewModel(Seq.empty, Map.empty, Seq.empty, 42)

    val expected = YourTaxFreeAmountViewModel(dateFormatted, annualTaxFreeAmount, taxFreeAmountSummary)

    val yourTaxFreeAmount = new YourTaxFreeAmount() with ViewModelHelperMock with TaxAccountCalculatorMock

    val actual = yourTaxFreeAmount.buildTaxFreeAmount(date, Seq.empty, Map.empty, Seq.empty)

    actual mustBe expected
  }

  trait TaxAccountCalculatorMock { this: TaxAccountCalculator =>
    override def taxFreeAmount(codingComponents: Seq[CodingComponent]): BigDecimal = {
      42
    }
  }

  trait ViewModelHelperMock { this: ViewModelHelper =>
    override def withPoundPrefixAndSign(moneyPounds: MoneyPounds): String = {
      "£42"
    }
  }
}
