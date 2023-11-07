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

import cats.data.EitherT
import com.google.inject.Inject
import play.api.Logging
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, HtmlPartial}
import uk.gov.hmrc.sca.config.AppConfig
import uk.gov.hmrc.tai.model.{PertaxRequestDetails, PertaxResponse}

import scala.concurrent.{ExecutionContext, Future}

class PertaxConnector @Inject() (
  httpClient: HttpClient,
  http: HttpClientV2,
  appConfig: AppConfig,
  httpClientResponse: HttpHandler,
  headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
) extends Logging {

  private val pertaxUrl = appConfig.pertaxUrl

  def pertaxPostAuthorise()(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, UpstreamErrorResponse, PertaxResponse] =
    httpClientResponse
      .read(
        http
          .post(url"$pertaxUrl/pertax/authorise")
          .setHeader(HeaderNames.ACCEPT -> "application/vnd.hmrc.2.0+json")
          .withBody(PertaxRequestDetails().toString)
          .execute[Either[UpstreamErrorResponse, HttpResponse]]
      )
      .map(_.json.as[PertaxResponse])

  def loadPartial(url: String)(implicit request: RequestHeader, ec: ExecutionContext): Future[HtmlPartial] = {
    implicit val hc: HeaderCarrier = headerCarrierForPartialsConverter.fromRequestWithEncryptedCookie(request)

    httpClient.GET[HtmlPartial](s"$pertaxUrl$url") map {
      case partial: HtmlPartial.Success =>
        partial
      case partial: HtmlPartial.Failure =>
        logger.error(s"Failed to load partial from $url, partial info: $partial")
        partial
    } recover { case e =>
      logger.error(s"Failed to load partial from $url", e)
      e match {
        case ex: HttpException =>
          HtmlPartial.Failure(Some(ex.responseCode))
        case _ =>
          HtmlPartial.Failure(None)
      }
    }
  }
}
