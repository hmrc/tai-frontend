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

package uk.gov.hmrc.tai.service.journeyCache

import akka.Done
import cats.data.EitherT
import cats.implicits.catsStdInstancesForFuture
import play.api.Logging
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiCacheError, TaiResponse, TaiSuccessResponse}
import uk.gov.hmrc.tai.util.constants.journeyCache._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyCacheService @Inject() (val journeyName: String, journeyCacheConnector: JourneyCacheConnector)
    extends Logging {

  def currentValue(key: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[String]] =
    currentValueAs[String](key, identity)

  def currentValueAsInt(key: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Int]] =
    currentValueAs[Int](key, string => string.toInt)

  def currentValueAsBoolean(key: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    currentValueAs[Boolean](key, string => string.toBoolean)

  def currentValueAsDate(key: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[LocalDate]] =
    currentValueAs[LocalDate](key, string => LocalDate.parse(string))

  def mandatoryJourneyValue(key: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, String] =
    mandatoryJourneyValueAs[String](key, identity)

  def mandatoryJourneyValueAsInt(key: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, Int] =
    mandatoryJourneyValueAs[Int](key, string => string.toInt)

  def mandatoryValueAsBoolean(key: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, Boolean] =
    mandatoryJourneyValueAs[Boolean](key, string => string.toBoolean)

  def mandatoryValueAsDate(key: String)(implicit hc: HeaderCarrier): EitherT[Future, UpstreamErrorResponse, LocalDate] =
    mandatoryJourneyValueAs[LocalDate](key, string => LocalDate.parse(string))

  def mandatoryJourneyValues(
    keys: String*
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Either[String, Seq[String]]] =
    for {
      cache <- currentCache
    } yield mappedMandatory(cache, keys)

  def optionalValues(
    keys: String*
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Seq[Option[String]]] =
    currentCache.map(cache => mappedOptional(cache, keys).toList)

  def collectedJourneyValues(mandatoryJourneyValues: Seq[String], optionalValues: Seq[String])(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Either[String, (Seq[String], Seq[Option[String]])]] =
    for {
      cache <- currentCache
    } yield mappedMandatory(cache, mandatoryJourneyValues).map { mandatoryResult =>
      val optionalResult = mappedOptional(cache, optionalValues)
      (mandatoryResult, optionalResult)
    }

  private def mappedMandatory(
    cache: Map[String, String],
    mandatoryJourneyValues: Seq[String]
  ): Either[String, Seq[String]] = {

    val allPresentValues = mandatoryJourneyValues flatMap { key =>
      cache.get(key) match {
        case Some(str) if str.trim.nonEmpty => Some(str)
        case _ =>
          logger.warn(s"The mandatory value under key '$key' was not found in the journey cache for '$journeyName'")
          None
      }
    }

    if (allPresentValues.size == mandatoryJourneyValues.size) Right(allPresentValues)
    else Left("Mandatory values missing from cache")
  }

  private def mappedOptional(cache: Map[String, String], optionalValues: Seq[String]): Seq[Option[String]] =
    optionalValues map { key =>
      cache.get(key) match {
        case found @ Some(str) if str.trim.nonEmpty => found
        case _                                      => None
      }
    }

  def currentCache(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector
      .currentCache(journeyName)
      .fold(
        _ => Map.empty[String, String],
        result =>
          if (result.status == NO_CONTENT) {
            Map.empty[String, String]
          } else {
            result.json.as[Map[String, String]]
          }
      )

  def currentValueAs[T](key: String, convert: String => T)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Option[T]] =
    journeyCacheConnector.currentValueAs[T](journeyName, key, convert).getOrElse(None)

  def mandatoryJourneyValueAs[T](key: String, convert: String => T)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, UpstreamErrorResponse, T] =
    journeyCacheConnector.mandatoryJourneyValueAs[T](journeyName, key, convert)

  def cache(key: String, value: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector
      .cache(journeyName, Map(key -> value))
      .getOrElse(
        Map.empty[String, String]
      ) // TODO - Consult other devs to establish correct behaviour. This method of caching is very flawed

  def cache(data: Map[String, String])(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector
      .cache(journeyName, data)
      .getOrElse(
        Map.empty[String, String]
      ) // TODO - Consult other devs to establish correct behaviour. This method of caching is very flawed

  def flush()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Done] =
    journeyCacheConnector
      .flush(journeyName)
      .getOrElse(
        Done
      ) // TODO - Consult other devs to establish correct behaviour. This method of caching is very flawed

  def flushWithEmpId(empId: Int)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Done] =
    journeyCacheConnector
      .flushWithEmpId(journeyName, empId)
      .getOrElse(
        Done
      ) // TODO - Consult other devs to establish correct behaviour. This method of caching is very flawed

  def delete()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[TaiResponse] =
    journeyCacheConnector
      .delete(UpdateIncomeConstants.DeleteJourneyKey)
      .fold(
        error => TaiCacheError(error.message),
        _ => TaiSuccessResponse
      ) // TODO - Investigate the need for custom error trait
}
