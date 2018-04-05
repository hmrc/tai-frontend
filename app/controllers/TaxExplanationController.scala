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
import controllers.auth.{TaiUser, WithAuthorisedForTai}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.SessionData
import uk.gov.hmrc.tai.service.TaiService
import uk.gov.hmrc.tai.viewModels.TaxExplanationViewModel

import scala.concurrent.Future


trait TaxExplanationController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTai
  with Auditable
  with FeatureTogglesConfig {

  def taiService: TaiService

  def taxExplanationPage(): Action[AnyContent] = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user => implicit sessionData => implicit request =>
      getTaxExplanationPage(Nino(user.getNino))
  }

  private[controllers] def getTaxExplanationPage(nino: Nino)
                                                (implicit request: Request[AnyContent],
                                                 user: TaiUser,
                                                 sessionData: SessionData): Future[Result] = {

    val taxExplanationViewModel = TaxExplanationViewModel(sessionData.taxSummaryDetailsCY, scottishTaxRateEnabled)

    Future(Ok(views.html.howIncomeTaxIsCalculated(taxExplanationViewModel)))

  } recoverWith handleErrorResponse("getTaxExplanationPage", nino)

}

object TaxExplanationController extends TaxExplanationController with AuthenticationConnectors {
  override val taiService = TaiService
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}
