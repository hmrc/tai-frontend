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

package utils

import controllers.auth.{AuthJourney, AuthenticatedRequest, DataRequest, InternalAuthenticatedRequest}
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc._
import play.api.test.Helpers
import uk.gov.hmrc.tai.model.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthJourney(
  userAnswers: UserAnswers
) extends AuthJourney {

  lazy val messagesApi: MessagesApi = Helpers.stubMessagesApi()

  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi)

  private val authWithDataRetrievalAuthActionBuilder = new ActionBuilder[DataRequest, AnyContent] {
    override def invokeBlock[A](
      request: Request[A],
      block: DataRequest[A] => Future[Result]
    ): Future[Result] = {
      val authRequest = AuthenticatedRequestFixture.buildUserRequest(request)
      block(
        DataRequest(
          authRequest.request,
          authRequest.taiUser,
          authRequest.fullName,
          userAnswers
        )
      )
    }

    override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser()

    override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  private val authWithValidatePersonActionBuilder = new ActionBuilder[AuthenticatedRequest, AnyContent] {
    override def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest[A] => Future[Result]
    ): Future[Result] =
      block(AuthenticatedRequestFixture.buildUserRequest(request))

    override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser()

    override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  private val authWithoutValidatePersonActionBuilder = new ActionBuilder[InternalAuthenticatedRequest, AnyContent] {
    override def invokeBlock[A](
      request: Request[A],
      block: InternalAuthenticatedRequest[A] => Future[Result]
    ): Future[Result] = {
      val authRequest = AuthenticatedRequestFixture.buildUserRequest(request)
      block(
        InternalAuthenticatedRequest(
          authRequest.request,
          authRequest.taiUser
        )
      )
    }

    override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser()

    override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }

  override val authWithValidatePerson: ActionBuilder[AuthenticatedRequest, AnyContent]            =
    authWithValidatePersonActionBuilder
  override val authWithoutValidatePerson: ActionBuilder[InternalAuthenticatedRequest, AnyContent] =
    authWithoutValidatePersonActionBuilder
  override val authWithDataRetrieval: ActionBuilder[DataRequest, AnyContent]                      = authWithDataRetrievalAuthActionBuilder
}

trait AuthWithDataRetrievalActionBuilderFixture extends ActionBuilder[DataRequest, AnyContent] {
  override def invokeBlock[A](
    a: Request[A],
    block: DataRequest[A] => Future[Result]
  ): Future[Result]
  override def parser: BodyParser[AnyContent]               = Helpers.stubBodyParser()
  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}

trait AuthWithValidatePersonActionBuilderFixture extends ActionBuilder[AuthenticatedRequest, AnyContent] {
  override def invokeBlock[A](
    a: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result]
  override def parser: BodyParser[AnyContent]               = Helpers.stubBodyParser()
  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}

trait AuthWithoutValidatePersonActionBuilderFixture extends ActionBuilder[InternalAuthenticatedRequest, AnyContent] {
  override def invokeBlock[A](
    a: Request[A],
    block: InternalAuthenticatedRequest[A] => Future[Result]
  ): Future[Result]
  override def parser: BodyParser[AnyContent]               = Helpers.stubBodyParser()
  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
