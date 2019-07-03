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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.service.{AuditService, SessionService}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

class ExternalServiceRedirectController @Inject()(sessionService: SessionService,
                                                  auditService: AuditService,
                                                  authenticate: AuthAction,
                                                  validatePerson: ValidatePerson,
                                                  override implicit val partialRetriever: FormPartialRetriever,
                                                  override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  def auditInvalidateCacheAndRedirectService(serviceAndIFormName: String): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request => {

      (for {
        redirectUri <- auditService.sendAuditEventAndGetRedirectUri(request.taiUser.nino, serviceAndIFormName)
        _ <- sessionService.invalidateCache()
      } yield {
        Redirect(redirectUri)
      }) recover {
        case _ => internalServerError("Unable to audit and redirect")
      }
    }
  }
}
