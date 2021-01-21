/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, NotFound}
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.metrics.Metrics
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.viewModels.JrsClaimsViewModel

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JrsClaimsController @Inject()(
  val auditConnector: AuditConnector,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  jrsService: JrsService,
  metrics: Metrics,
  mcc: MessagesControllerComponents,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def getJrsClaims(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    jrsService.getJrsClaims(nino).map {
      case Right(jrsClaimsData) => Ok(views.html.jrsClaimSummary(JrsClaimsViewModel(jrsClaimsData)))
      case Left(response)       => handleNonSuccessResponse(response)
    } recoverWith {
      case ex: BadRequestException => Future.successful(BadRequest(ex.message))
      case ex: NotFoundException   => Future.successful(NotFound(ex.message))
      case ex                      => throw ex
    }
  }

  def handleNonSuccessResponse(response: HttpResponse): Result =
    response.status match {

      case NOT_FOUND           => NotFound(response.body)
      case NO_CONTENT          => NoContent
      case FORBIDDEN           => Forbidden(response.body)
      case BAD_REQUEST         => BadRequest(response.body)
      case BAD_GATEWAY         => BadGateway(response.body)
      case GATEWAY_TIMEOUT     => GatewayTimeout(response.body)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(response.body)
      case UNAUTHORIZED        => Unauthorized(response.body)
      case _                   => InternalServerError(response.body)
    }
}
