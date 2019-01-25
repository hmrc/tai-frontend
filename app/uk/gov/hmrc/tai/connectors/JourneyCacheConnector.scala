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

import com.google.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyCacheConnector @Inject() (httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def cacheUrl(journeyName: String) = s"$serviceUrl/tai/journey-cache/$journeyName"

  def currentCache(journeyName: String)(implicit hc: HeaderCarrier): Future[Map[String,String]] = {
    httpHandler.getFromApi(cacheUrl(journeyName)).map(_.as[Map[String,String]]) recover {
      case e:NotFoundException => Map.empty[String, String]
    }
  }

  def currentValueAs[T](journeyName: String, key: String, convert: String => T)(implicit hc: HeaderCarrier): Future[Option[T]] = {
    val url = s"${cacheUrl(journeyName)}/values/$key"
    httpHandler.getFromApi(url).map(value => Some(convert(value.as[String]))) recover {
      case e:NotFoundException => None
    }
  }

  def mandatoryValueAs[T](journeyName: String, key: String, convert: String => T)(implicit hc: HeaderCarrier): Future[T] = {
    val url = s"${cacheUrl(journeyName)}/values/$key"
    httpHandler.getFromApi(url).map(value => convert(value.as[String])) recover {
      case e:NotFoundException => throw new RuntimeException(s"The mandatory value under key '$key' was not found in the journey cache for '$journeyName'")
    }
  }

  def cache(journeyName: String, data: Map[String,String])(implicit hc: HeaderCarrier): Future[Map[String,String]] = {
    httpHandler.postToApi(
      cacheUrl(journeyName), data
    ).map(_.json.as[Map[String,String]])
  }

  def flush(journeyName: String)(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    httpHandler.deleteFromApi(cacheUrl(journeyName)).map(_ => TaiSuccessResponse)
  }
}
