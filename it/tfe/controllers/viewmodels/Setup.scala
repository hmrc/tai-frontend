package tfe.controllers.viewmodels

import controllers.auth.TaiUser
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.TaiRoot

trait Setup {
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val user: TaiUser = UserBuilder.apply()
}


object UserBuilder {
  def generateNino: String = new Generator().nextNino.nino

  def apply() = {
    val taiRoot = TaiRoot(nino = generateNino,
      title = "Mr",
      version = 99,
      firstName = "Jjj",
      secondName = None,
      surname = "Bbb",
      name = "Jjj Bbb")

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

    new TaiUser(
      authContext = taiAuthContext("jjjbbb", taiRoot.nino),
      taiRoot = taiRoot

    )
  }

}
