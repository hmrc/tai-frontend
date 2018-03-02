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

package uk.gov.hmrc.tai.model.tai

import hmrc.nps2.{NpsEmployment, Iabd, TaxAccount}
import uk.gov.hmrc.tai.model.rti.{RtiStatus, RtiData}
import uk.gov.hmrc.http.HeaderCarrier

case class AnnualAccount(
  year: TaxYear,
  nps: Option[TaxAccount] = None,
  rti: Option[RtiData] = None,
  rtiStatus: Option[RtiStatus] = None
) {
  def employments: Seq[Employment] = {
    val rtiEmps = rti.map{_.employments}.getOrElse(Nil)
    val npsIncomes = nps.map(_.incomes.filter(_.employmentRecord.isDefined)).getOrElse(Seq())
    rtiEmps.map{ r =>
      npsIncomes.filter { n =>
        n.payeRef == r.payeRef &&
          n.taxDistrict == Some(r.officeRefNo.toInt)
      } match {
        case Seq(one) => Some(Employment(one,Some(r)))
        case Nil => None
        case m => m.filter {
          _.worksNumber == r.currentPayId &&
          r.currentPayId.isDefined
        }.headOption.map{
          h => Employment(h,Some(r))
        }
      }
    }.flatten
  }
}

object AnnualAccount {

  import play.api.libs.json._

  def fromJson(
                npsAccount: JsValue,
                npsEmployment: JsValue,
                npsIabds: JsValue,
                rtiData: Option[JsValue],
                taxYear: TaxYear = TaxYear(),
                nino: String,
                rtiStatus: Option[JsValue]
                )(implicit hc: HeaderCarrier): AnnualAccount = {
    val nps = npsAccount.as[TaxAccount]
    val allIabds = npsIabds.as[List[Iabd]]
    val employments = npsEmployment.as[Seq[NpsEmployment]].map { emp =>
      emp.copy(
        iabds = allIabds.filter{
          _.employmentSequence == Some(emp.sequenceNumber)}
      )
    }

    val rtiDetails = rtiData.map(_.as[RtiData])
    val rtiState = rtiStatus.map(_.as[RtiStatus])

    AnnualAccount(
      taxYear,
      Some(nps.withEmployments{employments}),
      rtiDetails,
      rtiState
    )
  }

}
