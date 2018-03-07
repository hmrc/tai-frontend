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

package controllers.benefits

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import controllers.{AuthenticationConnectors, ServiceCheckLite, TaiBaseController}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.domain.BenefitComponentType
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService, JourneyCacheService, TaiService}
import uk.gov.hmrc.tai.util.{AuditConstants, JourneyCacheConstants, TaiConstants, UpdateOrRemoveCompanyBenefitDecisionConstants}

import scala.concurrent.Future

trait CompanyBenefitController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with AuditConstants
  with JourneyCacheConstants
  with UpdateOrRemoveCompanyBenefitDecisionConstants {

  def taiService: TaiService
  def auditService: AuditService
  def employmentService: EmploymentService
  def journeyCacheService: JourneyCacheService
  def trackingJourneyCacheService: JourneyCacheService

  def redirectCompanyBenefitSelection(empId: Int, benefitType: BenefitComponentType) : Action[AnyContent] = authorisedForTai(taiService).async {
      implicit user =>
        implicit taiRoot =>
          implicit request =>
            ServiceCheckLite.personDetailsCheck {

              val benefitTypeText = Messages(s"tai.taxFreeAmount.table.taxComponent.${benefitType.toString}")
              val cacheValues = Map(EndCompanyBenefit_EmploymentIdKey -> empId.toString, EndCompanyBenefit_BenefitTypeKey -> benefitTypeText)

              journeyCacheService.cache(cacheValues) map {
                _ => Redirect(controllers.benefits.routes.CompanyBenefitController.decision())
              }
            }
  }

  def decision: Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            (for{
              currentCache <- journeyCacheService.currentCache
              employment <- employmentService.employment(Nino(user.getNino), currentCache(EndCompanyBenefit_EmploymentIdKey).toInt)
            } yield {
              employment match {
                case Some(employment) =>
                  val cache = Map(EndCompanyBenefit_EmploymentNameKey -> employment.name)
                  journeyCacheService.cache(cache).map { _ =>
                    Ok(views.html.benefits.updateOrRemoveCompanyBenefitDecision(
                      UpdateOrRemoveCompanyBenefitDecisionForm.form,
                      currentCache(EndCompanyBenefit_BenefitTypeKey),
                      employment.name))
                  }
                case None => throw new RuntimeException("No employment found")
              }
            }).flatMap(identity)
          }
  }


  def submitDecision: Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          UpdateOrRemoveCompanyBenefitDecisionForm.form.bindFromRequest.fold(
            formWithErrors => {
              journeyCacheService.currentCache map { currentCache =>
                BadRequest(views.html.benefits.updateOrRemoveCompanyBenefitDecision(
                  formWithErrors,
                  currentCache(EndCompanyBenefit_BenefitTypeKey),
                  currentCache(EndCompanyBenefit_EmploymentNameKey))
                )
              }
            },
            success =>{
              success match{
                case Some(NoIDontGetThisBenefit) =>
                  Future.successful(Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.stopDate()))
                case Some(YesIGetThisBenefit) =>
                  Future.successful(Redirect(controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.CompanyBenefitsIform).url))
              }
            }
          )
  }
}

object CompanyBenefitController extends CompanyBenefitController with AuthenticationConnectors {
  override val taiService: TaiService = TaiService
  override val auditService: AuditService = AuditService
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: PartialRetriever = TaiHtmlPartialRetriever
  override val employmentService: EmploymentService = EmploymentService
  override val journeyCacheService: JourneyCacheService = JourneyCacheService(EndCompanyBenefit_JourneyKey)
  override val trackingJourneyCacheService: JourneyCacheService = JourneyCacheService(TrackSuccessfulJourney_JourneyKey)
}


