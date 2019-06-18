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

package controllers

import javax.inject.Inject
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.service.{AuditService, CodingComponentService, TaxAccountService}
import uk.gov.hmrc.tai.util.Referral
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.PotentialUnderpaymentViewModel

class PotentialUnderpaymentController @Inject()(taxAccountService: TaxAccountService,
                                                codingComponentService: CodingComponentService,
                                                auditService: AuditService,
                                                authenticate: AuthAction,
                                                validatePerson: ValidatePerson,
                                                override implicit val partialRetriever: FormPartialRetriever,
                                                override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with AuditConstants
  with Referral {

  def potentialUnderpaymentPage(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request => {

      implicit val user = request.taiUser
      val nino = user.nino

      val tasFuture = taxAccountService.taxAccountSummary(nino, TaxYear())
      val ccFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())

      for {
        TaiSuccessResponseWithPayload(tas: TaxAccountSummary) <- tasFuture
        ccs <- ccFuture
      } yield {
        auditService.createAndSendAuditEvent(PotentialUnderpayment_InYearAdjustment, Map("nino" -> nino.toString()))
        val vm = PotentialUnderpaymentViewModel(tas, ccs, referer, resourceName)
        Ok(views.html.potentialUnderpayment(vm))
      }
    } recoverWith handleErrorResponse("getPotentialUnderpaymentPage", request.taiUser.nino)
  }
}
