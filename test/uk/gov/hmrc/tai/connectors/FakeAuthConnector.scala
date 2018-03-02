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

package TestConnectors
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}

class FakeAuthConnector extends AuthConnector {
  override val serviceUrl: String = "some-url"
  override def http: HttpGet = ???
  val nino:Option[String]= Some(new Generator().nextNino.nino)

  override def currentAuthority(implicit hc : HeaderCarrier, ec: ExecutionContext) : Future[Option[Authority]] = {
    nino match {
      case Some(nino) => Future.successful(
        Some(Authority(uri = s"/path/to/authority",
          accounts = Accounts(paye = Some(PayeAccount(s"/tai/$nino", Nino(nino))),
            tai = Some(TaxForIndividualsAccount(s"/tai/$nino", Nino(nino)))),
          loggedInAt = None,
          previouslyLoggedInAt = None,
          credentialStrength = CredentialStrength.Strong,
          confidenceLevel = ConfidenceLevel.L200,
          userDetailsLink = Some("/user-details/jjjbbb"),
          enrolments = Some("/auth/oid/jbloggs/enrolments"),
          ids = Some("/auth/oid/jjjbbb/ids"),
          legacyOid = "jjjbbb")))
      case None => Future.successful(None)
    }
  }
}
object FakeAuthConnector extends FakeAuthConnector
