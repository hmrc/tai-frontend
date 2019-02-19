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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import controllers.FakeTaiPlayApplication
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain.{CarBenefit, JobExpenses, TaxCodeChange}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount._

class TaxCodeChangeDynamicTextViewModelSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport {

  private val taxFreeInfo = TaxFreeInfo("12-12-2015", 2000, 1000)
  private val jobExpensesIncrease = CodingComponentPair(JobExpenses, Some(2), Some(50), Some(100))
  private val carBenefitIncrease = CodingComponentPair(CarBenefit, Some(1), Some(1000), Some(2000))
  private val taxCodeChange = mock[TaxCodeChange]
  implicit val messagesApi = app.injector.instanceOf[MessagesApi]

  "TaxCodeChangeDynamicTextViewModel apply method" must {
    "have no reasons" when {
      "there are no allowances and deductions" in {
        val pairs: AllowancesAndDeductionPairs = AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
        val taxFreeAmountComparison: YourTaxFreeAmountComparison = YourTaxFreeAmountComparison(None, taxFreeInfo, pairs)
        val model = TaxCodeChangeDynamicTextViewModel(taxCodeChange, taxFreeAmountComparison)

        model.reasons mustBe Seq.empty
      }

      "there is no previous amount" in {
        val noPreviousAmount = CodingComponentPair(JobExpenses, None, None, Some(123))
        val pairs: AllowancesAndDeductionPairs = AllowancesAndDeductionPairs(Seq(noPreviousAmount), Seq.empty)
        val taxFreeAmountComparison: YourTaxFreeAmountComparison = YourTaxFreeAmountComparison(None, taxFreeInfo, pairs)

        val model = TaxCodeChangeDynamicTextViewModel(taxCodeChange, taxFreeAmountComparison)

        model.reasons mustBe Seq.empty
      }

      "there is no current amount" in {
        val noCurrentAmount = CodingComponentPair(JobExpenses, None, Some(123), None)
        val pairs: AllowancesAndDeductionPairs = AllowancesAndDeductionPairs(Seq(noCurrentAmount), Seq.empty)
        val taxFreeAmountComparison: YourTaxFreeAmountComparison = YourTaxFreeAmountComparison(None, taxFreeInfo, pairs)

        val model = TaxCodeChangeDynamicTextViewModel(taxCodeChange, taxFreeAmountComparison)

        model.reasons mustBe Seq.empty
      }
    }

    "translate a YourTaxFreeAmountViewModel to a seq of dynamic text" in {
      val pairs: AllowancesAndDeductionPairs = AllowancesAndDeductionPairs(Seq(jobExpensesIncrease), Seq(carBenefitIncrease))
      val taxFreeAmountComparison: YourTaxFreeAmountComparison = YourTaxFreeAmountComparison(None, taxFreeInfo, pairs)

      val model = TaxCodeChangeDynamicTextViewModel(taxCodeChange, taxFreeAmountComparison)

      model.reasons mustBe Seq(
        "Your Job expenses has been updated",
        "Your Car benefit has been updated"
      )
    }
  }

  "translate to the generic benefit message" when {
    "there are more than 4 benefit changes" in {
      val pairs: AllowancesAndDeductionPairs = AllowancesAndDeductionPairs(Seq.empty,
        Seq(carBenefitIncrease, carBenefitIncrease, carBenefitIncrease, carBenefitIncrease, carBenefitIncrease))
      val taxFreeAmountComparison: YourTaxFreeAmountComparison = YourTaxFreeAmountComparison(None, taxFreeInfo, pairs)

      val model = TaxCodeChangeDynamicTextViewModel(taxCodeChange, taxFreeAmountComparison)

      model.reasons mustBe Seq(messagesApi("taxCode.change.yourTaxCodeChanged.paragraph"))
    }
  }
}
