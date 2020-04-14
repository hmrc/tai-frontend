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

package controllers

import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, TaiConstants}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TaxAccountSummaryController @Inject()(
  employmentService: EmploymentService,
  taxAccountService: TaxAccountService,
  taxAccountSummaryService: TaxAccountSummaryService,
  auditService: AuditService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  override val messagesApi: MessagesApi,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController with AuditConstants with FeatureTogglesConfig {

  def onPageLoad: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser
    val nino = user.nino

    auditService.createAndSendAuditEvent(TaxAccountSummary_UserEntersSummaryPage, Map("nino" -> nino.toString()))

    (taxAccountService
      .taxAccountSummary(nino, TaxYear())
      .flatMap {
        case TaiTaxAccountFailureResponse(message)
            if message.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) ||
              message.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) =>
          Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
        case TaiSuccessResponseWithPayload(taxAccountSummary: TaxAccountSummary) =>
          taxAccountSummaryService.taxAccountSummaryViewModel(nino, taxAccountSummary) map { vm =>
            Ok(views.html.incomeTaxSummary(vm))
          }
        case TaiTaxAccountFailureResponse(message) =>
          throw new RuntimeException(s"Failed to fetch tax account summary details with exception: $message")
        case TaiUnauthorisedResponse(message) =>
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
      })
      .recover {
        case e: UnauthorizedException =>
          Logger.warn("taxAccountSummary failed with: " + e.getMessage)
          Redirect(controllers.routes.UnauthorisedController.onPageLoad())
        case NonFatal(e) => internalServerError(e.getMessage, Some(e))
      }
  }
}
