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

package controllers.actions

import controllers.{FakeAuthAction, FakeTaiPlayApplication, routes}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Controller
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.PersonService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class ValidatePersonSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {

  implicit val hc = HeaderCarrier()
  val personService = mock[PersonService]
  val personNino = new Generator(new Random).nextNino
  val personDeceased = true
  val personAlive = !personDeceased

  class Harness(deceased: ValidatePerson) extends Controller {
    def onPageLoad() = (FakeAuthAction andThen deceased) { request =>
      Ok
    }
  }

  "DeceasedActionFilter" when {
    "a person is returned and they are deceased" must {
      "redirect the user to a deceased page" in {

        when(personService.personDetails(any())(any()))
          .thenReturn(Future.successful(Right(Person(personNino, "firstName", "Surname", personDeceased, false))))

        val validatePerson = new ValidatePersonImpl(personService)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DeceasedController.deceased().toString)

      }
    }

    "the person is returned and they are alive" must {
      "not redirect the user to a deceased page" in {

        when(personService.personDetails(any())(any()))
          .thenReturn(Future.successful(Right(Person(personNino, "firstName", "Surname", personAlive, false))))

        val validatePerson = new ValidatePersonImpl(personService)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK

      }

      "redirect to a corrupt page if user has corrupt data" in {
        when(personService.personDetails(any())(any()))
          .thenReturn(
            Future.successful(Right(Person(personNino, "firstName", "Surname", personAlive, hasCorruptData = true))))

        val validatePerson = new ValidatePersonImpl(personService)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ServiceController.gateKeeper().toString)
      }
    }

    "a TaiUnauthorisedResponse is returned" must {
      "redirect to Unauthorised page" in {
        when(personService.personDetails(any())(any()))
          .thenReturn(Future.successful(Left(TaiUnauthorisedResponse("Failed"))))

        val validatePerson = new ValidatePersonImpl(personService)

        val controller = new Harness(validatePerson)

        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().toString)

      }
    }
  }
}
