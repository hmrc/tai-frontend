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

import akka.Done
import cats.data.EitherT
import play.api.Logging
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyCacheConnector @Inject() (
  httpClient: HttpClient,
  httpClientResponse: HttpClientResponse,
  servicesConfig: ServicesConfig
)(implicit
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def cacheUrl(journeyName: String): String = s"$serviceUrl/tai/journey-cache/$journeyName"

  def currentCache(journeyName: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](cacheUrl(journeyName))
    )

  def currentValueAs[T](journeyName: String, key: String, convert: String => T)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Option[T]] = {
    val url = s"${cacheUrl(journeyName)}/values/$key"
    httpClientResponse
      .read(
        httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](url)
      )
      .map { result =>
        result.json.asOpt[String].map(data => convert(data))
      } /// TODO - Investigate why type T is needed here if the result is parsed from String
  }

  def mandatoryJourneyValueAs[T](journeyName: String, key: String, convert: String => T)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, T] = {
    val url = s"${cacheUrl(journeyName)}/values/$key"
    httpClientResponse
      .read(
        httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](url)
      )
      .map { result =>
        convert(result.json.as[String])
      } /// TODO - Investigate why type T is needed here if the result is parsed from String
  }

  def cache(journeyName: String, data: Map[String, String])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Map[String, String]] =
    httpClientResponse
      .read(
        httpClient.POST[Map[String, String], Either[UpstreamErrorResponse, HttpResponse]](cacheUrl(journeyName), data)
      )
      .map(_.json.as[Map[String, String]])

  def flush(journeyName: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Done] =
    httpClientResponse
      .read(
        httpClient.DELETE[Either[UpstreamErrorResponse, HttpResponse]](cacheUrl(journeyName))
      )
      .map(_ => Done)

  def flushWithEmpId(journeyName: String, empId: Int)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Done] =
    httpClientResponse
      .read(
        httpClient.DELETE[Either[UpstreamErrorResponse, HttpResponse]](cacheUrl(s"$journeyName/$empId"))
      )
      .map(_ => Done)

  def testOnlyCacheUrl(journeyName: String): String = s"$serviceUrl/tai/test-only/journey-cache/$journeyName"

  def delete(journeyName: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    httpClientResponse.read(
      httpClient.DELETE[Either[UpstreamErrorResponse, HttpResponse]](testOnlyCacheUrl(journeyName))
    )

}
