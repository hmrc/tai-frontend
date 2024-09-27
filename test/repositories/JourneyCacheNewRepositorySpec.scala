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

package repositories

import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, DefaultPlayMongoRepositorySupport}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.UserAnswers

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class JourneyCacheNewRepositorySpec
    extends AnyFreeSpec with Matchers with DefaultPlayMongoRepositorySupport[UserAnswers] with ScalaFutures
    with IntegrationPatience with OptionValues with MockitoSugar with CleanMongoCollectionSupport {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val userAnswers = UserAnswers("session-id-1", "nino", Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[ApplicationConfig]
  when(mockAppConfig.sessionTimeoutInSeconds) thenReturn 1

  protected override val repository = new JourneyCacheNewRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {
    "must set the last updated time on the supplied user answers to `now`, and save them" in {
      val expectedResult = userAnswers copy (lastUpdated = instant)

      val setResult = repository.set(userAnswers).futureValue
      val updatedRecord = find(
        Filters.and(
          Filters.equal("sessionId", userAnswers.sessionId),
          Filters.equal("nino", userAnswers.nino)
        )
      ).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual expectedResult
    }

    "must overwrite value when value already exists in collection with same nino and session id" in {
      repository
        .set(userAnswers)
        .futureValue

      val setResult: Boolean = repository
        .set(userAnswers)
        .futureValue

      val results = find(
        Filters.and(
          Filters.equal("sessionId", userAnswers.sessionId),
          Filters.equal("nino", userAnswers.nino)
        )
      ).futureValue

      setResult mustEqual true
      results.size mustBe 1
      val actualUserAnswers = results.headOption.value
      actualUserAnswers.sessionId mustBe userAnswers.sessionId
      actualUserAnswers.nino mustBe userAnswers.nino
      actualUserAnswers.data mustBe userAnswers.data
    }

    "must save a new document when value already exists in collection with different nino but same session id" in {
      val existingUserAnswers =
        UserAnswers("session-id-1", "nino2", Json.obj("foo2" -> "bar2"), Instant.ofEpochSecond(1))
      repository
        .set(existingUserAnswers)
        .futureValue

      val setResult: Boolean = repository
        .set(userAnswers)
        .futureValue

      val results = findAll().futureValue

      setResult mustEqual true
      results.size mustBe 2
      results.exists(ua => ua.sessionId == userAnswers.sessionId && ua.nino == userAnswers.nino) mustBe true
      results.exists(ua =>
        ua.sessionId == existingUserAnswers.sessionId && ua.nino == existingUserAnswers.nino
      ) mustBe true
    }
  }

  ".get" - {
    "when there is a record for this id" - {
      "must update the lastUpdated time and get the record" in {
        insert(userAnswers).futureValue

        val result = repository.get(userAnswers.sessionId, userAnswers.nino).futureValue
        val expectedResult = userAnswers copy (lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {
      "must return None" in {
        repository.get("id that does not exist", "nino that does not exist").futureValue must not be defined
      }
    }
  }

  ".clear" - {
    "must remove a record" in {
      insert(userAnswers).futureValue
      val result = repository.clear(userAnswers.sessionId, userAnswers.nino).futureValue

      result mustEqual true
      repository.get(userAnswers.sessionId, userAnswers.nino).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist", "nino does not exist").futureValue

      result mustEqual true
    }
  }

  ".keepAlive" - {
    "when there is a record for this id" - {
      "must update its lastUpdated to `now` and return true" in {

        insert(userAnswers).futureValue

        val result = repository.keepAlive(userAnswers.sessionId, userAnswers.nino).futureValue

        val expectedUpdatedAnswers = userAnswers copy (lastUpdated = instant)

        result mustEqual true

        val updatedAnswers = find(
          Filters.and(
            Filters.equal("sessionId", userAnswers.sessionId),
            Filters.equal("nino", userAnswers.nino)
          )
        ).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {
      "must return true" in {
        repository.keepAlive("id that does not exist", "not exist").futureValue mustEqual true
      }
    }
  }
}
