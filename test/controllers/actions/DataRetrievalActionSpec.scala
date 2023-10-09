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

import builders.RequestBuilder.uuid
import builders.UserBuilder
import controllers.auth.{AuthedUser, AuthenticatedRequest, DataRequest, IdentifierRequest}
import org.mockito.ArgumentMatchers
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.util.constants.TaiConstants
import utils.BaseSpec

import java.time.Instant
import scala.concurrent.Future

class DataRetrievalActionSpec extends BaseSpec with MockitoSugar with ScalaFutures {

  class Harness(repository: JourneyCacheNewRepository) extends DataRetrievalActionImpl(repository) {
    def callTransform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] = transform(request)
  }

  private val repository = mock[JourneyCacheNewRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(repository)
  }

  "Data Retrieval Action" when {
    "when there is no data in the cache" must {
      "must set an empty UserAnswers in the DataRequest with correct id + nino where not acting as trusted helper" in {
        val userId = s"session-$uuid"
        val request = FakeRequest(GET, "/").withSession(SessionKeys.sessionId -> userId)

        when(repository.get(any, any)).thenReturn(Future.successful(None))
        val action = new Harness(repository)

        val result = action
          .callTransform(IdentifierRequest(AuthenticatedRequest(request, authedUser, "testName"), userId))
          .futureValue

        result.userAnswers.id mustBe userId
        result.userAnswers.nino mustBe authedUser.nino.nino
        result.userAnswers.data mustBe Json.obj()
      }

      "must set an empty UserAnswers in the DataRequest with correct id + nino where acting as trusted helper" in {
        val userId = s"session-$uuid"
        val request = FakeRequest(GET, "/").withSession(SessionKeys.sessionId -> userId)

        when(repository.get(any, any)).thenReturn(Future.successful(None))
        val action = new Harness(repository)

        val helperNino = "helper-nino"
        val authedUser: AuthedUser = UserBuilder(
          utr = "utr",
          providerType = TaiConstants.AuthProviderGG,
          principalName = "",
          principalNino = helperNino
        )

        val result = action
          .callTransform(IdentifierRequest(AuthenticatedRequest(request, authedUser, "testName"), userId))
          .futureValue

        result.userAnswers.id mustBe userId
        result.userAnswers.nino mustBe helperNino
        result.userAnswers.data mustBe Json.obj()
      }
    }

    "when there is data in the cache" must {
      "must set the UserAnswers in the DataRequest which exists in the cache where not acting as helper" in {
        val userId = s"session-$uuid"
        val request = FakeRequest().withSession(SessionKeys.sessionId -> userId)
        val instant = Instant.now()
        val data = Json.obj("testKey" -> "testValue")

        when(repository.get(ArgumentMatchers.eq(userId), ArgumentMatchers.eq(authedUser.nino.nino))).thenReturn(
          Future(Some(UserAnswers(userId, authedUser.nino.nino, Json.obj("testKey" -> "testValue"), instant)))
        )
        val action = new Harness(repository)

        val result = action
          .callTransform(IdentifierRequest(AuthenticatedRequest(request, authedUser, "testName"), userId))
          .futureValue

        result.userAnswers.id mustBe userId
        result.userAnswers.nino mustBe authedUser.nino.nino
        result.userAnswers.data mustBe data
        result.userAnswers.lastUpdated mustBe instant
      }

      "must set the UserAnswers in the DataRequest which exists in the cache where acting as helper" in {
        val userId = s"session-$uuid"
        val request = FakeRequest().withSession(SessionKeys.sessionId -> userId)
        val instant = Instant.now()
        val data = Json.obj("testKey" -> "testValue")

        val helperNino = "helper-nino"
        val authedUser: AuthedUser = UserBuilder(
          utr = "utr",
          providerType = TaiConstants.AuthProviderGG,
          principalName = "",
          principalNino = helperNino
        )

        when(repository.get(ArgumentMatchers.eq(userId), ArgumentMatchers.eq(helperNino))).thenReturn(
          Future(Some(UserAnswers(userId, helperNino, Json.obj("testKey" -> "testValue"), instant)))
        )
        val action = new Harness(repository)

        val result = action
          .callTransform(IdentifierRequest(AuthenticatedRequest(request, authedUser, "testName"), userId))
          .futureValue

        result.userAnswers.id mustBe userId
        result.userAnswers.nino mustBe helperNino
        result.userAnswers.data mustBe data
        result.userAnswers.lastUpdated mustBe instant
      }
    }
  }

}
