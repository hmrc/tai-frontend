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

import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

import scala.concurrent.Future

object AuthBuilder {

  val nino: Nino = new Generator().nextNino

  def createFakeAuthData: Future[Some[Authority]] = {
    Future.successful {
      Some( createFakeAuthority(nino.nino) )
    }
  }

  def createFakeAuthData(nino: Nino): Future[Some[Authority]] = {
    Future.successful {
      Some( createFakeAuthority(nino.nino) )
    }
  }

  def createFakeAuthority(nino: String) = {
    Authority(uri = s"/path/to/authority",
      accounts = Accounts(paye = Some(PayeAccount("", Nino(nino))),
        tai = Some(TaxForIndividualsAccount("", Nino(nino)))),
      loggedInAt = None,
      previouslyLoggedInAt = None,
      credentialStrength = CredentialStrength.Strong,
      confidenceLevel = ConfidenceLevel.L200,
      userDetailsLink = Some("/user-details/jjjbbb"),
      enrolments = Some("/auth/oid/jbloggs/enrolments"),
      ids = Some("/auth/oid/jjjbbb/ids"),
      legacyOid = "jjjbbb")
  }

}