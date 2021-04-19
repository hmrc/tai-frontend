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

import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.UnauthorizedException
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse, TaiUnauthorisedResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, TaiConstants}
import views.html.{error_no_primary, error_template_noauth, incomeTaxSummary}

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
  incomeTaxSummary: incomeTaxSummary,
  override val error_template_noauth: error_template_noauth,
  override val error_no_primary: error_no_primary,
  override implicit val partialRetriever: FormPartialRetriever,
  override implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with AuditConstants {

  def onPageLoad: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    auditService.createAndSendAuditEvent(TaxAccountSummary_UserEntersSummaryPage, Map("nino" -> nino.toString()))

    taxAccountService
      .taxAccountSummary(nino, TaxYear())
      .flatMap {
        case TaiNotFoundResponse(_) =>
          Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
        case TaiTaxAccountFailureResponse(message)
            if message.toLowerCase.contains(TaiConstants.NpsTaxAccountDataAbsentMsg) ||
              message.toLowerCase.contains(TaiConstants.NpsNoEmploymentForCurrentTaxYear) =>
          Future.successful(Redirect(routes.NoCYIncomeTaxErrorController.noCYIncomeTaxErrorPage()))
        case TaiSuccessResponseWithPayload(taxAccountSummary: TaxAccountSummary) =>
          taxAccountSummaryService.taxAccountSummaryViewModel(nino, taxAccountSummary) map { vm =>
            Ok(incomeTaxSummary(vm, appConfig))
          }
        case TaiTaxAccountFailureResponse(message) =>
          throw new RuntimeException(s"Failed to fetch tax account summary details with exception: $message")
        case TaiUnauthorisedResponse(message) =>
          Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad()))
      }
      .recover {
        case e: UnauthorizedException =>
          Logger.warn("taxAccountSummary failed with: " + e.getMessage)
          Redirect(controllers.routes.UnauthorisedController.onPageLoad())
        case NonFatal(e) => internalServerError(e.getMessage, Some(e))
      }
  }
}
