/*
 * Copyright 2024 HM Revenue & Customs
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

package utils.generators

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.TryValues
import pages._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.{RichJsObject, UserAnswers}

import scala.util.Random

trait UserAnswersGenerator extends TryValues {
  self: Generators =>

  val generators: Seq[Gen[(QuestionPage[_], JsValue)]] = Nil

  private def generateNino: Nino = new Generator(new Random).nextNino

  implicit lazy val arbitraryUserData: Arbitrary[UserAnswers] =
    Arbitrary {
      for {
        sessionId <- nonEmptyString
        data      <- generators match {
                       case Nil => Gen.const(Map[QuestionPage[_], JsValue]())
                       case _   => Gen.mapOf(oneOf(generators))
                     }
      } yield UserAnswers(
        sessionId = sessionId,
        nino = generateNino.nino,
        data = data.foldLeft(Json.obj()) { case (obj, (path, value)) =>
          obj.setObject(path.path, value).get
        }
      )
    }
}
