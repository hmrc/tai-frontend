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

package controllers

import builders.UserBuilder
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.mvc.Results.Ok
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.Person

import scala.concurrent.Future

class ServiceCheckLiteSpec extends PlaySpec with FakeTaiPlayApplication {

  implicit val hc = HeaderCarrier()
  implicit val user = UserBuilder.apply()

  val nino = new Generator().nextNino

  def definePerson(mci:Boolean, di: Boolean) =
    Person(nino, "firstname", "surname", di, mci)

  implicit val timeout = 16

  "personDetailsCheck in ServiceCheckLite" should {
    "redirect users" when {
      "deceased indicator is true for the user" in {
        implicit val person = definePerson(true, true)
        val result = ServiceCheckLite.personDetailsCheck{
          Future.successful(Ok("test"))
        }

        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe routes.DeceasedController.deceased.url
      }

      "MCI indicator (aka hasCorruptData) is true for the user" in {
        implicit val person = definePerson(true, false)
        val result = ServiceCheckLite.personDetailsCheck{
          Future.successful(Ok("test"))
        }

        status(result) mustBe SEE_OTHER
        redirectLocation(result).getOrElse("") mustBe routes.ServiceController.gateKeeper.url
      }
    }

    "not be redirected" when {
      "deceased indicator and MCI (hasCorruptData) is false" in {
        implicit val person = definePerson(false, false)
        val result = ServiceCheckLite.personDetailsCheck{
          Future.successful(Ok("test"))
        }

        status(result) mustBe OK
      }
    }
  }

}
