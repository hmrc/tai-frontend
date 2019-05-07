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

import com.google.inject.{Inject, Singleton}
import play.Logger
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.config.WSHttp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class HttpHandler @Inject()(val http: WSHttp) {

  val notFoundResponse = (response: HttpResponse) => {
    Logger.warn(s"HttpHandler - No DATA Found")
    throw new NotFoundException(response.body)
  }

 val internalServerErrorResponse = (response: HttpResponse) => {
   Logger.warn(s"HttpHandler - Internal Server error")
   throw new InternalServerException(response.body)
 }

  val badRequestResponse = (response: HttpResponse) => {
    Logger.warn(s"HttpHandler - Bad request exception")
    throw new BadRequestException(response.body)
  }

  val generalExceptionResponse = (response: HttpResponse) => {
    Logger.warn(s"HttpHandler - Error received with status: ${response.status} and body: ${response.body}")
    throw new HttpException(response.body, response.status)
  }


  def getFromApi(url: String)(implicit hc: HeaderCarrier): Future[JsValue] = {

    implicit val reads = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case Status.OK => response
          case Status.NOT_FOUND => notFoundResponse(response)
          case Status.INTERNAL_SERVER_ERROR => internalServerErrorResponse(response)
          case Status.BAD_REQUEST => badRequestResponse(response)
          case Status.LOCKED => {
            Logger.warn(s"HttpHandler - Locked received")
            throw new LockedException(response.body)
          }
          case _ => generalExceptionResponse(response)
        }
      }
    }

    http.GET[HttpResponse](url) map(_.json)

  }

  def putToApi[I](url: String, data: I)(implicit hc: HeaderCarrier, rds: HttpReads[I], writes: Writes[I]): Future[HttpResponse] = {

    implicit val reads = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case OK => response
          case NOT_FOUND => notFoundResponse(response)
          case INTERNAL_SERVER_ERROR => internalServerErrorResponse(response)
          case BAD_REQUEST => badRequestResponse(response)
          case _ => generalExceptionResponse(response)
        }
      }
    }

    http.PUT[I, HttpResponse](url, data)
  }

  def postToApi[I](url: String, data: I)(implicit hc: HeaderCarrier, writes: Writes[I]): Future[HttpResponse] = {

    implicit val rawHttpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case OK | CREATED => response
          case _ => generalExceptionResponse(response)
        }
      }
    }

    http.POST[I, HttpResponse](url, data)
  }

  def deleteFromApi(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    implicit val rawHttpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
      override def read(method: String, url: String, response: HttpResponse): HttpResponse = {
        response.status match {
          case OK | NO_CONTENT | ACCEPTED => response
          case _ => generalExceptionResponse(response)
        }
      }


    }
    http.DELETE[HttpResponse](url)
  }
}
