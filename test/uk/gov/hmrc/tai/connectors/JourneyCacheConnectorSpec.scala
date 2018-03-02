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

package uk.gov.hmrc.tai.connectors

import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import play.api.http.Status._
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, InternalServerException, NotFoundException }

class JourneyCacheConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "currentCache" must {

    "return the map of current cached values [String, String], as returned from the api call" in {
      val sut = createSUT()
      val cacheString = """{"key1":"value1","key2":"value2"}"""
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(cacheString)))

      val expectedResult = Map("key1" -> "value1", "key2" -> "value2")
      val result = Await.result(sut.currentCache(journeyName), 5 seconds)
      result mustBe expectedResult
    }
    "trap a NOT FOUND exception (a valid business scenario), and return an empty map in its place" in {
      val sut = createSUT()
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new NotFoundException("no cache was found")))

      val result = Await.result(sut.currentCache(journeyName), 5 seconds)
      result mustBe Map.empty[String, String]
    }
    "expose any exception that is not a NOT FOUND type" in {
      val sut = createSUT()
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new InternalServerException("something terminal")))

      val thrown = the[InternalServerException] thrownBy Await.result(sut.currentCache(journeyName), 5 seconds)
      thrown.getMessage mustBe "something terminal"
    }
  }

  "currentValueAs" must {

    "return the cached value transformed by the supplied function" in {
      val sut = createSUT()
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(JsString("1")))

      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(JsString("2017-03-04")))
      Await.result(sut.currentValueAs[LocalDate](journeyName, "dateValKey", string => LocalDate.parse(string)), 5 seconds) mustBe Some(new LocalDate("2017-03-04"))
    }

    "trap a NOT FOUND exception (a valid business scenario), and return None in its place" in {
      val sut = createSUT()
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new NotFoundException("key wasn't found in cache")))

      val result = Await.result(sut.currentValueAs[String](journeyName, "key1", string => string), 5 seconds)
      result mustBe None
    }

    "expose an exception that is not a NOT FOUND type" in {
      val sut = createSUT()
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new InternalServerException("something terminal")))

      val thrown = the[InternalServerException] thrownBy Await.result(sut.currentValueAs[String](journeyName, "key1", string => string), 5 seconds)
      thrown.getMessage mustBe "something terminal"
    }
  }

  "mandatoryValueAs" must {

    "return the requested values where present" in {
      val sut = createSUT()
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(JsString("true")))
      Await.result(sut.mandatoryValueAs[Boolean](journeyName, "booleanValKey", string => string.toBoolean), 5 seconds) mustBe true
    }

    "throw a runtime exception when the requested value is not found" in {
      val sut = createSUT()
      when(sut.httpHandler.getFromApi(any())(any())).thenReturn(Future.failed(new NotFoundException("key wasn't found in cache")))

      val expectedMsg = "The mandatory value under key 'key1' was not found in the journey cache for 'journey1'"
      val thrown2 = the[RuntimeException] thrownBy Await.result(sut.mandatoryValueAs[Int](journeyName, "key1", string => string.toInt), 5 seconds)
      thrown2.getMessage mustBe expectedMsg
    }
  }

  "cache" must {

    "return the updated journey cache in full" in {
      val sut = createSUT()
      val newValuesToCache = Map("key1" -> "value1", "key2" -> "value2")
      val updatedCacheJson = """{"key1":"value1","key2":"value2","key7":"value7"}"""
      val updatedCacheMap = Map("key1" -> "value1", "key2" -> "value2", "key7" -> "value7")
      when(sut.httpHandler.postToApi(any(),any())(any(),any(),any())).thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(updatedCacheJson)))))

      val result = Await.result(sut.cache(journeyName, newValuesToCache), 5 seconds)
      result mustBe updatedCacheMap
    }
  }

  "flush" must {
    "remove journey cache data for company car journey" in {
      val sut = createSUT()
      val url = s"${sut.cacheUrl(journeyName)}"
      when(sut.httpHandler.deleteFromApi(Matchers.eq(url))(any(),any())).thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = Await.result(sut.flush(journeyName), 5 seconds)
      result mustBe TaiSuccessResponse
    }
  }

  private val journeyName = "journey1"
  private def createSUT() = new SUT
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private class SUT extends JourneyCacheConnector {
    override val serviceUrl: String = "mockUrl"
    override val httpHandler: HttpHandler = mock[HttpHandler]
  }
}
