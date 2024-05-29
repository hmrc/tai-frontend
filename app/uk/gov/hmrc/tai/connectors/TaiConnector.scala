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

package uk.gov.hmrc.tai.connectors

import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaiConnector @Inject() (httpClientV2: HttpClientV2, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def url(path: String): String = s"$serviceUrl$path"

  def calculateEstimatedPay(payDetails: PayDetails)(implicit hc: HeaderCarrier): Future[CalculatedPay] = {
    val postUrl = url(s"/tai/calculator/calculate-estimated-pay")

    httpClientV2
      .post(url"$postUrl")
      .withBody(Json.toJson(payDetails))
      .execute[CalculatedPay]
  }
}
