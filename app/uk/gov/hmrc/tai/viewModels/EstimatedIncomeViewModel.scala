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

import play.api.libs.json.Json

case class BandedGraph(
                        id:String,
                        bands:List[Band] = List(),
                        minBand :BigDecimal =0,
                        nextBand :BigDecimal = 0,
                        incomeTotal:BigDecimal = 0,
                        zeroIncomeAsPercentage: BigDecimal =0,
                        zeroIncomeTotal: BigDecimal =0,
                        incomeAsPercentage: BigDecimal =0,
                        taxTotal:BigDecimal =0,
                        nextBandMessage: Option[String] = None,
                        swatch:Option[Swatch] = None
                      )

case class Band(
                 colour:String,
                 barPercentage: BigDecimal = 0,
                 tablePercentage: String = "0",
                 income: BigDecimal = 0,
                 tax: BigDecimal = 0,
                 bandType: String
               )

case class Swatch(barPercentage:BigDecimal = 0,taxAmount:BigDecimal = 0)

object Band {
  implicit val format = Json.format[Band]
}

object Swatch{
  implicit val format = Json.format[Swatch]
}

object BandedGraph {
  implicit val format = Json.format[BandedGraph]
}

