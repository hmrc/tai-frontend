/*
 * Copyright 2018 HM Revenue & Customs
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

package builders

import java.util.UUID

import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys

object RequestBuilder {

  def buildFakeRequestWithAuth(method: String) =
    FakeRequest(method = method, path = "").withFormUrlEncodedBody(
      "name" -> "test1", "description" -> "description",
      "employmentId" -> "14",
      "newAmount" -> "1675",
      "oldAmount" -> "11",
      "worksNumber" -> "",
      "startDate" -> "2013-08-03",
      "endDate" -> "",
      "isLive" -> "true",
      "isOccupationalPension" -> "false",
      "hasMultipleIncomes" -> "true").withSession(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
      SessionKeys.authProvider -> "IDA",
      SessionKeys.userId -> s"/path/to/authority")
  SessionKeys.authToken -> "a token"

  def buildFakeRequestWithAuth(method: String, action: String) =
    FakeRequest(method = method, path = "").withFormUrlEncodedBody(
      "name" -> "test1", "description" -> "description",
      "employmentId" -> "14",
      "newAmount" -> "1675",
      "oldAmount" -> "11",
      "worksNumber" -> "",
      "startDate" -> "2013-08-03",
      "endDate" -> "",
      "isLive" -> "true",
      "action" -> action,
      "isOccupationalPension" -> "false",
      "hasMultipleIncomes" -> "true").withSession(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
      SessionKeys.authProvider -> "IDA",
      SessionKeys.userId -> s"/path/to/authority")
  SessionKeys.authToken -> "a token"

  def buildFakeInvalidRequestWithAuth(method: String) =
    FakeRequest(method = method, path = "").withFormUrlEncodedBody(
      "name" -> "test1", "description" -> "description",
      "employmentId" -> "14").withSession(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}",
      SessionKeys.authProvider -> "IDA",
      SessionKeys.userId -> s"/path/to/authority")
  SessionKeys.authToken -> "a token"

  def buildFakeRequestWithoutAuth(method: String) =
    FakeRequest(method = method, path = "").withFormUrlEncodedBody(
      "name" -> "test1", "description" -> "description",
      "employmentId" -> "14",
      "newAmount" -> "1675",
      "oldAmount" -> "11",
      "worksNumber" -> "",
      "startDate" -> "2013-08-03",
      "endDate" -> "",
      "isLive" -> "true",
      "isOccupationalPension" -> "false",
      "hasMultipleIncomes" -> "true").withSession(
      SessionKeys.sessionId -> s"session-${UUID.randomUUID()}")

}
