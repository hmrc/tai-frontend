/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service.journeyCache

import javax.inject.Inject
import java.time.LocalDate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.connectors.responses.TaiResponse
import play.api.Logging
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyCacheService @Inject()(val journeyName: String, journeyCacheConnector: JourneyCacheConnector)
    extends Logging {

  def currentValue(key: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    currentValueAs[String](key, identity)

  def currentValueAsInt(key: String)(implicit hc: HeaderCarrier): Future[Option[Int]] =
    currentValueAs[Int](key, string => string.toInt)

  def currentValueAsBoolean(key: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    currentValueAs[Boolean](key, string => string.toBoolean)

  def currentValueAsDate(key: String)(implicit hc: HeaderCarrier): Future[Option[LocalDate]] =
    currentValueAs[LocalDate](key, string => LocalDate.parse(string))

  def mandatoryJourneyValue(key: String)(implicit hc: HeaderCarrier): Future[Either[String, String]] =
    mandatoryJourneyValueAs[String](key, identity)

  def mandatoryJourneyValueAsInt(key: String)(implicit hc: HeaderCarrier): Future[Either[String, Int]] =
    mandatoryJourneyValueAs[Int](key, string => string.toInt)

  def mandatoryValueAsBoolean(key: String)(implicit hc: HeaderCarrier): Future[Either[String, Boolean]] =
    mandatoryJourneyValueAs[Boolean](key, string => string.toBoolean)

  def mandatoryValueAsDate(key: String)(implicit hc: HeaderCarrier): Future[Either[String, LocalDate]] =
    mandatoryJourneyValueAs[LocalDate](key, string => LocalDate.parse(string))

  def mandatoryJourneyValues(keys: String*)(implicit hc: HeaderCarrier): Future[Either[String, Seq[String]]] =
    for {
      cache <- currentCache
    } yield {
      mappedMandatory(cache, keys)
    }

  def optionalValues(keys: String*)(implicit hc: HeaderCarrier): Future[Seq[Option[String]]] =
    for {
      cache <- currentCache
    } yield {
      mappedOptional(cache, keys)
    }

  def collectedJourneyValues(mandatoryJourneyValues: Seq[String], optionalValues: Seq[String])(
    implicit hc: HeaderCarrier): Future[Either[String, (Seq[String], Seq[Option[String]])]] =
    for {
      cache <- currentCache
    } yield {
      mappedMandatory(cache, mandatoryJourneyValues).map { mandatoryResult =>
        val optionalResult = mappedOptional(cache, optionalValues)
        (mandatoryResult, optionalResult)
      }
    }

  private def mappedMandatory(
    cache: Map[String, String],
    mandatoryJourneyValues: Seq[String]): Either[String, Seq[String]] = {

    val allPresentValues = mandatoryJourneyValues flatMap { key =>
      cache.get(key) match {
        case Some(str) if str.trim.nonEmpty => Some(str)
        case _ => {
          logger.warn(s"The mandatory value under key '$key' was not found in the journey cache for '$journeyName'")
          None
        }
      }
    }

    if (allPresentValues.size == mandatoryJourneyValues.size) Right(allPresentValues)
    else Left("Mandatory values missing from cache")
  }

  @deprecated("Use mappedMandatory", "0.576.0")
  private def mappedMandatoryDeprecated(cache: Map[String, String], mandatoryJourneyValues: Seq[String]): Seq[String] =
    mandatoryJourneyValues map { key =>
      cache.get(key) match {
        case Some(str) if !str.trim.isEmpty => str
        case _ =>
          throw new RuntimeException(
            s"The mandatory value under key '$key' was not found in the journey cache for '$journeyName'")
      }
    }

  private def mappedOptional(cache: Map[String, String], optionalValues: Seq[String]): Seq[Option[String]] =
    optionalValues map { key =>
      cache.get(key) match {
        case found @ Some(str) if !str.trim.isEmpty => found
        case _                                      => None
      }
    }

  def currentCache(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector.currentCache(journeyName)

  def currentValueAs[T](key: String, convert: String => T)(implicit hc: HeaderCarrier): Future[Option[T]] =
    journeyCacheConnector.currentValueAs[T](journeyName, key, convert)

  def mandatoryJourneyValueAs[T](key: String, convert: String => T)(
    implicit hc: HeaderCarrier): Future[Either[String, T]] =
    journeyCacheConnector.mandatoryJourneyValueAs[T](journeyName, key, convert)

  def cache(key: String, value: String)(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector.cache(journeyName, Map(key -> value))

  def cache(data: Map[String, String])(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector.cache(journeyName, data)

  def flush()(implicit hc: HeaderCarrier): Future[TaiResponse] =
    journeyCacheConnector.flush(journeyName)
}
