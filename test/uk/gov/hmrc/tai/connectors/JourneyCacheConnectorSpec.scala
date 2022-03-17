/*
 * Copyright 2022 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpException, HttpResponse, InternalServerException, NotFoundException}
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponse
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class JourneyCacheConnectorSpec extends BaseSpec {

  "currentCache" must {

    "return the map of current cached values [String, String], as returned from the api call" in {
      val cacheString = """{"key1":"value1","key2":"value2"}"""
      when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(Json.parse(cacheString)))

      val expectedResult = Map("key1" -> "value1", "key2" -> "value2")
      val result = Await.result(sut.currentCache(journeyName), 5 seconds)
      result mustBe expectedResult
    }
    "trap a NO CONTENT exception (a valid business scenario), and return an empty map in its place" in {
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(Future.failed(new HttpException("no cache was found", NO_CONTENT)))

      val result = Await.result(sut.currentCache(journeyName), 5 seconds)
      result mustBe Map.empty[String, String]
    }
    "expose any exception that is not a NOT FOUND type" in {
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(Future.failed(new InternalServerException("something terminal")))

      val thrown = the[InternalServerException] thrownBy Await.result(sut.currentCache(journeyName), 5 seconds)
      thrown.getMessage mustBe "something terminal"
    }
  }

  "currentValueAs" must {

    "return the cached value transformed by the supplied function" in {
      when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(JsString("1")))

      when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(JsString("2017-03-04")))
      Await.result(
        sut.currentValueAs[LocalDate](journeyName, "dateValKey", string => LocalDate.parse(string)),
        5 seconds) mustBe Some(new LocalDate("2017-03-04"))
    }

    "trap a NO CONTENT exception (a valid business scenario), and return None in its place" in {
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(Future.failed(new HttpException("key wasn't found in cache", NO_CONTENT)))

      val result = Await.result(sut.currentValueAs[String](journeyName, "key1", string => string), 5 seconds)
      result mustBe None
    }

    "expose an exception that is not a NOT FOUND type" in {
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(Future.failed(new InternalServerException("something terminal")))

      val thrown = the[InternalServerException] thrownBy Await
        .result(sut.currentValueAs[String](journeyName, "key1", string => string), 5 seconds)
      thrown.getMessage mustBe "something terminal"
    }
  }

//  "mandatoryValueAs" must {
//
//    "return the requested values where present" in {
//      when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(JsString("true")))
//      Await
//        .result(
//          sut.mandatoryJourneyValueAs[Boolean](journeyName, "booleanValKey", string => string.toBoolean),
//          5 seconds) mustBe Right(true)
//    }
//
//    "throw a runtime exception when the requested value is not found" in {
//      when(httpHandler.getFromApiV2(any())(any()))
//        .thenReturn(Future.failed(new NotFoundException("key wasn't found in cache")))
//
//      val expectedMsg = "The mandatory value under key 'key1' was not found in the journey cache for 'journey1'"
//      val thrown2 = the[RuntimeException] thrownBy Await
//        .result(sut.mandatoryJourneyValueAs[Int](journeyName, "key1", string => string.toInt), 5 seconds)
//      thrown2.getMessage mustBe expectedMsg
//    }
//  }

  "mandatoryJourneyValueAs" must {

    "return the requested values where present" in {
      when(httpHandler.getFromApiV2(any())(any())).thenReturn(Future.successful(JsString("true")))
      Await.result(
        sut.mandatoryJourneyValueAs[Boolean](journeyName, "booleanValKey", string => string.toBoolean),
        5 seconds) mustBe Right(true)
    }

    "return an error message when the requested value is not found" in {
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(Future.failed(new HttpException("key wasn't found in cache", NO_CONTENT)))

      val expectedMsg = "The mandatory value under key 'key1' was not found in the journey cache for 'journey1'"
      Await
        .result(sut.mandatoryJourneyValueAs[Int](journeyName, "key1", string => string.toInt), 5 seconds) mustBe Left(
        expectedMsg)
    }
  }

  "cache" must {

    "return the updated journey cache in full" in {
      val newValuesToCache = Map("key1" -> "value1", "key2" -> "value2")
      val updatedCacheJson = """{"key1":"value1","key2":"value2","key7":"value7"}"""
      val updatedCacheMap = Map("key1" -> "value1", "key2" -> "value2", "key7" -> "value7")
      when(httpHandler.postToApi(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(updatedCacheJson)))))

      val result = Await.result(sut.cache(journeyName, newValuesToCache), 5 seconds)
      result mustBe updatedCacheMap
    }
  }

  "flush" must {
    "remove journey cache data for company car journey" in {
      val url = s"${sut.cacheUrl(journeyName)}"
      when(httpHandler.deleteFromApi(Matchers.eq(url))(any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = Await.result(sut.flush(journeyName), 5 seconds)
      result mustBe TaiSuccessResponse
    }
  }

  private val journeyName = "journey1"

  val httpHandler: HttpHandler = mock[HttpHandler]

  def sut: JourneyCacheConnector = new JourneyCacheConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = "mockUrl"
  }

}
