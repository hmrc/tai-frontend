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

package utils

import uk.gov.hmrc.tai.model.domain.TaxCodeChange

object JsonGenerator {

  def taxCodeChangeJson(taxCodeChange: TaxCodeChange) =
    s"""{"data":{
       |"current":[
       |${taxCodeChange.current
        .map { record =>
          s"""{"taxCodeId":4,
        |"taxCode":"${record.taxCode}",
        |"basisOfOperation":"Week 1 Month 1",
        |"startDate":"${record.startDate.toString}",
        |"endDate":"${record.endDate.toString}",
        |"employerName":"${record.employerName}",
        |"pensionIndicator":${record.pensionIndicator},
        |"primary":${record.primary}
        |}"""
        }
        .mkString(",")}
    ], "previous": [
       |${taxCodeChange.previous
        .map { record =>
          s"""{"taxCodeId":4,
        "taxCode":"${record.taxCode}",
        |"basisOfOperation":"Week 1 Month 1",
        |"startDate":"${record.startDate.toString}",
        |"endDate":"${record.endDate.toString}",
        |"employerName":"${record.employerName}",
        |"pensionIndicator":${record.pensionIndicator},
        |"primary":${record.primary}
        |}"""
        }
        .mkString(",")}
    ]},"links":[]}""".stripMargin

  val taxCodeIncomesJson: String = """{"data":[
                                     |{"componentType":"EmploymentIncome",
                                     |"employmentId":1,
                                     |"amount":0,
                                     |"description":"EmploymentIncome",
                                     |"taxCode":"taxCode1",
                                     |"name":"RTI SETUP",
                                     |"basisOperation":"OtherBasisOperation",
                                     |"status":"Live",
                                     |"inYearAdjustmentIntoCY":0,
                                     |"totalInYearAdjustment":0,
                                     |"inYearAdjustmentIntoCYPlusOne":0}
                                     |],"links":[]}
                                     |""".stripMargin

}
