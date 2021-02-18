/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.OffsetDateTime
import java.util.UUID

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import reactivemongo.bson.{BSONDateTime, BSONDocument}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.identifiers.JrsClaimsId
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class CachingItSpec extends UnitSpec with GuiceOneAppPerSuite
  with PatienceConfiguration
  with BeforeAndAfterEach {

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))

  def mongo: SessionRepository = app.injector.instanceOf[SessionRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(mongo.remove())
    await(mongo.drop)
  }

  val jrsClaimsAPIResponse = JrsClaims(
    List(
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2020-11"))),
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-02"), YearAndMonth("2021-01")))
    ))

  val cacheMap = new CacheMap("id", Map(JrsClaimsId.toString -> Json.toJson(jrsClaimsAPIResponse)))


  "SessionRepository" when {
    "get is called" should {
      "return an empty list" when {
        "there isn't an existing record" in {

          val fGet = mongo.get(JrsClaimsId.toString)

          await(fGet) shouldBe None
        }

        "there is an existing record" in {

          await(mongo.upsert(cacheMap))

          val fGet = mongo.get("id")

          await(fGet) shouldBe Some(cacheMap)

        }
      }
    }

    "upsert is called" should {
      "return true" when {
        "there isn't an existing record" in {

          val result = await(mongo.upsert(cacheMap))

          result shouldBe true
        }

        "there is existing data with the same id but different data" in {

          val newData = JrsClaims(
            List(
              Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2020-11"))),
              Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-02"), YearAndMonth("2021-01"))),
              Employers("MORRISONS", "ABC-DEFGHIJ", List(YearAndMonth("2021-03"), YearAndMonth("2021-01")))
            ))

          await(mongo.upsert(cacheMap))

          val cacheMapAlt = cacheMap.copy(data = Map(JrsClaimsId.toString -> Json.toJson(newData)))

          val result = await(mongo.upsert(cacheMapAlt))

          result shouldBe true
        }

        "there is an existing record" in {

          await(mongo.upsert(cacheMap))

          val result = await(mongo.upsert(cacheMap))

          result shouldBe true
        }
      }
    }
  }
}