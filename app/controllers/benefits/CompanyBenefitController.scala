/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.tai.DecisionCacheWrapper
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.domain.BenefitComponentType
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{TaiConstants, UpdateOrRemoveCompanyBenefitDecisionConstants}
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel
import views.html.benefits.UpdateOrRemoveCompanyBenefitDecisionView

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class CompanyBenefitController @Inject() (
  employmentService: EmploymentService,
  decisionCacheWrapper: DecisionCacheWrapper,
  @Named("End Company Benefit") journeyCacheService: JourneyCacheService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  updateOrRemoveCompanyBenefitDecision: UpdateOrRemoveCompanyBenefitDecisionView,
  implicit val errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  def redirectCompanyBenefitSelection(empId: Int, benefitType: BenefitComponentType): Action[AnyContent] =
    authenticate.authWithValidatePerson.async { implicit request =>
      val cacheValues = Map(
        EndCompanyBenefitConstants.EmploymentIdKey -> empId.toString,
        EndCompanyBenefitConstants.BenefitTypeKey  -> benefitType.toString
      )

      journeyCacheService.cache(cacheValues) map { _ =>
        Redirect(controllers.benefits.routes.CompanyBenefitController.decision())
      }

    }

  def decision: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (for {
      currentCache <- journeyCacheService.currentCache
      employment <- employmentService
                      .employment(user.nino, currentCache(EndCompanyBenefitConstants.EmploymentIdKey).toInt)
      decision <- decisionCacheWrapper.getDecision()
    } yield employment match {
      case Some(employment) =>
        val referer = currentCache.get(EndCompanyBenefitConstants.RefererKey) match {
          case Some(value) => value
          case None =>
            request.headers.get("Referer").getOrElse(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
        }

        val form =
          UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(decision)

        val viewModel = CompanyBenefitDecisionViewModel(
          currentCache(EndCompanyBenefitConstants.BenefitTypeKey),
          employment.name,
          form,
          employment.sequenceNumber
        )

        val cache = Map(
          EndCompanyBenefitConstants.EmploymentNameKey -> employment.name,
          EndCompanyBenefitConstants.BenefitNameKey    -> viewModel.benefitName,
          EndCompanyBenefitConstants.RefererKey        -> referer
        )

        journeyCacheService.cache(cache).map { _ =>
          Ok(updateOrRemoveCompanyBenefitDecision(viewModel))
        }

      case None => throw new RuntimeException("No employment found")
    }).flatten recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError("CompanyBenefitController exception", Some(e))
    }
  }

  def submitDecisionRedirect(decision: String, failureRoute: Result): Result =
    decision match {
      case UpdateOrRemoveCompanyBenefitDecisionConstants.NoIDontGetThisBenefit =>
        Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.stopDate())
      case UpdateOrRemoveCompanyBenefitDecisionConstants.YesIGetThisBenefit =>
        Redirect(
          controllers.routes.ExternalServiceRedirectController
            .auditAndRedirectService(TaiConstants.CompanyBenefitsIform)
        )
      case _ =>
        logger.error(s"Bad Option provided in submitDecision form: $decision")
        failureRoute
    }

  def submitDecision: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    UpdateOrRemoveCompanyBenefitDecisionForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheService.currentCache.map { currentCache =>
            val viewModel = CompanyBenefitDecisionViewModel(
              currentCache(EndCompanyBenefitConstants.BenefitTypeKey),
              currentCache(EndCompanyBenefitConstants.EmploymentNameKey),
              formWithErrors,
              currentCache(EndCompanyBenefitConstants.EmploymentIdKey).toInt
            )
            BadRequest(updateOrRemoveCompanyBenefitDecision(viewModel))
          },
        success => decisionCacheWrapper.cacheDecision(success.getOrElse(""), submitDecisionRedirect)
      )
  }
}
