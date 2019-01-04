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

package uk.gov.hmrc.tai.connectors

import com.google.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.model.UserDetails

import scala.concurrent.Future

class UserDetailsConnector @Inject()(val http: CoreGet) {

  def userDetails(userDetailsUri: String)(implicit hc: HeaderCarrier): Future[UserDetails] = {
    Logger.debug(s"Calling User Details with uri: $userDetailsUri")
    http.GET[UserDetails](userDetailsUri)
  }

  def userDetails(authContext: AuthContext)(implicit hc: HeaderCarrier): Future[UserDetails] = {
    authContext.userDetailsUri match {
      case Some(ud: String) =>
        userDetails(ud)
      case _ =>
        Logger.info("User details URI couldn't be found!!!")
        Future.failed(new RuntimeException("User details URI couldn't be found!!!"))
    }
  }
}
