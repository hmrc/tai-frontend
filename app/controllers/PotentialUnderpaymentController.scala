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

import com.google.inject.Inject
import controllers.actions.ValidatePerson
import controllers.audit.Auditable
import controllers.auth.{AuthAction, WithAuthorisedForTaiLite}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.service.{AuditService, CodingComponentService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.PotentialUnderpaymentViewModel

class PotentialUnderpaymentController @Inject()(taxAccountService: TaxAccountService,
                                                codingComponentService: CodingComponentService,
                                                auditService: AuditService,
                                                personService: PersonService,
                                                val auditConnector: AuditConnector,
                                                authenticate: AuthAction,
                                                validatePerson: ValidatePerson,
                                                override implicit val partialRetriever: FormPartialRetriever,
                                                override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with Auditable
  with AuditConstants {

  def potentialUnderpaymentPage(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request => {
      implicit val user = request.taiUser
      val nino = request.taiUser.nino

      sendActingAttorneyAuditEvent("getPotentialUnderpaymentPage")
      val tasFuture = taxAccountService.taxAccountSummary(nino, TaxYear())
      val ccFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())

      for {
        TaiSuccessResponseWithPayload(tas: TaxAccountSummary) <- tasFuture
        ccs <- ccFuture
      } yield {
        auditService.createAndSendAuditEvent(PotentialUnderpayment_InYearAdjustment, Map("nino" -> nino.toString()))
        val vm = PotentialUnderpaymentViewModel(tas, ccs)
        Ok(views.html.potentialUnderpayment(vm))
      }
    } recoverWith handleErrorResponse("getPotentialUnderpaymentPage", request.taiUser.nino)
  }
}
