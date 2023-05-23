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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{JsResultException, JsValue, Json}
import uk.gov.hmrc.http.{HttpResponse, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.domain.Person
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PersonConnectorSpec extends BaseSpec {

  "person method" must {

    "return a Person model instance" when {
      "the http call returns successfully" in {
        when(httpHandler.getFromApiV2(meq(s"/fakeUrl/tai/${nino.nino}/person"))(any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, JsValue](Future.successful(Right(apiResponse(person)))))

        val result = Await.result(sut.person(nino), 5.seconds)
        result mustBe person
      }
    }

    "return a Failed Future" when {
      "the http call returns a not found exception" in {
        when(httpHandler.getFromApiV2(meq(s"/fakeUrl/tai/${nino.nino}/person"))(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, JsValue](
              Future.successful(Left(UpstreamErrorResponse("", NOT_FOUND)))
            )
          )
        assertThrows[NotFoundException] {
          Await.result(sut.person(nino), 5.seconds)
        }
      }

      "the http call returns invalid json" in {
        val invalidJson = Json.obj("data" -> Json.obj("notEven" -> "close"))
        when(httpHandler.getFromApiV2(meq(s"/fakeUrl/tai/${nino.nino}/person"))(any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, JsValue](Future.successful(Right(invalidJson))))
        assertThrows[JsResultException] {
          Await.result(sut.person(nino), 5.seconds)
        }
      }
    }
  }

  def apiResponse(person: Person) = Json.obj("data" -> Json.toJson(person))

  val person = fakePerson(nino)

  val httpHandler: HttpClientResponse = mock[HttpClientResponse]

  def sut: PersonConnector = new PersonConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = "/fakeUrl"
  }

}
