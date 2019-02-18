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
  private val jobExpensesIncrease: CodingComponentPair = CodingComponentPair(JobExpenses, Some(2), Some(50), Some(100))
  private val carBenefitIncrease: CodingComponentPair = CodingComponentPair(CarBenefit, Some(1), Some(1000), Some(2000))
  private val taxCodeChange: TaxCodeChange = mock[TaxCodeChange]
  private val employmentsMap: Map[Int, String] = Map(1 -> "TESCO", 2 -> "Sainsburys")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]


  "TaxCodeChangeDynamicTextViewModel apply method" must {
    "translate a YourTaxFreeAmountViewModel to a seq of dynamic text" in {
      val pairs: AllowancesAndDeductionPairs = AllowancesAndDeductionPairs(Seq(jobExpensesIncrease), Seq(carBenefitIncrease))
      val taxFreeAmountComparison: YourTaxFreeAmountComparison = YourTaxFreeAmountComparison(None, taxFreeInfo, pairs)

      val model = TaxCodeChangeDynamicTextViewModel(taxCodeChange, taxFreeAmountComparison, employmentsMap)

      model.reasons mustBe Seq(
        "Job expenses from Sainsburys has increased from 50 to 100",
        "Car benefit from TESCO has increased from 1000 to 2000"
      )
    }
  }
}
