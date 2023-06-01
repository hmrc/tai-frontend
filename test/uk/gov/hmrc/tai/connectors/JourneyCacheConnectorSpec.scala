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

package uk.gov.hmrc.tai.connectors

import akka.Done
import cats.data.EitherT
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, okJson, urlPathMatching}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.Application
import play.api.http.Status._
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpClient, HttpResponse, InternalServerException, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.webchat.client.WebChatClient
import uk.gov.hmrc.webchat.testhelpers.WebChatClientStub

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class JourneyCacheConnectorSpec extends ConnectorSpec {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.tai.port"                      -> server.port(),
      "microservice.services.tai-frontend.port"             -> server.port(),
      "microservice.services.contact-frontend.port"         -> "6666",
      "microservice.services.pertax-frontend.port"          -> "1111",
      "microservice.services.personal-tax-summary.port"     -> "2222",
      "microservice.services.activity-logger.port"          -> "5555",
      "tai.cy3.enabled"                                     -> true,
      "microservice.services.feedback-survey-frontend.port" -> "3333",
      "microservice.services.company-auth.port"             -> "4444",
      "microservice.services.citizen-auth.port"             -> "9999"
    ) // TODO - Trim down on configs
    .overrides(
      play.api.inject.bind[WebChatClient].toInstance(new WebChatClientStub)
    )
    .build()

  override def messagesApi: MessagesApi = inject[MessagesApi]

  implicit lazy val ec: ExecutionContext = inject[ExecutionContext]

  def connector: JourneyCacheConnector = inject[JourneyCacheConnector]

  "JourneyCacheConnector" when {
    val cacheString = """{"key1":"value1","key2":"value2"}"""
    "currentCache is run" must {
      val url = s"/tai/journey-cache/$journeyName"
      "return an OK response if successful" in {
        server.stubFor(
          get(urlPathMatching(url))
            .willReturn(okJson(Json.toJson(cacheString).toString))
        )
        connector.currentCache(cacheString).value.futureValue.map { result =>
          result.status mustBe OK
          result.json mustBe Json.toJson(cacheString)
        }
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        UNPROCESSABLE_ENTITY,
        TOO_MANY_REQUESTS,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse with the status $errorStatus" in {
          server.stubFor(
            get(urlPathMatching(url))
              .willReturn(aResponse().withStatus(errorStatus))
          )
          connector.currentCache(cacheString).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
  }

  "currentValueAs" must {
    "return the cached value transformed by the supplied function" in {
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, JsValue](
            Future.successful(Right(JsString("1")))
          )
        )

      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, JsValue](
            Future.successful(Right(JsString("2017-03-04")))
          )
        )

      Await.result(
        sut.currentValueAs[LocalDate](journeyName, "dateValKey", string => LocalDate.parse(string)),
        5 seconds
      ) mustBe Some(LocalDate.parse("2017-03-04"))
    }

//    "trap a NO CONTENT exception (a valid business scenario), and return None in its place" in {
//      when(httpHandler.getFromApiV2(any())(any()))
//        .thenReturn(Future.failed(new HttpException("key wasn't found in cache", NO_CONTENT)))
//
//      val result = Await.result(sut.currentValueAs[String](journeyName, "key1", string => string), 5 seconds)
//      result mustBe None
//    } // TODO - Check this is correct

    "expose an exception that is not a NOT FOUND type" in {
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, JsValue](
            Future.successful(Left(UpstreamErrorResponse("something terminal", INTERNAL_SERVER_ERROR)))
          )
        )
      val thrown = the[InternalServerException] thrownBy Await
        .result(sut.currentValueAs[String](journeyName, "key1", string => string), 5 seconds)
      thrown.getMessage mustBe "something terminal"
    }
  }

//  "mandatoryValueAs" must {
//
//    "return the requested values where present" in {
//      when(httpHandler).thenReturn(Future.successful(JsString("true")))
//      Await
//        .result(
//          sut.mandatoryJourneyValueAs[Boolean](journeyName, "booleanValKey", string => string.toBoolean),
//          5 seconds) mustBe Right(true)
//    }
//
//    "throw a runtime exception when the requested value is not found" in {
//      when(httpHandler)
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
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, JsValue](
            Future.successful(Right(JsString("true")))
          )
        )
      Await.result(
        sut.mandatoryJourneyValueAs[Boolean](journeyName, "booleanValKey", string => string.toBoolean),
        5 seconds
      ) mustBe Right(true)
    }

    "return an error message when the requested value is not found" in {
      val expectedMsg = "The mandatory value under key 'key1' was not found in the journey cache for 'journey1'"
      when(httpHandler.getFromApiV2(any())(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, JsValue](
            Future.successful(Left(UpstreamErrorResponse(expectedMsg, NOT_FOUND)))
          )
        )
      Await
        .result(sut.mandatoryJourneyValueAs[Int](journeyName, "key1", string => string.toInt), 5 seconds) mustBe Left(
        expectedMsg
      )
    }
  }

  "cache" must {

    "return the updated journey cache in full" in {
      val newValuesToCache = Map("key1" -> "value1", "key2" -> "value2")
      val updatedCacheJson = """{"key1":"value1","key2":"value2","key7":"value7"}"""
      val updatedCacheMap = Map("key1" -> "value1", "key2" -> "value2", "key7" -> "value7")
      when(httpHandler.postToApi(any(), any())(any(), any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, HttpResponse](
            Future.successful(Right(HttpResponse(OK, updatedCacheJson)))
          )
        )

      val result = Await.result(sut.cache(journeyName, newValuesToCache), 5 seconds)
      result mustBe updatedCacheMap
    }
  }

  "flush" must {
    "remove journey cache data for company car journey" in {
      val url = s"${sut.cacheUrl(journeyName)}"
      when(httpHandler.deleteFromApi(meq(url))(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(NO_CONTENT, ""))))
        )

      val result = Await.result(sut.flush(journeyName), 5 seconds)
      result mustBe Done
    }
  }

  "flushWithEmpId" must {
    "remove journey cache data for company car journey" in {
      val url = s"${sut.cacheUrl(s"$journeyName/1")}"
      when(httpHandler.deleteFromApi(meq(url))(any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(NO_CONTENT, ""))))
        )

      val result = Await.result(sut.flushWithEmpId(journeyName, 1), 5 seconds)
      result mustBe Done
    }
  }

  private val journeyName = "journey1"

  val httpHandler: HttpClientResponse = mock[HttpClientResponse]

  def sut: JourneyCacheConnector = new JourneyCacheConnector(inject[HttpClient], httpHandler, inject[ServicesConfig]) {
    override val serviceUrl: String = "mockUrl"
  }

}
