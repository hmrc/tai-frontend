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

import javax.inject.Inject
import play.api.libs.json.Reads
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.tai.config.{DefaultServicesConfig}
import uk.gov.hmrc.tai.model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaiConnector @Inject()(http: DefaultHttpClient) extends RawResponseReads with DefaultServicesConfig {

  val serviceUrl = baseUrl("tai")

  def url(path: String) = s"$serviceUrl$path"

  def responseTo[T](uri: String)(response: HttpResponse)(implicit rds: Reads[T]) = response.json.as[T]

  val STATUS_OK = 200
  val STATUS_EMAIL_RESPONSE = 201

  def calculateEstimatedPay(payDetails: PayDetails)(implicit hc: HeaderCarrier): Future[CalculatedPay] = {
    val postUrl = url(s"/tai/calculator/calculate-estimated-pay")
    http.POST(postUrl, payDetails).map(responseTo[CalculatedPay](postUrl))
  }
}
