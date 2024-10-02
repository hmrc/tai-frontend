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
import play.api.libs.json.{JsNumber, JsPath, JsString}
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

  def currentValue(key: String)(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Option[String]] =
    currentValueAs[String](key, identity) // if not found in cache , look in userANswers

  def currentValueAsInt(
    key: String
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Option[Int]] =
    currentValueAs[Int](key, string => string.toInt)

  def currentValueAsBoolean(
    key: String
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Option[Boolean]] =
    currentValueAs[Boolean](key, string => string.toBoolean)

  def currentValueAsDate(
    key: String
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Option[LocalDate]] =
    currentValueAs[LocalDate](key, string => LocalDate.parse(string))

  def mandatoryJourneyValue(
    key: String
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Either[String, String]] =
    mandatoryJourneyValueAs[String](key, identity)

  def mandatoryJourneyValueAsInt(
    key: String
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Either[String, Int]] =
    mandatoryJourneyValueAs[Int](key, string => string.toInt)

  def mandatoryValueAsBoolean(
    key: String
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Either[String, Boolean]] =
    mandatoryJourneyValueAs[Boolean](key, string => string.toBoolean)

  def mandatoryValueAsDate(
    key: String
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Either[String, LocalDate]] =
    mandatoryJourneyValueAs[LocalDate](key, string => LocalDate.parse(string))

  //
  def mandatoryJourneyValues(
    keys: Seq[String]
  )(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext,
    request: DataRequest[AnyContent]
  ): Future[Either[String, Seq[String]]] = {
    println("\n ----> INSIDE JourneyCachService.mandatoryJourneyValues--- ")
    println("\n ----->  KEYS :" + keys)
    for {
      cache <- currentCache
    } yield mappedMandatory(cache, keys)
  }

  def optionalValues(
    keys: Seq[String]
  )(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Seq[Option[String]]] =
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

    println("\n ----->  INSIDE mappedMandatory ---")
    println("\n ----->  cache :" + cache)
    println("\n ----->  mandatoryJourneyValue KEYS :" + mandatoryJourneyValues)
    val allPresentValues = mandatoryJourneyValues flatMap { key =>
      getValue(key, cache) match {
        case Some(str) if str.trim.nonEmpty => Some(str)
        case _ =>
          println(
            s"\nJourneyCacheService.mappedMandatory------> The mandatory value under key '$key' was not found in the journey cache for '$journeyName'"
          )
          None
      }
    }

    if (allPresentValues.size == mandatoryJourneyValues.size) Right(allPresentValues)
    else {
      println("\n---------> The mandatory values MISSING")
      Left("Mandatory values missing from cache")
    }
  }

  private def getValue(key: String, cache: Map[String, String])(implicit
    request: DataRequest[AnyContent]
  ): Option[String] = {
    println("\n INSIDE  getValue ------ key " + key)
    cache.get(key) match {
      case x @ Some(_) => x
      case _ =>
        println("\n ----> Key Not Found in JourneyCache. Trying to retrive from userAnswers")
        println("\n -----> User Answers --- : " + request.userAnswers)
        val a = request.userAnswers.data \ key
        val result = a.toOption map {
          case JsString(x) => x
          case JsNumber(y) => y.toInt.toString
          case _           => throw new RuntimeException("Invalid Type :")
        }
        result
    }
  }

  private def mappedOptional(cache: Map[String, String], optionalValues: Seq[String]): Seq[Option[String]] =
    optionalValues map { key =>
      cache.get(key) match {
        case found @ Some(str) if str.trim.nonEmpty => found
        case _                                      => None
      }
    }

  def currentCache(implicit hc: HeaderCarrier): Future[Map[String, String]] =
    journeyCacheConnector.currentCache(journeyName)

  def currentValueAs[T](key: String, convert: String => T)(implicit hc: HeaderCarrier): Future[Option[T]] =
    journeyCacheConnector.currentValueAs[T](journeyName, key, convert)

  def mandatoryJourneyValueAs[T](key: String, convert: String => T)(implicit
    hc: HeaderCarrier
  ): Future[Either[String, T]] =
    journeyCacheConnector.mandatoryJourneyValueAs[T](journeyName, key, convert)

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
