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

package uk.gov.hmrc.tai.model

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.util.constants.TaiConstants

class UserDetailsSpec extends PlaySpec {

  "hasVerifyAuthProvider" should {
    "return true" when {
      "authProviderType is Verify" in {
        val userDetails = UserDetails(TaiConstants.AuthProviderVerify)
        userDetails.hasVerifyAuthProvider mustBe true
      }
    }
    "return false" when {
      "authProviderType is NOT Verify" in {
        val userDetails = UserDetails(TaiConstants.AuthProviderGG)
        userDetails.hasVerifyAuthProvider mustBe false
      }
    }
  }
}
