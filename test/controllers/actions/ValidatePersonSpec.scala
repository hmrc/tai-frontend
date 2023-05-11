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

import controllers.{FakeAuthAction, routes}
import org.mockito.ArgumentMatchers.any
import play.api.mvc.AbstractController
import play.api.test.Helpers._
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.PersonService
import utils.BaseSpec

import scala.concurrent.Future

class ValidatePersonSpec extends BaseSpec {

  val personService = mock[PersonService]
  val personDeceased = true
  val personAlive = !personDeceased
  val cc = stubControllerComponents()

  class Harness(deceased: ValidatePerson) extends AbstractController(cc) {
    def onPageLoad() = (FakeAuthAction andThen deceased) { request =>
      Ok
    }
  }

  "DeceasedActionFilter" when {
    "the person is deceased" must {
      "redirect the user to a deceased page " in {

        when(personService.personDetails(any())(any(), any()))
          .thenReturn(
            Future.successful(
              Person(nino, "firstName", "Surname", personDeceased, manualCorrespondenceInd = false, address)
            )
          )

        val validatePerson = new ValidatePersonImpl(personService)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DeceasedController.deceased.toString)

      }
    }

    "the person is alive" must {
      "not redirect the user to a deceased page " in {

        when(personService.personDetails(any())(any(), any()))
          .thenReturn(
            Future.successful(
              Person(nino, "firstName", "Surname", personAlive, manualCorrespondenceInd = false, address)
            )
          )

        val validatePerson = new ValidatePersonImpl(personService)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK

      }

      "redirect to an mci error page if user's manualCorrespondenceInd is true " in {
        when(personService.personDetails(any())(any(), any()))
          .thenReturn(
            Future.successful(
              Person(nino, "firstName", "Surname", personAlive, manualCorrespondenceInd = true, address)
            )
          )

        val validatePerson = new ValidatePersonImpl(personService)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ServiceController.mciErrorPage.toString)
      }
    }
  }
}
