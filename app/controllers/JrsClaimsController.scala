/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import controllers.auth.AuthJourney
import play.api.mvc._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.service._
import views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsClaimsController @Inject() (
  val auditConnector: AuditConnector,
  authenticate: AuthJourney,
  jrsService: JrsService,
  mcc: MessagesControllerComponents,
  appConfig: ApplicationConfig,
  jrsClaimSummary: JrsClaimSummaryView,
  internalServerError: InternalServerErrorView,
  noJrsClaim: NoJrsClaimView
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def onPageLoad(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    val nino = request.taiUser.nino

    if (appConfig.jrsClaimsEnabled) {

      jrsService
        .getJrsClaims(nino)
        .fold(
          NotFound(noJrsClaim(appConfig))
        )(jrsClaims => Ok(jrsClaimSummary(jrsClaims, appConfig)))
    } else {
      Future.successful(InternalServerError(internalServerError(appConfig)))
    }
  }
}
