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

package uk.gov.hmrc.tai.config

import uk.gov.hmrc.tai.viewModels.{ComplexTax, SimpleTax}

object TaxBandComplexityConfig {

  val ukTaxBandType = Map(
    "B" -> SimpleTax,
    "D0" -> SimpleTax,
    "D1" -> SimpleTax,
    "pa" -> SimpleTax,
    "TaxedIncome" -> SimpleTax,
    "SR" -> ComplexTax,
    "PSR" -> ComplexTax,
    "S" -> ComplexTax,
    "SDR" -> ComplexTax,
    "LSR" -> ComplexTax,
    "HSR1" -> ComplexTax,
    "HSR2" -> ComplexTax,
    "LDR" -> ComplexTax,
    "HDR1" -> ComplexTax,
    "HDR2" -> ComplexTax
  )

  val scottishBandType = Map(
    "B" -> SimpleTax,
    "D0" -> SimpleTax,
    "D1" -> SimpleTax,
    "D2" -> SimpleTax,
    "D3" -> SimpleTax,
    "D4" -> SimpleTax,
    "D5" -> SimpleTax,
    "D6" -> SimpleTax,
    "D7" -> SimpleTax,
    "D8" -> SimpleTax,
    "pa" -> SimpleTax,
    "SR" -> ComplexTax,
    "PSR" -> ComplexTax,
    "S" -> ComplexTax,
    "SDR" -> ComplexTax,
    "LSR" -> ComplexTax,
    "HSR1" -> ComplexTax,
    "HSR2" -> ComplexTax,
    "LDR" -> ComplexTax,
    "HDR1" -> ComplexTax,
    "HDR2" -> ComplexTax,
  "TaxedIncome" -> ComplexTax
  )
}
