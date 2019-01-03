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

package uk.gov.hmrc.tai.connectors

import play.api.libs.json.Reads
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CoreDelete, CoreGet, CorePost, CorePut, _}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaiConnector extends RawResponseReads{
  def http: CoreGet with CorePost with CorePut with CoreDelete

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def responseTo[T](uri: String)(response: HttpResponse)(implicit rds: Reads[T]) = response.json.as[T]

  val STATUS_OK = 200
  val STATUS_EMAIL_RESPONSE = 201

  def calculateEstimatedPay(payDetails : PayDetails)(implicit hc: HeaderCarrier): Future[CalculatedPay] = {
    val postUrl = url(s"/tai/calculator/calculate-estimated-pay")
    http.POST(postUrl, payDetails).map(responseTo[CalculatedPay](postUrl))
  }
}
// $COVERAGE-OFF$
object TaiConnector extends TaiConnector with ServicesConfig {

  lazy val serviceUrl = baseUrl("tai")
  override def http = WSHttp
}
// $COVERAGE-ON$
