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

import controllers.auth.DataRequest
import org.apache.pekko.Done
import pages.QuestionPage
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.connectors.responses.TaiResponse
import uk.gov.hmrc.tai.util.constants.journeyCache._

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyCacheService @Inject() (val journeyName: String, journeyCacheConnector: JourneyCacheConnector)
    extends Logging {

  def currentValue(key: String)(implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent],
    ec: ExecutionContext,
    reads: Reads[String]
  ): Future[Option[String]] =
    currentValueAs[String](key, identity)

  def currentValueAsInt(
    key: String
  )(implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent],
    ec: ExecutionContext,
    reads: Reads[Int]
  ): Future[Option[Int]] =
    currentValueAs[Int](key, string => string.toInt)

  def currentValueAsBoolean(
    key: String
  )(implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent],
    ec: ExecutionContext,
    reads: Reads[Boolean]
  ): Future[Option[Boolean]] =
    currentValueAs[Boolean](key, string => string.toBoolean)

  def currentValueAsDate(
    key: String
  )(implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent],
    ec: ExecutionContext,
    reads: Reads[LocalDate]
  ): Future[Option[LocalDate]] =
    currentValueAs[LocalDate](key, string => LocalDate.parse(string))

  def mandatoryJourneyValue(
    key: String
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Either[String, String]] =
    mandatoryJourneyValueAs[String](key, identity)

  def mandatoryJourneyValueAsInt(
    key: String
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Either[String, Int]] =
    mandatoryJourneyValueAs[Int](key, string => string.toInt)

  def mandatoryValueAsBoolean(
    key: String
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Either[String, Boolean]] =
    mandatoryJourneyValueAs[Boolean](key, string => string.toBoolean)

  def mandatoryValueAsDate(
    key: String
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Either[String, LocalDate]] =
    mandatoryJourneyValueAs[LocalDate](key, string => LocalDate.parse(string))

  def mandatoryJourneyValues(
    keys: Seq[String]
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Either[String, Seq[String]]] =
    for {
      cache <- currentCache
    } yield mappedMandatory(cache, keys)

  def optionalValues(
    keys: Seq[String]
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Seq[Option[String]]] =
    currentCache.map(cache => mappedOptional(cache, keys).toList)

  def collectedJourneyValues(mandatoryJourneyValues: Seq[String], optionalValues: Seq[String])(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
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
  )(implicit request: DataRequest[AnyContent]): Either[String, Seq[String]] = {
    val allPresentValues = mandatoryJourneyValues flatMap { key =>
      getValue(key, cache) match {
        case Some(str) if str.trim.nonEmpty => Some(str)
        case _ =>
          logger.warn(s"The mandatory value under key '$key' was not found in the journey cache for '$journeyName'")
          None
      }
    }

    if (allPresentValues.size == mandatoryJourneyValues.size) {
      Right(allPresentValues)
    } else {
      Left("Mandatory values missing from cache")
    }
  }

  private def getValue(key: String, cache: Map[String, String])(implicit
    request: DataRequest[AnyContent]
  ): Option[String] =
    cache.get(key) match {
      case x @ Some(_) => x
      case _ =>
        val a = request.userAnswers.data \ key
        val result = a.toOption map {
          case JsString(s)  => s
          case JsNumber(n)  => n.toInt.toString
          case JsBoolean(b) => b.toString
          case _            => throw new RuntimeException("Invalid Value Type")
        }
        result
    }

  private def mappedOptional(cache: Map[String, String], optionalValues: Seq[String]): Seq[Option[String]] =
    optionalValues map { key =>
      cache.get(key) match {
        case found @ Some(str) if str.trim.nonEmpty => found
        case _                                      => None
      }
    }

  private def valueOrElseUA[A](oldCacheValue: Option[A], key: String)(implicit
    request: DataRequest[AnyContent],
    reads: Reads[A]
  ): Option[A] =
    oldCacheValue match {
      case x @ Some(_) => x
      case _ =>
        val page = new QuestionPage[A] {
          override def path: JsPath = JsPath \ key
        }
        request.userAnswers.get(page)
    }

  def currentCache(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Map[String, String]] =
    journeyCacheConnector.currentCache(journeyName).map {
      case m if m.isEmpty =>
        request.userAnswers.data
          .as[Map[String, JsValue]]
          .view
          .mapValues {
            case JsString(s)  => s
            case JsNumber(s)  => s.toInt.toString
            case JsBoolean(s) => s.toString
            case e            => throw new RuntimeException("Error" + e)
          }
          .toMap
      case m => m
    }

  def currentValueAs[T](key: String, convert: String => T)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: DataRequest[AnyContent],
    reads: Reads[T]
  ): Future[Option[T]] =
    journeyCacheConnector.currentValueAs[T](journeyName, key, convert).map(x => valueOrElseUA(x, key))

  def mandatoryJourneyValueAs[T](key: String, convert: String => T)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: DataRequest[AnyContent],
    reads: Reads[T]
  ): Future[Either[String, T]] =
    journeyCacheConnector.mandatoryJourneyValueAs[T](journeyName, key, convert).flatMap {
      case Right(value) =>
        Future.successful(Right(value))

      case Left(errorMessage) =>
        Future.successful {
          valueOrElseUA(None, key) match {
            case Some(value) => Right(value)
            case None        => Left(errorMessage)
          }
        }
    }

  def cache(key: String, value: String)(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector.cache(journeyName, Map(key -> value))

  def cache(data: Map[String, String])(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector.cache(journeyName, data)

  def flush()(implicit hc: HeaderCarrier): Future[Done] =
    journeyCacheConnector.flush(journeyName)

  def flushWithEmpId(empId: Int)(implicit hc: HeaderCarrier): Future[Done] =
    journeyCacheConnector.flushWithEmpId(journeyName, empId)

  def delete()(implicit hc: HeaderCarrier): Future[TaiResponse] =
    journeyCacheConnector.delete(UpdateIncomeConstants.DeleteJourneyKey)
}
