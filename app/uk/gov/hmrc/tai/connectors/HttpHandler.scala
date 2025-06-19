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
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{BadRequestException, _}

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps
import scala.util.{Failure, Success, Try}

class HttpHandler @Inject() (val http: HttpClientV2) extends HttpErrorFunctions with Logging {

  private val logErrorResponses: PartialFunction[Try[Either[UpstreamErrorResponse, HttpResponse]], Unit] = {
    case Success(Left(error))
        if Set(NOT_FOUND, LOCKED, UNPROCESSABLE_ENTITY, UNAUTHORIZED).contains(error.statusCode) =>
      logger.info(error.message)
    case Success(Left(error)) if error.statusCode >= 499 || error.statusCode == TOO_MANY_REQUESTS =>
      logger.error(error.message)
    case Success(Left(error))                                                                     =>
      logger.error(error.message, error)
    case Failure(exception: HttpException)                                                        =>
      logger.error(exception.message)
  }

  def httpClient: HttpClientV2 = http

  def read(
    response: Future[Either[UpstreamErrorResponse, HttpResponse]]
  )(implicit ec: ExecutionContext): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    EitherT(
      response andThen logErrorResponses recover { case exception: HttpException =>
        Left(UpstreamErrorResponse(exception.message, BAD_GATEWAY, BAD_GATEWAY))
      }
    )

  private val includeTimeOut: (Option[DurationInt], RequestBuilder) => RequestBuilder = (timeoutInSec, rb) =>
    timeoutInSec
      .fold(rb)(timeOut => rb.transform(_.withRequestTimeout(timeOut.seconds)))

  def getFromApiV2(url: String, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[JsValue] = {
    implicit val httpRds: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      def customRead(http: String, url: String, response: HttpResponse): HttpResponse =
        response.status match {
          case UNAUTHORIZED => response
          case _            =>
            handleResponseEither(http, url)(response)
              .fold(
                err =>
                  err.statusCode match {
                    case 400 => throw new BadRequestException(badRequestMessage("GET", url, response.body))
                    case 404 => throw new NotFoundException(notFoundMessage("GET", url, response.body))
                    case _   => throw err
                  },
                identity
              )
        }

      def read(http: String, url: String, res: HttpResponse): HttpResponse = customRead(http, url, res)
    }

    http
      .get(url"$url")(hc)
      .pipe(includeTimeOut(timeoutInSec, _))
      .execute[HttpResponse]
      .flatMap { httpResponse =>
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

  trait DataConverter[I] {
    def convert(data: I): JsValue
  }

  implicit val stringDataConverter: DataConverter[String] = new DataConverter[String] {
    def convert(data: String): JsValue = Json.toJson(data)
  }

  def putToApi[I](url: String, data: I, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    writes: Writes[I]
  ): Future[HttpResponse] =
    http
      .put(url"$url")(hc)
      .withBody(Json.toJson(data))
      .pipe(includeTimeOut(timeoutInSec, _))
      .execute[HttpResponse]
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK        =>
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

  def postToApi[I](url: String, data: I, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    writes: Writes[I]
  ): Future[HttpResponse] =
    http
      .post(url"$url")(hc)
      .withBody(Json.toJson(data))
      .pipe(includeTimeOut(timeoutInSec, _))
      .execute[HttpResponse]
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK | CREATED =>
            Future.successful(httpResponse)
          case _            =>
            logger.warn(
              s"HttpHandler - Error received with status: ${httpResponse.status} and body: ${httpResponse.body}"
            )
            Future.failed(new HttpException(httpResponse.body, httpResponse.status))
        }
      }

  def deleteFromApi(url: String, timeoutInSec: Option[DurationInt] = None)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[HttpResponse] =
    http
      .delete(url"$url")(hc)
      .pipe(includeTimeOut(timeoutInSec, _))
      .execute[HttpResponse]
      .flatMap { httpResponse =>
        httpResponse.status match {
          case OK | NO_CONTENT | ACCEPTED =>
            Future.successful(httpResponse)
          case _                          =>
            logger.warn(
              s"HttpHandler - Error received with status: ${httpResponse.status} and body: ${httpResponse.body}"
            )
            Future.failed(new HttpException(httpResponse.body, httpResponse.status))
        }
      }

}
