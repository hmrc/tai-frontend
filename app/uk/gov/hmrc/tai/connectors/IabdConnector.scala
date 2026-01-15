/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.data.EitherT
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IabdConnector @Inject() (
  httpClientV2: HttpClientV2,
  servicesConfig: ServicesConfig,
  httpClientResponse: HttpClientResponse
)(implicit
  ec: ExecutionContext
) {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def url(path: String): String = s"$serviceUrl$path"

  def getIabds(nino: Nino, taxYear: TaxYear)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, JsValue] = {
    val iabdsUrl = url(s"/tai/${nino.nino}/iabds/years/${taxYear.year}iabdType=27")

    httpClientResponse.read(
      httpClientV2
        .get(url"$iabdsUrl")
        .execute[Either[UpstreamErrorResponse, JsValue]]
    )
  }
}
