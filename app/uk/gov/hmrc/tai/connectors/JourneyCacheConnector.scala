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

import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyCacheConnector @Inject() (httpHandler: HttpHandler, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  val serviceUrl: String = servicesConfig.baseUrl("tai")

  def cacheUrl(journeyName: String): String = s"$serviceUrl/tai/journey-cache/$journeyName"

  def currentCache(journeyName: String)(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    httpHandler.getFromApiV2(cacheUrl(journeyName)).map(_.as[Map[String, String]]) recover {
      case e: HttpException if e.responseCode == NO_CONTENT => Map.empty[String, String]
    }

  def currentValueAs[T](journeyName: String, key: String, convert: String => T)(implicit
    hc: HeaderCarrier
  ): Future[Option[T]] = {
    val url = s"${cacheUrl(journeyName)}/values/$key"
    httpHandler.getFromApiV2(url).map(value => Some(convert(value.as[String]))) recover {
      case e: HttpException if e.responseCode == NO_CONTENT => None
    }
  }

  def mandatoryJourneyValueAs[T](journeyName: String, key: String, convert: String => T)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, T]] = {
    val url = s"${cacheUrl(journeyName)}/values/$key"
    httpHandler.getFromApiV2(url).map(value => Right(convert(value.as[String]))) recover {
      case e: HttpException if e.responseCode == NO_CONTENT =>
        val errorMessage = s"The mandatory value under key '$key' was not found in the journey cache for '$journeyName'"
        logger.warn(errorMessage)
        Left(errorMessage)
    }
  }

  def cache(journeyName: String, data: Map[String, String])(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    httpHandler
      .postToApi(
        cacheUrl(journeyName),
        data
      )
      .map(_.json.as[Map[String, String]])

  def flush(journeyName: String)(implicit hc: HeaderCarrier): Future[Done] =
    httpHandler.deleteFromApi(cacheUrl(journeyName)).map(_ => Done)

  def flushWithEmpId(journeyName: String, empId: Int)(implicit hc: HeaderCarrier): Future[Done] =
    httpHandler.deleteFromApi(cacheUrl(s"$journeyName/$empId")).map(_ => Done)

  private def testOnlyCacheUrl(journeyName: String): String = s"$serviceUrl/tai/test-only/journey-cache/$journeyName"

  def delete(journeyName: String)(implicit hc: HeaderCarrier): Future[TaiResponse] =
    httpHandler.deleteFromApi(testOnlyCacheUrl(journeyName)).map(_ => TaiSuccessResponse)
}
