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

package uk.gov.hmrc.tai.model.domain

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.Live

import java.time.LocalDate

class EmploymentSpec extends PlaySpec {

  "latestAnnualAccount" must {
    "return the latest annual account" when {
      "there are multiple annual accounts" in {
        val employment = Employment(
          "",
          Live,
          None,
          Some(LocalDate.now),
          None,
          List(annualAccount1, annualAccount2),
          "",
          "",
          1,
          None,
          false,
          false,
          EmploymentIncome
        )

        employment.latestAnnualAccount mustBe Some(annualAccount2)

      }
      "there is only one annual account" in {
        val employment =
          Employment(
            "",
            Live,
            None,
            Some(LocalDate.now),
            None,
            List(annualAccount1),
            "",
            "",
            1,
            None,
            false,
            false,
            EmploymentIncome
          )

        employment.latestAnnualAccount mustBe Some(annualAccount1)
      }
    }
    "return none" when {
      "there are no annual accounts" in {
        val employment =
          Employment("", Live, None, Some(LocalDate.now), None, Nil, "", "", 1, None, false, false, EmploymentIncome)

        employment.latestAnnualAccount mustBe None
      }
    }
  }

  val annualAccount1 = AnnualAccount(7, TaxYear(2016), Available, Nil, Nil)
  val annualAccount2 = AnnualAccount(7, TaxYear(2017), Available, Nil, Nil)

}
