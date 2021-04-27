/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.name.Named
import controllers.{ErrorPagesHandler, TaiBaseController}
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.DecisionCacheWrapper
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.domain.BenefitComponentType
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{JourneyCacheConstants, TaiConstants, UpdateOrRemoveCompanyBenefitDecisionConstants}
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel
import views.html.benefits.updateOrRemoveCompanyBenefitDecision
import views.html.{error_no_primary, error_template_noauth}
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class CompanyBenefitController @Inject()(
  employmentService: EmploymentService,
  decisionCacheWrapper: DecisionCacheWrapper,
  @Named("End Company Benefit") journeyCacheService: JourneyCacheService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  updateOrRemoveCompanyBenefitDecision: updateOrRemoveCompanyBenefitDecision,
  override val error_template_noauth: error_template_noauth,
  override val error_no_primary: error_no_primary,
  override implicit val templateRenderer: TemplateRenderer,
  override implicit val partialRetriever: FormPartialRetriever,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with UpdateOrRemoveCompanyBenefitDecisionConstants {

  private val logger = Logger(this.getClass)

  def redirectCompanyBenefitSelection(empId: Int, benefitType: BenefitComponentType): Action[AnyContent] =
    (authenticate andThen validatePerson).async { implicit request =>
      val cacheValues = Map(
        EndCompanyBenefit_EmploymentIdKey -> empId.toString,
        EndCompanyBenefit_BenefitTypeKey  -> benefitType.toString)

      journeyCacheService.cache(cacheValues) map { _ =>
        Redirect(controllers.benefits.routes.CompanyBenefitController.decision())
      }

    }

  def decision: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    (for {
      currentCache <- journeyCacheService.currentCache
      employment <- employmentService
                     .employment(user.nino, currentCache(EndCompanyBenefit_EmploymentIdKey).toInt)
      decision <- decisionCacheWrapper.getDecision()
    } yield {
      employment match {
        case Some(employment) =>
          val referer = currentCache.get(EndCompanyBenefit_RefererKey) match {
            case Some(value) => value
            case None =>
              request.headers.get("Referer").getOrElse(controllers.routes.TaxAccountSummaryController.onPageLoad.url)
          }

          val form =
            UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(decision)

          val viewModel = CompanyBenefitDecisionViewModel(
            currentCache(EndCompanyBenefit_BenefitTypeKey),
            employment.name,
            form
          )

          val cache = Map(
            EndCompanyBenefit_EmploymentNameKey -> employment.name,
            EndCompanyBenefit_BenefitNameKey    -> viewModel.benefitName,
            EndCompanyBenefit_RefererKey        -> referer
          )

          journeyCacheService.cache(cache).map { _ =>
            Ok(updateOrRemoveCompanyBenefitDecision(viewModel))
          }

        case None => throw new RuntimeException("No employment found")
      }
    }).flatMap(identity) recover {
      case NonFatal(e) => errorPagesHandler.internalServerError("CompanyBenefitController exception", Some(e))
    }
  }

  def submitDecisionRedirect(decision: String, failureRoute: Result) =
    decision match {
      case NoIDontGetThisBenefit => {
        Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.stopDate())
      }
      case YesIGetThisBenefit => {
        Redirect(
          controllers.routes.ExternalServiceRedirectController
            .auditAndRedirectService(TaiConstants.CompanyBenefitsIform))
      }
      case _ => {
        logger.error(s"Bad Option provided in submitDecision form: $decision")
        failureRoute
      }
    }

  def submitDecision: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user = request.taiUser

    UpdateOrRemoveCompanyBenefitDecisionForm.form.bindFromRequest.fold(
      formWithErrors => {
        journeyCacheService.currentCache.map { currentCache =>
          val viewModel = CompanyBenefitDecisionViewModel(
            currentCache(EndCompanyBenefit_BenefitTypeKey),
            currentCache(EndCompanyBenefit_EmploymentNameKey),
            formWithErrors)
          BadRequest(updateOrRemoveCompanyBenefitDecision(viewModel))
        }
      },
      success => {
        decisionCacheWrapper.cacheDecision(success.getOrElse(""), submitDecisionRedirect)
      }
    )
  }
}
