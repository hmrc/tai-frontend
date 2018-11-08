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

package controllers.income

import controllers.audit.Auditable
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import controllers.auth.WithAuthorisedForTaiLite
import controllers.{AuthenticationConnectors, ServiceCheckLite, TaiBaseController}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.service.{PersonService, UpdateNextYearsIncomeService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions

trait UpdateIncomeNextYearController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable {

  val updateNextYearsIncomeService: UpdateNextYearsIncomeService

  def personService: PersonService

  def start(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            updateNextYearsIncomeService.setup(employmentId, Nino(user.getNino)) map { model =>
              println(model)
              Ok(views.html.incomes.nextYear.updateIncomeCYPlus1Start(model))
            }
          }
  }

  def edit(employmentId: Int): Action[AnyContent] = ???
  def confirm(employmentId: Int): Action[AnyContent] = ???
  def success(employmentId: Int): Action[AnyContent] = ???

}

object UpdateIncomeNextYearController extends UpdateIncomeNextYearController with AuthenticationConnectors {
  override val personService = PersonService
  override implicit def templateRenderer: TemplateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  val updateNextYearsIncomeService: UpdateNextYearsIncomeService = new UpdateNextYearsIncomeService

}
