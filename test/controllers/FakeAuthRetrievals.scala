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

package controllers

import controllers.auth.{AuthRetrievals, AuthedUser, InternalAuthenticatedRequest}
import play.api.mvc._
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.domain.{Generator, Nino}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object FakeAuthRetrievals extends AuthRetrievals {

  val nino: Nino       = new Generator(new Random).nextNino
  val user: AuthedUser =
    AuthedUser(
      Nino(nino.toString()),
      Some("saUtr"),
      None
    )

  val cc: ControllerComponents = stubControllerComponents()

  override def invokeBlock[A](
    request: Request[A],
    block: InternalAuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    block(InternalAuthenticatedRequest(request, user))
  override def parser: BodyParser[AnyContent]               = cc.parsers.defaultBodyParser
  override protected def executionContext: ExecutionContext = cc.executionContext
}
