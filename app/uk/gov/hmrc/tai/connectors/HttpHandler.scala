/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpHandler @Inject()(val http: DefaultHttpClient) extends HttpErrorFunctions {

  private def customRead(http: String, url: String, response: HttpResponse): HttpResponse =
    response.status match {
      case UNAUTHORIZED => response
      case _            => handleResponse(http, url)(response)
    }

  def getFromApi(url: String)(implicit hc: HeaderCarrier): Future[JsValue] = {

    implicit val httpRds = new HttpReads[HttpResponse] {
      def read(http: String, url: String, res: HttpResponse) = customRead(http, url, res)
    }

    val futureResponse = http.GET[HttpResponse](url)

    futureResponse.flatMap { httpResponse =>
      httpResponse.status match {

        case OK =>
          Future.successful(httpResponse.json)

        case NOT_FOUND =>
          Logger.warn(s"HttpHandler - No data can be found")
          Future.failed(new NotFoundException(httpResponse.body))

        case INTERNAL_SERVER_ERROR =>
          Logger.warn(s"HttpHandler - Internal Server Error received")
          Future.failed(new InternalServerException(httpResponse.body))

        case BAD_REQUEST =>
          Logger.warn(s"HttpHandler - Bad Request received")
          Future.failed(new BadRequestException(httpResponse.body))

        case LOCKED =>
          Logger.warn(s"HttpHandler - Locked received")
          Future.failed(new LockedException(httpResponse.body))

        case UNAUTHORIZED =>
          Logger.warn(s"HttpHandler - Unauthorized received")
          Future.successful(new UnauthorizedException(httpResponse.body))

        case _ =>
          Logger.warn(s"HttpHandler - Server error received")
          Future.failed(new HttpException(httpResponse.body, httpResponse.status))
      }
    }
  }

  def putToApi[I](
    url: String,
    data: I)(implicit hc: HeaderCarrier, rds: HttpReads[I], writes: Writes[I]): Future[HttpResponse] =
    http.PUT[I, HttpResponse](url, data).flatMap { httpResponse =>
      httpResponse.status match {

        case OK =>
          Future.successful(httpResponse)

        case NOT_FOUND =>
          Logger.warn(s"HttpHandler - No data can be found")
          Future.failed(new NotFoundException(httpResponse.body))

        case INTERNAL_SERVER_ERROR =>
          Logger.warn(s"HttpHandler - Internal Server Error received")
          Future.failed(new InternalServerException(httpResponse.body))

        case BAD_REQUEST =>
          Logger.warn(s"HttpHandler - Bad Request received")
          Future.failed(new BadRequestException(httpResponse.body))

        case _ =>
          Logger.warn(s"HttpHandler - Server error received")
          Future.failed(new HttpException(httpResponse.body, httpResponse.status))
      }
    }

  def postToApi[I](
    url: String,
    data: I)(implicit hc: HeaderCarrier, rds: HttpReads[I], writes: Writes[I]): Future[HttpResponse] =
    http.POST[I, HttpResponse](url, data) flatMap { httpResponse =>
      httpResponse status match {
        case OK | CREATED =>
          Future.successful(httpResponse)

        case _ =>
          Logger.warn(
            s"HttpHandler - Error received with status: ${httpResponse.status} and body: ${httpResponse.body}")
          Future.failed(new HttpException(httpResponse.body, httpResponse.status))
      }
    }

  def deleteFromApi(url: String)(implicit hc: HeaderCarrier, rds: HttpReads[HttpResponse]): Future[HttpResponse] =
    http.DELETE[HttpResponse](url) flatMap { httpResponse =>
      httpResponse status match {
        case OK | NO_CONTENT | ACCEPTED =>
          Future.successful(httpResponse)

        case _ =>
          Logger.warn(
            s"HttpHandler - Error received with status: ${httpResponse.status} and body: ${httpResponse.body}")
          Future.failed(new HttpException(httpResponse.body, httpResponse.status))
      }
    }

}
