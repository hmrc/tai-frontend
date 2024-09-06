/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.CitizenDetailsConnector
import uk.gov.hmrc.tai.model.domain.{Address, Person}
import utils.BaseSpec

import scala.concurrent.Future

class PersonServiceSpec extends BaseSpec {

  val mockConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val sut: PersonService = new PersonService(mockConnector)

  val mockService: PersonService = mock[PersonService]
  val person: Person = Person(nino, "John", "Doe", isDeceased = false, address)

  "personDetails" must {
    "return a Person model instance" when {
      "connector returns the data successfully" in {
        val personDetails = fakePerson(nino)
        when(mockConnector.retrieveCitizenDetails(any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson(personDetails).toString)))
            )
          )

        val result = sut.personDetails(nino).value.futureValue

        result mustBe a[Right[_, _]]
        result.getOrElse(personDetails.copy(address = Address.emptyAddress)) mustBe personDetails
      }
    }

    List(
      BAD_REQUEST,
      NOT_FOUND,
      LOCKED,
      TOO_MANY_REQUESTS,
      REQUEST_TIMEOUT,
      INTERNAL_SERVER_ERROR,
      SERVICE_UNAVAILABLE,
      BAD_GATEWAY
    ).foreach { errorResponse =>
      s"return an UpstreamErrorResponse containing $errorResponse when connector returns the same" in {
        when(mockConnector.retrieveCitizenDetails(any())(any())).thenReturn(
          EitherT[Future, UpstreamErrorResponse, HttpResponse](
            Future.successful(Left(UpstreamErrorResponse("", errorResponse)))
          )
        )

        val result = sut.personDetails(nino).value.futureValue

        result mustBe a[Left[UpstreamErrorResponse, _]]
        result.swap.getOrElse(UpstreamErrorResponse("", OK)).statusCode mustBe errorResponse
      }
    }
  }

}
