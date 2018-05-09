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

package controllers.auth

import builders.UserBuilder
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.frontend.auth.{Attorney, AuthContext, Link}
import uk.gov.hmrc.tai.model.TaiRoot

class TaiUserSpec extends PlaySpec with MockitoSugar {

    "TaiUser" should {
      "return changed principle name" in {
        taiUser.getAuthContext("test").authContext
          .principal.name.get mustBe "test"
      }

      "return the TaiUser under test" in {
        taiUserWithAttorney.getAuthContext("") mustBe taiUserWithAttorney
      }

      "return the Name Not Defined for the principle" in {
        taiUser.getDisplayName mustBe "Name Not Defined"
      }

      "return Test Name" in {
        taiUserWithAttorney.getDisplayName mustBe "Test Name"
      }

      "return nino" in {
        taiUser.getNino mustBe UserBuilder.nino.nino
      }

      "return UTR as an empty String" in {
        taiUser.getUTR mustBe ""
      }
    }

  private val user = UserBuilder()
  private val userWithAttorney = UserBuilder.createUserWithAttorney("Test Name", Link("",""))

  private val taiUser = TaiUser(user.authContext, user.taiRoot)
  private val taiUserWithAttorney = TaiUser(userWithAttorney.authContext, userWithAttorney.taiRoot)
}
