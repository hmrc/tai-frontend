/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.auth.{AuthAction, AuthedUser}

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import cats.implicits._
import play.api.Logger
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.service.{AuditService, CodingComponentService, TaxAccountService}
import uk.gov.hmrc.tai.util.Referral
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.PotentialUnderpaymentViewModel
import views.html.PotentialUnderpaymentView

import scala.concurrent.ExecutionContext

class PotentialUnderpaymentController @Inject() (
  taxAccountService: TaxAccountService,
  codingComponentService: CodingComponentService,
  auditService: AuditService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  potentialUnderpayment: PotentialUnderpaymentView,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Referral {

  def potentialUnderpaymentPage(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      {

        implicit val user: AuthedUser = request.taiUser
        val nino = user.nino
        (
          taxAccountService.taxAccountSummary(nino, TaxYear()),
          codingComponentService.taxFreeAmountComponents(nino, TaxYear())
        ).mapN { case (tas, ccs) =>
          auditService.createAndSendAuditEvent(
            AuditConstants.PotentialUnderpaymentInYearAdjustment,
            Map("nino" -> nino.toString())
          )
          val vm = PotentialUnderpaymentViewModel(tas, ccs, referer, resourceName)
          Ok(potentialUnderpayment(vm))
        }
      } recover { case e: Exception =>
        errorPagesHandler.internalServerError(e.getMessage, Some(e))
      }
  }
}
