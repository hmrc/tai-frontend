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

package repository

import com.google.inject.Inject
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.UserAnswers

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Singleton

@Singleton
class JourneyCacheNewRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: ApplicationConfig,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[UserAnswers](
      collectionName = "user-answers",
      mongoComponent = mongoComponent,
      domainFormat = UserAnswers.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("sessionId", "nino"),
          IndexOptions()
            .unique(true)
            .name("sessionIdAndNino")
        ),
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(
              appConfig.sessionTimeoutInSeconds,
              TimeUnit.SECONDS
            )
        )
      )
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byIdAndNino(id: String, nino: String): Bson =
    Filters.and(
      Filters.equal("sessionId", id),
      Filters.equal("nino", nino)
    )

  def keepAlive(id: String, nino: String): Future[Boolean] =
    collection
      .updateOne(
        filter = byIdAndNino(id, nino),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)

  def get(id: String, nino: String): Future[Option[UserAnswers]] =
    keepAlive(id, nino).flatMap { _ =>
      collection
        .find(byIdAndNino(id, nino))
        .headOption()
    }

  def set(answers: UserAnswers): Future[Boolean] = {
    val updatedAnswers = answers copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byIdAndNino(updatedAnswers.sessionId, updatedAnswers.nino),
        replacement = updatedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  def clear(id: String, nino: String): Future[Boolean] =
    collection
      .deleteOne(byIdAndNino(id, nino))
      .toFuture()
      .map(_ => true)
}
