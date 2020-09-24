/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.connectors.{PersonConnector, TaiConnector}
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class PersonServiceSpec extends BaseSpec {

  "personDetailsNew method" must {
    "return a Person model instance" when {
      "connector returns successfully" in {
        val sut = createSut
        val person = fakePerson(nino)
        when(personConnector.person(Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(person)))

        val result = Await.result(sut.personDetails(nino), testTimeout)
        result mustBe (person)
      }
    }
    "throw a runtime exception" when {
      "copnnector did not return successfully" in {
        val sut = createSut
        when(personConnector.person(Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(TaiNotFoundResponse("downstream not found")))

        val thrown = the[RuntimeException] thrownBy Await.result(sut.personDetails(nino), testTimeout)
        thrown.getMessage must include("Failed to retrieve person details for nino")
      }
    }
  }

  val testTimeout = 5 seconds

  def createSut = new PersonServiceTest

  val personConnector = mock[PersonConnector]
  val taiConnector = mock[TaiConnector]

  class PersonServiceTest
      extends PersonService(
        taiConnector,
        personConnector
      )

}
