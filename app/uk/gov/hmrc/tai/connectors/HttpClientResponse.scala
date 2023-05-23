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
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HttpClientResponse @Inject() (httpClient: HttpClient)(implicit ec: ExecutionContext) extends Logging {

  def read(
    response: Future[Either[UpstreamErrorResponse, HttpResponse]]
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    EitherT(response.map {
      case Right(response) =>
        Right(response)
      case Left(error) if error.statusCode == NOT_FOUND || error.statusCode == UNPROCESSABLE_ENTITY =>
        logger.info(error.message)
        Left(error)
      case Left(error) if error.statusCode >= 499 || error.statusCode == TOO_MANY_REQUESTS =>
        logger.error(error.message)
        Left(error)
      case Left(error) =>
        logger.error(error.message, error)
        Left(error)
    } recover { case exception: HttpException =>
      logger.error(exception.message)
      Left(UpstreamErrorResponse(exception.message, 502, 502))
    })

  def getFromApiV2(url: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, JsValue] = { /// To be removed
    val futureResponse = httpClient.GET[Either[UpstreamErrorResponse, HttpResponse]](url)
    read(futureResponse).map(_.json)
  }

  def putToApi[I](url: String, data: I)(implicit
    hc: HeaderCarrier,
    writes: Writes[I]
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    read(httpClient.PUT[I, Either[UpstreamErrorResponse, HttpResponse]](url, data))

  def postToApi[I](url: String, data: I)(implicit
    hc: HeaderCarrier,
    writes: Writes[I]
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    read(httpClient.POST[I, Either[UpstreamErrorResponse, HttpResponse]](url, data))

  def deleteFromApi(url: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    read(httpClient.DELETE[Either[UpstreamErrorResponse, HttpResponse]](url))
}
