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
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.BodyWritable
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{BadRequestException, _}

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag
import scala.util.chaining.scalaUtilChainingOps

class HttpHandler @Inject() (val http: HttpClientV2) extends HttpErrorFunctions with Logging {

  def read(
    response: Future[Either[UpstreamErrorResponse, HttpResponse]]
  )(implicit executionContext: ExecutionContext): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
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

  def getFromApiV2(url: String, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[JsValue] = {
    implicit val httpRds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      def customRead(http: String, url: String, response: HttpResponse): HttpResponse =
        response.status match {
          case UNAUTHORIZED => response
          case _            => handleResponse(http, url)(response)
        }

      def read(http: String, url: String, res: HttpResponse): HttpResponse = customRead(http, url, res)
    }

    val httpCall = http.get(url"$url")

    val transformedHttpCall =
      timeoutInSec.fold(httpCall)(timeOut => httpCall.transform(_.withRequestTimeout(timeOut.seconds)))

    val futureResponse = transformedHttpCall.execute[HttpResponse]
    futureResponse.flatMap { httpResponse =>
      httpResponse.status match {

        case OK =>
          Future.successful(httpResponse.json)

        case NOT_FOUND =>
          logger.warn(s"HttpHandler - No data can be found")
          Future.failed(new NotFoundException(httpResponse.body))

        case INTERNAL_SERVER_ERROR =>
          logger.warn(s"HttpHandler - Internal Server Error received")
          Future.failed(new InternalServerException(httpResponse.body))

        case BAD_REQUEST =>
          logger.warn(s"HttpHandler - Bad Request received")
          Future.failed(new BadRequestException(httpResponse.body))

        case LOCKED =>
          logger.warn(s"HttpHandler - Locked received")
          Future.failed(new LockedException(httpResponse.body))

        case UNAUTHORIZED =>
          logger.warn(s"HttpHandler - Unauthorized received")
          Future.failed(new UnauthorizedException(httpResponse.body))

        case _ =>
          logger.warn(s"HttpHandler - Server error received")
          Future.failed(new HttpException(httpResponse.body, httpResponse.status))
      }
    }
  }

  def putToApi[I: TypeTag](url: String, data: I, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    jsValueBodyWritable: BodyWritable[I]
  ): Future[HttpResponse] = {

    val httpCall = http.put(url"$url")(hc).withBody(data)

    val transformedHttpCall =
      timeoutInSec.fold(httpCall)(timeOut => httpCall.transform(_.withRequestTimeout(timeOut.seconds)))

    transformedHttpCall
      .execute[HttpResponse]
      .flatMap { httpResponse =>
        httpResponse.status match {

          case OK =>
            Future.successful(httpResponse)

          case NOT_FOUND =>
            logger.warn(s"HttpHandler - No data can be found")
            Future.failed(new NotFoundException(httpResponse.body))

          case INTERNAL_SERVER_ERROR =>
            logger.warn(s"HttpHandler - Internal Server Error received")
            Future.failed(new InternalServerException(httpResponse.body))

          case BAD_REQUEST =>
            logger.warn(s"HttpHandler - Bad Request received")
            Future.failed(new BadRequestException(httpResponse.body))

          case _ =>
            logger.warn(s"HttpHandler - Server error received")
            Future.failed(new HttpException(httpResponse.body, httpResponse.status))
        }
      }
  }

  /*
  def postToApi[I](url: String, data: I)(implicit
    hc: HeaderCarrier,
    writes: Writes[I],
    executionContext: ExecutionContext
  ): Future[HttpResponse] =
    http.POST[I, HttpResponse](url, data) flatMap { httpResponse =>
      httpResponse.status match {
        case OK | CREATED =>
          Future.successful(httpResponse)

        case _ =>
          logger.warn(
            s"HttpHandler - Error received with status: ${httpResponse.status} and body: ${httpResponse.body}"
          )
          Future.failed(new HttpException(httpResponse.body, httpResponse.status))
      }
    }
   */

  def postToApi[I: TypeTag](url: String, data: I, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    writes: Writes[I]
  ): Future[HttpResponse] = {
    def includeTimeOut: RequestBuilder => RequestBuilder = rb =>
      timeoutInSec
        .fold(rb)(timeOut => rb.transform(_.withRequestTimeout(timeOut.seconds)))

    http
      .post(url"$url")(hc)
      .withBody(Json.toJson(data))
      .pipe(includeTimeOut)
      .execute[HttpResponse]
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK | CREATED =>
            Future.successful(httpResponse)
          case _ =>
            logger.warn(
              s"HttpHandler - Error received with status: ${httpResponse.status} and body: ${httpResponse.body}"
            )
            Future.failed(new HttpException(httpResponse.body, httpResponse.status))
        }
      }
  }

  def deleteFromApi(url: String, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[HttpResponse] = {

    val httpCall = http.delete(url"$url")

    val transformedHttpCall =
      timeoutInSec.fold(httpCall)(timeOut => httpCall.transform(_.withRequestTimeout(timeOut.seconds)))

    transformedHttpCall
      .execute[HttpResponse]
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK | NO_CONTENT | ACCEPTED =>
            Future.successful(httpResponse)
          case _ =>
            logger.warn(
              s"HttpHandler - Error received with status: ${httpResponse.status} and body: ${httpResponse.body}"
            )
            Future.failed(new HttpException(httpResponse.body, httpResponse.status))
        }
      }
  }

}
