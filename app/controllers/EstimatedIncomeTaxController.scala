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

import controllers.ServiceChecks.CustomRule
import controllers.audit.Auditable
import controllers.auth.{TaiUser, WithAuthorisedForTai}
import controllers.viewModels.{EstimatedIncomePageVM, EstimatedIncomePageVMBuilder}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.auth.ConfigProperties
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.{LocalTemplateRenderer, PreferencesFrontendConnector}
import uk.gov.hmrc.tai.model.SessionData
import uk.gov.hmrc.tai.service.{ActivityLoggerService, HasFormPartialService, TaiService}

import scala.concurrent.Future


trait EstimatedIncomeTaxController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTai
  with Auditable {

  def taiService: TaiService

  def preferencesFrontendConnector: PreferencesFrontendConnector

  def activatePaperless: Boolean
  def activatePaperlessEvenIfGatekeeperFails: Boolean
  def taxPlatformTaiRootLandingPageUri: String
  def activityLoggerService: ActivityLoggerService
  val estimatedIncomePageVM: EstimatedIncomePageVMBuilder

  def partialService: HasFormPartialService

  def estimatedIncomeTax() = authorisedForTai(redirectToOrigin = true)(taiService).async {
    implicit user => implicit sessionData => implicit request =>
      getEstimatedIncomeTax(Nino(user.getNino))
  }

  private def getEstimatedIncomeTax(nino: Nino)(implicit request: Request[AnyContent], user: TaiUser, sessionData : SessionData): Future[Result] = {
    val rule: CustomRule = details => {
      for {
        fModel <- estimatedIncomePageVM.createObject(nino,details)
        iFormLinks <- partialService.getIncomeTaxPartial
      } yield Ok(views.html.estimatedIncomeTax(fModel, webChat = true, iFormLinks successfulContentOrElse Html("")))
    }
    ServiceChecks.executeWithServiceChecks(nino, SimpleServiceCheck, sessionData) {
      Some(rule)
    }
  } recoverWith handleErrorResponse("getEstimatedIncomeTaxPage", nino)

}

object EstimatedIncomeTaxController extends EstimatedIncomeTaxController with AuthenticationConnectors {
  override val taiService = TaiService
  override val activityLoggerService = ActivityLoggerService
  val preferencesFrontendConnector = PreferencesFrontendConnector
  lazy val activatePaperless: Boolean = ConfigProperties.activatePaperless
  lazy val activatePaperlessEvenIfGatekeeperFails: Boolean = ConfigProperties.activatePaperlessEvenIfGatekeeperFails
  lazy val taxPlatformTaiRootLandingPageUri: String =
    s"${ConfigProperties.taxPlatformTaiRootUri}${routes.TaxAccountSummaryController.onPageLoad().url}"
  override implicit def templateRenderer = LocalTemplateRenderer
  override implicit def partialRetriever: PartialRetriever = TaiHtmlPartialRetriever
  override val estimatedIncomePageVM: EstimatedIncomePageVMBuilder = EstimatedIncomePageVM
  override val partialService: HasFormPartialService = HasFormPartialService
}

