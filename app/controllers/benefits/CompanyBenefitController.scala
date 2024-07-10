/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.benefits.{EndCompanyBenefitEmploymentNamePage, EndCompanyBenefitNamePage, EndCompanyBenefitsIdPage, EndCompanyBenefitsTypePage}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.DecisionCacheWrapper
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.domain.BenefitComponentType
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.util.constants.{TaiConstants, UpdateOrRemoveCompanyBenefitDecisionConstants}
import uk.gov.hmrc.tai.viewModels.benefit.CompanyBenefitDecisionViewModel
import views.html.benefits.UpdateOrRemoveCompanyBenefitDecisionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CompanyBenefitController @Inject() (
  employmentService: EmploymentService,
  decisionCacheWrapper: DecisionCacheWrapper,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  updateOrRemoveCompanyBenefitDecision: UpdateOrRemoveCompanyBenefitDecisionView,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  errorPagesHandler: ErrorPagesHandler,
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with Logging {

  def redirectCompanyBenefitSelection(empId: Int, benefitType: BenefitComponentType): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      for {
        _ <- journeyCacheNewRepository.set(
          request.userAnswers
            .setOrException(EndCompanyBenefitsIdPage, empId)
            .setOrException(EndCompanyBenefitsTypePage, benefitType.toString)
        )
      } yield Redirect(controllers.benefits.routes.CompanyBenefitController.decision())
    }

  // scalastyle:off cyclomatic.complexity
  def decision: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (for {
      currentCache <- journeyCacheNewRepository.get(request.userAnswers.sessionId, request.userAnswers.nino)

      employmentId <- currentCache.flatMap(_.get(EndCompanyBenefitsIdPage)) match {
        case Some(id) => Future.successful(id)
        case None => Future.failed(new RuntimeException("Employment ID not found in cache"))
      }

      employment <- employmentService.employment(user.nino, employmentId)
      decision <- decisionCacheWrapper.getDecision()

    } yield employment match {
      case Some(employment) =>
        val referer = currentCache.flatMap(_.get(EndCompanyBenefitNamePage)) match {
          case Some(value) => value
          case None =>
            request.headers.get("Referer").getOrElse(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
        }

        val form =
          UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(decision)

        val benefitType = currentCache.flatMap(_.get(EndCompanyBenefitsTypePage)) match {
          case Some(benefit) => benefit
          case None => throw new RuntimeException("Benefit type not found in cache")
        }

        val viewModel = CompanyBenefitDecisionViewModel(
          benefitType,
          employment.name,
          form,
          employment.sequenceNumber
        )

        for {
          _ <- journeyCacheNewRepository.set(
            request.userAnswers
              .setOrException(EndCompanyBenefitEmploymentNamePage, employment.name)
              .setOrException(EndCompanyBenefitsTypePage, viewModel.benefitName)
              .setOrException(EndCompanyBenefitNamePage, referer)
          )
        } yield Ok(updateOrRemoveCompanyBenefitDecision(viewModel))

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

  def submitDecision: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    UpdateOrRemoveCompanyBenefitDecisionForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheNewRepository.get(request.userAnswers.sessionId, request.userAnswers.nino).map { _ =>
            val viewModel = CompanyBenefitDecisionViewModel(
              EndCompanyBenefitsTypePage,
              EndCompanyBenefitEmploymentNamePage,
              formWithErrors,
              EndCompanyBenefitsIdPage
            )
            BadRequest(updateOrRemoveCompanyBenefitDecision(viewModel))
          },
        success => decisionCacheWrapper.cacheDecision(success.getOrElse(""), submitDecisionRedirect)
      )
  }

}
