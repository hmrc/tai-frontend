package queries

import play.api.libs.json.JsPath
import uk.gov.hmrc.tai.model.UserAnswers

import scala.util.{Success, Try}

sealed trait Query {

  def path: JsPath
}

trait Gettable[A] extends Query

trait Settable[A] extends Query {

  def cleanup(value: Option[A], userAnswers: UserAnswers): Try[UserAnswers] =
    Success(userAnswers)
}
