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

import controllers.actions.ActionJourney
import controllers.auth.{AuthenticatedRequest, DataRequest}
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, Request, Result}
import play.api.test.Helpers
import uk.gov.hmrc.tai.model.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class FakeActionJourney(userAnswers: UserAnswers, authenticatedRequest: AuthenticatedRequest[_]) extends ActionJourney {
  private val actionBuilderFixture = new ActionBuilderFixture {
    override def invokeBlock[A](request: Request[A], block: DataRequest[A] => Future[Result]): Future[Result] =
      block(
        DataRequest(
          request,
          authenticatedRequest.taiUser,
          authenticatedRequest.fullName,
          userAnswers
        ) // TODO - Needs cleaning
      )
  }

  override val setJourneyCache: ActionBuilder[DataRequest, AnyContent] = actionBuilderFixture
}

trait ActionBuilderFixture extends ActionBuilder[DataRequest, AnyContent] {
  override def invokeBlock[A](a: Request[A], block: DataRequest[A] => Future[Result]): Future[Result]
  override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser()
  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
