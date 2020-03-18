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

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.connectors.{PersonConnector, TaiConnector}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random

class PersonServiceSpec extends PlaySpec with MockitoSugar with I18nSupport with FakeTaiPlayApplication {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private implicit val hc = HeaderCarrier()

  "personDetailsNew method" must {
    "return a Person model instance" when {
      "connector returns successfully" in {
        val sut = createSut
        val person = fakePerson(nino)
        when(personConnector.person(Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(TaiSuccessResponseWithPayload(person)))

        val result = Await.result(sut.personDetails(nino), testTimeout)
        result mustBe Right(person)
      }
    }
    "return a TaiUnauthorisedResponse" when {
      "connector returns a TaiUnauthorisedResponse" in {
        val sut = createSut
        val response = TaiUnauthorisedResponse("Unauthorised")
        when(personConnector.person(Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(response))

        val result = Await.result(sut.personDetails(nino), testTimeout)
        result mustBe Left(response)
      }
    }

    "throw a RuntimeException" when {
      "connector returns a TaiFailureResponse" in {
        val sut = createSut
        val response = TaiNotFoundResponse("Not Found")
        when(personConnector.person(Matchers.eq(nino))(any()))
          .thenReturn(Future.successful(response))

        the[RuntimeException] thrownBy {
          Await.result(sut.personDetails(nino), testTimeout)
        } must have message s"Failed to retrieve person details for nino $nino. Unable to proceed."
      }
    }
  }

  val testTimeout = 5 seconds

  val nino = new Generator(new Random).nextNino

  def createSut = new PersonServiceTest

  val personConnector = mock[PersonConnector]
  val taiConnector = mock[TaiConnector]

  class PersonServiceTest
      extends PersonService(
        taiConnector,
        personConnector
      )

}
