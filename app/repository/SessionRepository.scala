package repository

import com.google.inject.Inject
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, ReplaceOptions, Updates}
import uk.gov.hmrc.tai.config.ApplicationConfig
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.tai.model.UserAnswers

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionRepository @Inject()(
                                   mongoComponent: MongoComponent,
                                   appConfig: ApplicationConfig,
                                   clock: Clock
                                 )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UserAnswers](
    collectionName = "user-answers",
    mongoComponent = mongoComponent,
    domainFormat   = UserAnswers.format,
    indexes        = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(appConfig.sessionTimeoutInSeconds, TimeUnit.SECONDS) // TODO - Check if `.sessionTimeoutInSeconds` is the correct time in seconds
      )
    )
  ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byId(id: String): Bson = Filters.equal("_id", id)

  def keepAlive(id: String): Future[Boolean] =
    collection
      .updateOne(
        filter = byId(id),
        update = Updates.set("lastUpdated", Instant.now(clock)),
      )
      .toFuture
      .map(_ => true)

  def get(id: String): Future[Option[UserAnswers]] =
    keepAlive(id).flatMap {
      _ =>
        collection
          .find(byId(id))
          .headOption
    }

  def set(answers: UserAnswers): Future[Boolean] = {

    val updatedAnswers = answers copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter      = byId(updatedAnswers.id),
        replacement = updatedAnswers,
        options     = ReplaceOptions().upsert(true)
      )
      .toFuture
      .map(_ => true)
  }

  def clear(id: String): Future[Boolean] =
    collection
      .deleteOne(byId(id))
      .toFuture
      .map(_ => true)
}