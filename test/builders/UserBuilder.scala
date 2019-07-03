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

package builders

import controllers.auth.AuthedUser
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.util.constants.TaiConstants

object UserBuilder {
  val nino: Nino = new Generator().nextNino
  def apply(firstName: String = "Firstname", lastName: String = "Surname", utr: String = "utr", providerType: String = TaiConstants.AuthProviderGG) = {
    AuthedUser(firstName + " " + lastName, nino.toString(), utr, providerType, "200")  }
}
