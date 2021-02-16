/*
 * Copyright 2021 HM Revenue & Customs
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

package repositories

import javax.inject.{Inject, Singleton}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

case class DatedCacheMap(id: String, data: Map[String, JsValue], lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object DatedCacheMap {

  implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
  implicit val formats: Format[DatedCacheMap] = Json.format[DatedCacheMap]

  def apply(cacheMap: CacheMap): DatedCacheMap = DatedCacheMap(cacheMap.id, cacheMap.data)
}

@Singleton
class SessionRepository @Inject()(config: Configuration, component: ReactiveMongoComponent)(
  implicit ec: ExecutionContext)
    extends ReactiveRepository[DatedCacheMap, BSONObjectID](
      config.underlying.getString("appName"),
      component.mongoConnector.db,
      DatedCacheMap.formats) {

  val mongoLogger = Logger(this.getClass)

  val fieldName = "lastUpdated"
  val createdIndexName = "userAnswersExpiry"
  val expireAfterSeconds = "expireAfterSeconds"
  val timeToLiveInSeconds: Int = config.get[Int]("mongodb.timeToLiveInSeconds")

  checkIndex(fieldName, createdIndexName, timeToLiveInSeconds)

  private def checkIndex(field: String, indexName: String, ttl: Int): Future[Boolean] = {

    val indexes: Future[List[Index]] = collection.indexesManager.list()

    indexes.flatMap { indexSequence =>
      {

        val isIndexSet: Option[Boolean] = indexSequence.headOption.map { index =>
          val isIndex = index.name.contains(indexName)
          val isExpires = index.options.getAs[Int](expireAfterSeconds)
          val hasTtl = isExpires.contains(ttl)

          mongoLogger.info(s"Found index -> $isIndex = ${index.name}")
          mongoLogger.info(s"TTL matches config -> $hasTtl = $isExpires")

          isIndex && hasTtl
        }

        val dropped = isIndexSet match {
          case Some(false) =>
            mongoLogger.info(s"Dropping index and setting TTL to config value of $ttl")
            dropIndex(indexName)
          case _ => Future.successful(0)
        }

        for {
          _       <- dropped
          created <- createIndex(field, indexName, ttl)
        } yield {
          created
        }
      }
    }
  }

  private def dropIndex(indexName: String): Future[Int] =
    collection.indexesManager.drop(indexName)

  private def createIndex(field: String, indexName: String, ttl: Int): Future[Boolean] =
    collection.indexesManager.ensure(Index(
      Seq((field, IndexType.Ascending)),
      Some(indexName),
      options = BSONDocument(expireAfterSeconds -> ttl))) map { result =>
      {
        mongoLogger.info(s"set [$indexName] with value $ttl -> result : $result")
        result
      }
    } recover {
      case e =>
        mongoLogger.error("Failed to set TTL index", e)
        false
    }

  def upsert(cm: CacheMap): Future[Boolean] = {
    val selector = Json.obj("id" -> cm.id)
    val cmDocument = Json.toJson(DatedCacheMap(cm))
    val modifier = Json.obj("$set" -> cmDocument)

    collection.update(selector, modifier, upsert = true).map { lastError =>
      lastError.ok
    }
  }

  def get(id: String): Future[Option[CacheMap]] =
    collection.find(Json.obj("id" -> id), None).one[CacheMap]

  def remove(id: String): Future[Boolean] =
    collection.delete().one(Json.obj("id" -> id)).map { error =>
      error.ok
    }
}
