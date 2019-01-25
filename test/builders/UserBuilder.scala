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

import controllers.auth.{AuthedUser, TaiUser}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{Attorney, AuthContext, Link}
import uk.gov.hmrc.tai.model.domain.Person

object AuthActionedUserBuilder {
  val nino: Nino = new Generator().nextNino
  def apply(firstName: String = "Jjj", lastName: String = "Bbb", utr: String = "utr") = {
    AuthedUser(firstName + " " + lastName, nino.toString(), utr, "userDetails")  }
}

object UserBuilder {

  val nino: Nino = new Generator().nextNino

  def apply(title: String = "Mr", firstName: String = "Jjj", lastName: String = "Bbb") = {
    val person = Person(
      nino = nino,
      firstName = firstName,
      surname = lastName,
      isDeceased = false,
      hasCorruptData = false)

    def taxForIndividualsAuthority(id: String, nino: String): Authority =
      Authority(uri = s"/path/to/authority",
        accounts =  Accounts(tai = Some(TaxForIndividualsAccount(s"/tai/$nino", Nino(nino))),
          paye = Some(PayeAccount(s"/tai/$nino", Nino(nino)))),
        loggedInAt = None,
        previouslyLoggedInAt = None,
        credentialStrength = CredentialStrength.Strong,
        confidenceLevel = ConfidenceLevel.L200,
        userDetailsLink = Some(s"/user-details/$id"),
        enrolments = Some(s"/auth/oid/$id/enrolments"),
        ids = Some(s"/auth/oid/$id/ids"),
        legacyOid = s"$id")


    def payeAuthority(id: String, nino: String): Authority =
      Authority(uri = s"/path/to/authority",
        accounts =  Accounts(paye = Some(PayeAccount(s"/tai/$nino", Nino(nino)))),
        loggedInAt = None,
        previouslyLoggedInAt = None,
        credentialStrength = CredentialStrength.Strong,
        confidenceLevel = ConfidenceLevel.L200,
        userDetailsLink = Some(s"/user-details/$id"),
        enrolments = Some(s"/auth/oid/$id/enrolments"),
        ids = Some(s"/auth/oid/$id/ids"),
        legacyOid = s"$id")


    def taiAuthContext(id: String, nino: String) = {
      AuthContext(taxForIndividualsAuthority(id, nino),governmentGatewayToken = Some("a token"))
    }

    TaiUser(
      authContext = taiAuthContext(s"${firstName.head.toLower}${lastName.toLowerCase}", person.nino.nino),
      person = person
    )
  }

  def createUserWithAttorney(attorneyName: String, attorneyLink: Link): TaiUser = {
    val user = apply()
    TaiUser(user.authContext.copy(attorney = Some(Attorney(attorneyName,attorneyLink))), user.person)
  }

}