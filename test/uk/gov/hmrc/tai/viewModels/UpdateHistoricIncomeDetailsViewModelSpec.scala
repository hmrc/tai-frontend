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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.viewModels.income.previousYears.UpdateHistoricIncomeDetailsViewModel

class UpdateHistoricIncomeDetailsViewModelSpec extends PlaySpec with FakeTaiPlayApplication {

  "UpdateHistoricIncomeDetailsViewModel" must {
    "return given tax year" in {
      val givenYear = UpdateHistoricIncomeDetailsViewModel(2016)
      val result = givenYear.givenTaxYear
      result mustBe TaxYear(2016)
    }
    "return formatted tax year" in {
      val dynamicYear = UpdateHistoricIncomeDetailsViewModel(2016)
      val result = dynamicYear.formattedTaxYear
      result mustBe "6 April 2016 to 5 April 2017"
    }
  }

}
