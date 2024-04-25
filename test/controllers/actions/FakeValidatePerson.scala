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

package controllers.actions
import controllers.FakeAuthAction
import controllers.auth.{AuthenticatedRequest, InternalAuthenticatedRequest}
import play.api.mvc.Result
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.tai.model.domain.{Address, Person}

import scala.concurrent.{ExecutionContext, Future}

object FakeValidatePerson extends ValidatePerson {
  override protected def refine[A](
    request: InternalAuthenticatedRequest[A]
  ): Future[Either[Result, AuthenticatedRequest[A]]] = {
    val address: Address = Address("line1", "line2", "line3", "postcode", "country")
    def fakePerson: Person = Person(FakeAuthAction.nino, "firstname", "surname", isDeceased = false, address)
    Future.successful(Right(AuthenticatedRequest(request, request.taiUser, fakePerson)))
  }

  override protected def executionContext: ExecutionContext = stubControllerComponents().executionContext
}
