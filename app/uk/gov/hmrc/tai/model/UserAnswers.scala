/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model

import pages.QuestionPage
import play.api.libs.Files.logger
import play.api.libs.json._
import queries.{Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
  sessionId: String,
  nino: String,
  data: JsObject = Json.obj(),
  lastUpdated: Instant = Instant.now
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def set[A](page: Settable[A], value: A)(implicit writes: Writes[A]): Try[UserAnswers] = {
    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors)       =>
        logger.error(s"Failed to set value at path ${page.path}: $errors. Value: $value")
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers: UserAnswers = UserAnswers(sessionId, nino, d)
      page.cleanup(updatedAnswers)
    }
  }

  def remove[A](page: QuestionPage[A]): UserAnswers =
    data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) => this copy (data = jsValue)
      case JsError(_)            => this
    }

  def setOrException[A](page: Settable[A], value: A)(implicit writes: Writes[A]): UserAnswers =
    set(page, value) match {
      case Success(ua) => ua
      case Failure(ex) => throw ex
    }
}

object UserAnswers {

  val reads: Reads[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").read[String] and
        (__ \ "nino").read[String] and
        (__ \ "data").read[JsObject] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(UserAnswers.apply _)
  }

  val writes: OWrites[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").write[String] and
        (__ \ "nino").write[String] and
        (__ \ "data").write[JsObject] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    )(ua => Tuple4(ua.sessionId, ua.nino, ua.data, ua.lastUpdated))
  }

  implicit val format: OFormat[UserAnswers] = OFormat(reads, writes)
}
