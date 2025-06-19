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

import builders.UserBuilder
import controllers.FakeAuthRetrievals
import controllers.auth.{AuthedUser, AuthenticatedRequest}
import play.api.mvc.Request
import uk.gov.hmrc.tai.model.domain.{Address, Person}

object AuthenticatedRequestFixture {

  def buildUserRequest[A](
    request: Request[A],
    authedUser: AuthedUser = UserBuilder()
  ): AuthenticatedRequest[A] = {
    val address: Address   = Address("line1", "line2", "line3", "postcode", "country")
    def fakePerson: Person = Person(FakeAuthRetrievals.nino, "Firstname", "Surname", isDeceased = false, address)
    AuthenticatedRequest(request, authedUser, fakePerson)
  }
}
