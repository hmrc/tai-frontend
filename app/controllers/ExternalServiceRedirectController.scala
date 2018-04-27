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

import controllers.auth.WithAuthorisedForTaiLite
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.service.{AuditService, SessionService, PersonService}

trait ExternalServiceRedirectController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite {

  def personService: PersonService

  def auditService: AuditService

  def sessionService: SessionService

  def auditInvalidateCacheAndRedirectService(serviceAndIFormName: String): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request => {
          ServiceCheckLite.personDetailsCheck {
            for {
              redirectUri <- auditService.sendAuditEventAndGetRedirectUri(Nino(user.getNino), serviceAndIFormName)
              _ <- sessionService.invalidateCache()
            } yield Redirect(redirectUri)
          }
        }
  }
}
// $COVERAGE-OFF$
object ExternalServiceRedirectController extends ExternalServiceRedirectController with AuthenticationConnectors {
  override implicit def templateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  override val personService = PersonService
  override val auditService = AuditService
  override val sessionService = SessionService
}
// $COVERAGE-ON$