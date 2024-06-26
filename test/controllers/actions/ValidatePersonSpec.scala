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

import cats.data.EitherT
import controllers.{ErrorPagesHandler, FakeAuthRetrievals, routes}
import org.mockito.ArgumentMatchers.any
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.api.test.Helpers._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.model.domain.Person
import uk.gov.hmrc.tai.service.PersonService
import utils.BaseSpec

import scala.concurrent.Future

class ValidatePersonSpec extends BaseSpec with I18nSupport {

  val personService: PersonService = mock[PersonService]
  val errorPagesHandler: ErrorPagesHandler = inject[ErrorPagesHandler]
  val cc: ControllerComponents = stubControllerComponents()

  class Harness(deceased: ValidatePerson) extends AbstractController(cc) {
    def onPageLoad(): Action[AnyContent] = (FakeAuthRetrievals andThen deceased) { _ =>
      Ok
    }
  }

  "DeceasedActionFilter" when {
    "the person is deceased" must {
      "redirect the user to a deceased page " in {

        val deceasedPerson = fakePerson(nino).copy(isDeceased = true)

        when(personService.personDetails(any())(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](deceasedPerson))

        val validatePerson = new ValidatePersonImpl(personService, messagesApi, errorPagesHandler)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DeceasedController.deceased().url)
      }
    }

    "the person is alive" must {
      "not redirect the user to a deceased page " in {

        val alivePerson = fakePerson(nino)

        when(personService.personDetails(any())(any(), any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](alivePerson))

        val validatePerson = new ValidatePersonImpl(personService, messagesApi, errorPagesHandler)

        val controller = new Harness(validatePerson)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK
      }

      "the person details retrieval fails" must {
        "return an internal server error" in {
          when(personService.personDetails(any())(any(), any()))
            .thenReturn(
              EitherT.leftT[Future, Person](
                UpstreamErrorResponse("Failed to get person designatory details", INTERNAL_SERVER_ERROR)
              )
            )

          val validatePerson = new ValidatePersonImpl(personService, messagesApi, errorPagesHandler)
          val controller = new Harness(validatePerson)
          val result = controller.onPageLoad()(fakeRequest)

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}
