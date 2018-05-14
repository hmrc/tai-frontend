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

import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.domain.ConfidenceLevel
import uk.gov.hmrc.tai.util.TaiConstants
import uk.gov.hmrc.tai.util.TaiConstants.{CompletionUrl, FailureUrl, Origin}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.tai.config
import uk.gov.hmrc.tai.config.ApplicationConfig

trait TaiConfidenceLevelPredicate extends PageVisibilityPredicate {

  def origin: String

  def completionUrl: String

  def failureUrl: String

  def upliftUrl: String

  def apply(authContext: AuthContext, request: Request[AnyContent]): Future[PageVisibilityResult] = {

    implicit val hc = fromHeadersAndSession(request.headers, Some(request.session))

    if (has200ConfidenceLevel(authContext)) {
      Future.successful(PageIsVisible)
    }
    else {
      Future.successful(PageBlocked(Future.successful(buildIVUpliftUrl(ConfidenceLevel.L200))))
    }
  }

  private def has200ConfidenceLevel(authContext: AuthContext) = {
    authContext.user.confidenceLevel >= ConfidenceLevel.L200
  }

  private def buildIVUpliftUrl(confidenceLevel: ConfidenceLevel) =
    Redirect(
      upliftUrl,
      Map(Origin -> Seq(origin),
        TaiConstants.ConfidenceLevel -> Seq(confidenceLevel.level.toString),
        CompletionUrl -> Seq(completionUrl),
        FailureUrl -> Seq(failureUrl)
      )
    )
}
// $COVERAGE-OFF$
object TaiConfidenceLevelPredicate extends TaiConfidenceLevelPredicate {
  override def upliftUrl: String = ApplicationConfig.sa16UpliftUrl

  override def failureUrl: String = config.ApplicationConfig.pertaxServiceUpliftFailedUrl

  override def completionUrl: String = config.ApplicationConfig.taiFrontendServiceUrl

  override def origin: String = "TAI"
}
// $COVERAGE-ON$
