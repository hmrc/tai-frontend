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

import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, JobSeekerAllowanceIncome, OtherIncome, PensionIncome}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome

trait IncomeTaxEstimateHelper {

  def getTaxOnIncomeTypeHeading(taxCodeIncomes: Seq[TaxCodeIncome]): String = {

    val employments: Boolean = taxCodeIncomes.exists(x => x.componentType == EmploymentIncome)
    val pensions: Boolean = taxCodeIncomes.exists(x => x.componentType == PensionIncome)
    val other: Boolean = taxCodeIncomes.exists(x => x.componentType == JobSeekerAllowanceIncome || x.componentType == OtherIncome)

    (employments, pensions, other) match {
      case (true, false, false) => "employment"
      case (false, true, false) => "private.pension"
      case (_, _, _)            => "paye"
    }
  }

}
