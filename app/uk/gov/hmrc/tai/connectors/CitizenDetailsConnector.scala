/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.Logging
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.tai.config.ApplicationConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsConnector @Inject() (
  httpClientResponse: HttpHandler,
  http: HttpClientV2,
  appConfig: ApplicationConfig
)(implicit val ec: ExecutionContext)
    extends Logging {

  def retrieveCitizenDetails(
    nino: Nino
  )(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, HttpResponse] = {
    val designatoryDetailsUrl: String = s"${appConfig.citizenDetailsUrl}/citizen-details/$nino/designatory-details"
    httpClientResponse.read(
      http
        .get(url"$designatoryDetailsUrl")
        .execute[Either[UpstreamErrorResponse, HttpResponse]]
    )
  }
}
