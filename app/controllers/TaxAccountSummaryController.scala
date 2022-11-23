/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http._
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, TaiConstants}
import views.html.IncomeTaxSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class TaxAccountSummaryController @Inject()(
                                             employmentService: EmploymentService,
                                             taxAccountService: TaxAccountService,
                                             taxAccountSummaryService: TaxAccountSummaryService,
                                             auditService: AuditService,
                                             authenticate: AuthAction,
                                             validatePerson: ValidatePerson,
                                             appConfig: ApplicationConfig,
                                             mcc: MessagesControllerComponents,
                                             incomeTaxSummary: IncomeTaxSummaryView,
                                             implicit val templateRenderer: TemplateRenderer,
                                             errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
  extends TaiBaseController(mcc) with Logging {

  def onPageLoad: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    auditService
      .createAndSendAuditEvent(AuditConstants.TaxAccountSummaryUserEntersSummaryPage, Map("nino" -> nino.toString()))

    taxAccountService
      .taxAccountSummary(nino, TaxYear())
      .flatMap {
        taxAccountSummary =>
          taxAccountSummaryService.taxAccountSummaryViewModel(nino, taxAccountSummary) map { vm =>
            Ok(incomeTaxSummary(vm, appConfig))
          }
      }.recover {
      case e: NotFoundException =>
        logger.warn(s"No tax account information found: ${e.getMessage}")
        Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage())
      case NonFatal(e) if e.getMessage.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) ||
        e.getMessage.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) =>
        Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage())
      case NonFatal(e) =>
        errorPagesHandler.internalServerError(e.getMessage, Some(e))
      case e: UnauthorizedException =>
        logger.warn("taxAccountSummary failed with: " + e.getMessage)
        Redirect(controllers.routes.UnauthorisedController.onPageLoad())
    }
  }
}
