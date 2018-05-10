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

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.domain.Person

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class PersonConnectorSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with ServicesConfig {

  "person method" must {

    "return a Person model instance, wrapped in a TaiSuccessResponse" when {
      "the http call returns successfully" in {
        val sut = new SUT("/fakeUrl")
        when(sut.httpHandler.getFromApi(Matchers.eq(s"/fakeUrl/tai/${nino.nino}/person"))(any())).thenReturn(Future.successful(apiResponse(person)))
        val result = Await.result(sut.person(nino), 5 seconds)
        result mustBe(TaiSuccessResponseWithPayload(person))
      }
    }

    "return a TaiNotFoundResponse" when {
      "the http call returns a not found exception" in {
        val sut = new SUT("/fakeUrl")
        when(sut.httpHandler.getFromApi(Matchers.eq(s"/fakeUrl/tai/${nino.nino}/person"))(any())).thenReturn(Future.failed(new NotFoundException("downstream not found")))
        val result = Await.result(sut.person(nino), 5 seconds)
        result mustBe(TaiNotFoundResponse("downstream not found"))
      }

      "the http call returns invalid json" in {
        val sut = new SUT("/fakeUrl")
        val invalidJson = Json.obj("data" -> Json.obj("notEven" -> "close"))
        when(sut.httpHandler.getFromApi(Matchers.eq(s"/fakeUrl/tai/${nino.nino}/person"))(any())).thenReturn(Future.successful(invalidJson))
        val result = Await.result(sut.person(nino), 5 seconds)
        result match {
          case TaiNotFoundResponse(msg) => msg must include("JsResultException")
          case _ => fail("A TaiNotFoundResponse was expected!")
        }
      }
    }
  }

  def apiResponse(person: Person) = Json.obj("data" -> Json.toJson(person))

  implicit val hc = HeaderCarrier()
  val nino = new Generator(new Random).nextNino
  val person = fakePerson(nino)

  class SUT(servUrl: String = "")  extends PersonConnector {
    override val serviceUrl: String = servUrl
    override val httpHandler: HttpHandler = mock[HttpHandler]
  }

}
