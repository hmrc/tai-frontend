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

package uk.gov.hmrc.tai.filters

import uk.gov.hmrc.tai.model.domain.{EmploymentIncome, PensionIncome}
import uk.gov.hmrc.tai.model.domain.income.{Live, TaxCodeIncome}

trait TaxAccountFilter {

  def liveEmployment(taxCodeIncome: TaxCodeIncome) =
    taxCodeIncome.componentType == EmploymentIncome && taxCodeIncome.status == Live

  def livePension(taxCodeIncome: TaxCodeIncome) =
    taxCodeIncome.componentType == PensionIncome && taxCodeIncome.status == Live

  def ceasedEmployment(taxCodeIncome: TaxCodeIncome) =
    taxCodeIncome.componentType == EmploymentIncome && taxCodeIncome.status != Live
}
