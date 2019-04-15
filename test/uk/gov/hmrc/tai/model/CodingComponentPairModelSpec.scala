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

package uk.gov.hmrc.tai.model

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.GiftAidPayments
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}

class CodingComponentPairModelSpec extends PlaySpec with FakeTaiPlayApplication {
  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "CodingComponentPairModel" should {
    "return a tax label summary with the previous and current amounts" in {
      val previousAmount = 456
      val currentAmount = 789
        val taxBand = TaxBand("B", "BR", 16500, 1000, Some(0), Some(16500), 20)
        val incomeCatergories = IncomeCategory(NonSavingsIncomeCategory, 1000, 5000, 16500, Seq(taxBand))
        val totalTax : TotalTax = TotalTax(1000, Seq(incomeCatergories), None, None, None)

      val codingComponentPair = CodingComponentPair(GiftAidPayments, None, Some(previousAmount), Some(currentAmount))
      val actual = CodingComponentPairModel(codingComponentPair, TaxFreeAmountDetails(employmentIdNameMap = Map.empty, companyCarBenefits = Seq.empty, totalTax = totalTax))

      actual mustBe CodingComponentPairModel("Gift Aid Payments", previousAmount, currentAmount)
    }
  }
}
