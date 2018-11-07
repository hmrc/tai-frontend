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

import controllers.{AuthenticationConnectors, TaiBaseController}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.service.{JourneyCacheService, UpdateNextYearsIncomeService}
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

trait UpdateIncomeNextYearController extends TaiBaseController {

  def start(employmentId: Int): Action[AnyContent] = {
//    implicit person =>
//      implicit request =>
//    for {
//      model: UpdateNextYearsIncomeCacheModel <- updateNextYearIncomeService.setup(employmentId)
//    } yield {
//      viewModel = new ViewModel(model)
//      Ok(view(cacheViewModel))
//    }


    ???
  }
  def edit(employmentId: Int): Action[AnyContent] = ???
  def confirm(employmentId: Int): Action[AnyContent] = ???
  def success(employmentId: Int): Action[AnyContent] = ???

}

object UpdateIncomeNextYearController
  extends UpdateIncomeNextYearController
    with AuthenticationConnectors {

  override implicit def templateRenderer: TemplateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever

  val updateNextYearIncomeService: UpdateNextYearsIncomeService = new UpdateNextYearsIncomeService

}
