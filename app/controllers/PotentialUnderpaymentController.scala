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

package controllers

import controllers.audit.Auditable
import controllers.auth.{WithAuthorisedForTai, WithAuthorisedForTaiLite}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.domain.TaxAccountSummary
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.service.{AuditService, CodingComponentService, TaiService, TaxAccountService}
import uk.gov.hmrc.tai.util.AuditConstants
import uk.gov.hmrc.tai.viewModels.PotentialUnderpaymentViewModelNEW

import scala.concurrent.Future

trait PotentialUnderpaymentController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with AuditConstants {

  def taiService: TaiService
  def codingComponentService: CodingComponentService
  def taxAccountService: TaxAccountService
  def auditService: AuditService

  def potentialUnderpaymentPage(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            Future.successful(Ok(""))
          }
  }

}

object PotentialUnderpaymentController extends PotentialUnderpaymentController with AuthenticationConnectors {
  override def taiService: TaiService = TaiService
  override def codingComponentService: CodingComponentService = CodingComponentService
  override def taxAccountService: TaxAccountService = TaxAccountService
  override def auditService: AuditService = AuditService
  override implicit def templateRenderer: TemplateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever
}
