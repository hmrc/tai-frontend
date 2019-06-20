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

package uk.gov.hmrc.tai.service.journeyCache

import javax.inject.Inject
import org.joda.time.LocalDate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.connectors.responses.TaiResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneyCacheService @Inject() (val journeyName: String,
                                     journeyCacheConnector: JourneyCacheConnector) {


  def currentValue(key: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    currentValueAs[String](key, identity)
  }

  def currentValueAsInt(key: String)(implicit hc: HeaderCarrier): Future[Option[Int]] = {
    currentValueAs[Int](key, string => string.toInt)
  }

  def currentValueAsBoolean(key: String)(implicit hc: HeaderCarrier): Future[Option[Boolean]] = {
    currentValueAs[Boolean](key, string => string.toBoolean)
  }

  def currentValueAsDate(key: String)(implicit hc: HeaderCarrier): Future[Option[LocalDate]] = {
    currentValueAs[LocalDate](key, string => LocalDate.parse(string))
  }

  def mandatoryValue(key: String)(implicit hc: HeaderCarrier): Future[String] = {
    mandatoryValueAs[String](key, identity)
  }

  def mandatoryValueAsInt(key: String)(implicit hc: HeaderCarrier): Future[Int] = {
    mandatoryValueAs[Int](key, string => string.toInt)
  }

  def mandatoryValueAsBoolean(key: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    mandatoryValueAs[Boolean](key, string => string.toBoolean)
  }

  def mandatoryValueAsDate(key: String)(implicit hc: HeaderCarrier): Future[LocalDate] = {
    mandatoryValueAs[LocalDate](key, string => LocalDate.parse(string))
  }

  def mandatoryValues(keys : String*)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    for {
      cache <- currentCache
    } yield {
      mappedMandatory(cache, keys)
    }
  }

  def optionalValues(keys: String*)(implicit hc: HeaderCarrier): Future[Seq[Option[String]]] = {
    for {
      cache <- currentCache
    } yield {
      mappedOptional(cache, keys)
    }
  }

  def collectedValues(mandatoryValues: Seq[String], optionalValues: Seq[String])(implicit hc: HeaderCarrier): Future[(Seq[String], Seq[Option[String]])] = {
    for {
      cache <- currentCache
    } yield {
      val mandatoryResult = mappedMandatory(cache, mandatoryValues)
      val optionalResult = mappedOptional(cache, optionalValues)
      (mandatoryResult, optionalResult)
    }
  }

  private def mappedMandatory(cache: Map[String, String], mandatoryValues: Seq[String]): Seq[String] = {
    mandatoryValues map { key =>
      cache.get(key) match {
        case Some(str) if !str.trim.isEmpty => str
        case _ => throw new RuntimeException(s"The mandatory value under key '$key' was not found in the journey cache for '$journeyName'")
      }
    }
  }

  private def mappedOptional(cache: Map[String, String], optionalValues: Seq[String]): Seq[Option[String]] = {
    optionalValues map { key =>
      cache.get(key) match {
        case found @ Some(str) if !str.trim.isEmpty => found
        case _ => None
      }
    }
  }

  def currentCache(implicit hc: HeaderCarrier): Future[Map[String,String]] = {
    journeyCacheConnector.currentCache(journeyName)
  }

  def currentValueAs[T](key: String, convert: String => T)(implicit hc: HeaderCarrier): Future[Option[T]] = {
    journeyCacheConnector.currentValueAs[T](journeyName, key, convert)
  }

  def mandatoryValueAs[T](key: String, convert: String => T)(implicit hc: HeaderCarrier): Future[T] = {
    journeyCacheConnector.mandatoryValueAs[T](journeyName, key, convert)
  }

  def cache(key: String, value: String)(implicit hc: HeaderCarrier): Future[Map[String,String]] = {
    journeyCacheConnector.cache(journeyName, Map(key->value))
  }

  def cache(data: Map[String,String])(implicit hc: HeaderCarrier): Future[Map[String,String]] = {
    journeyCacheConnector.cache(journeyName, data)
  }

  def flush()(implicit hc: HeaderCarrier): Future[TaiResponse] = {
    journeyCacheConnector.flush(journeyName)
  }
}
