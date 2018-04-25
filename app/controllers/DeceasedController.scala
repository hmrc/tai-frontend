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
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.service.PersonService

import scala.concurrent.Future


trait DeceasedController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable {

  def personService: PersonService

  def deceased() = authorisedForTai(personService).async {
    implicit user => implicit taiRoot => implicit request =>
      Future.successful(Ok(views.html.deceased_helpline()))
  }
}

object DeceasedController extends DeceasedController with AuthenticationConnectors {
  override val personService = PersonService
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}
