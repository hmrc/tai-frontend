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
import pages.BenefitDecisionPage
import pages.benefits._
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheRepository
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.model.UserAnswers
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
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  updateOrRemoveCompanyBenefitDecision: UpdateOrRemoveCompanyBenefitDecisionView,
  journeyCacheRepository: JourneyCacheRepository,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with Logging {

  lazy val journeyStartRedirection: Result = Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad().url)

  def redirectCompanyBenefitSelection(empId: Int, benefitType: BenefitComponentType): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      for {
        _ <- journeyCacheRepository.set(
               request.userAnswers
                 .setOrException(EndCompanyBenefitsIdPage, empId)
                 .setOrException(EndCompanyBenefitsTypePage, benefitType.toString)
             )
      } yield Redirect(controllers.benefits.routes.CompanyBenefitController.decision())
    }

  private def getDecision(userAnswers: UserAnswers): Future[Option[String]] = {
    val benefitType: Option[String] = userAnswers.get(EndCompanyBenefitsTypePage)

    benefitType match {
      case Some(_) => Future.successful(userAnswers.get(BenefitDecisionPage))
      case None    => Future.successful(None)
    }
  }

  def decision: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (for {
      employment <- employmentService.employmentOnly(user.nino, request.userAnswers.get(EndCompanyBenefitsIdPage).get)
      decision   <- getDecision(request.userAnswers)
    } yield employment match {
      case Some(employment) =>
        val referer = request.userAnswers.get(EndCompanyBenefitsRefererPage) match {
          case Some(value) =>
            value
          case None        =>
            request.headers.get("Referer").getOrElse(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
        }
        val form    =
          UpdateOrRemoveCompanyBenefitDecisionForm.form.fill(decision)

        val viewModel = CompanyBenefitDecisionViewModel(
          request.userAnswers.get(EndCompanyBenefitsTypePage).get,
          employment.name,
          form,
          employment.sequenceNumber
        )

        for {
          _ <- journeyCacheRepository.set(
                 request.userAnswers
                   .setOrException(EndCompanyBenefitsEmploymentNamePage, employment.name)
                   .setOrException(EndCompanyBenefitsNamePage, viewModel.benefitName)
                   .setOrException(EndCompanyBenefitsRefererPage, referer)
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
      case UpdateOrRemoveCompanyBenefitDecisionConstants.YesIGetThisBenefit    =>
        Redirect(
          controllers.routes.ExternalServiceRedirectController
            .auditAndRedirectService(TaiConstants.CompanyBenefitsIform)
        )
      case _                                                                   =>
        failureRoute
    }

  def submitDecision: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    UpdateOrRemoveCompanyBenefitDecisionForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val viewModel = CompanyBenefitDecisionViewModel(
            request.userAnswers.get(EndCompanyBenefitsTypePage).get,
            request.userAnswers.get(EndCompanyBenefitsEmploymentNamePage).get,
            formWithErrors,
            request.userAnswers.get(EndCompanyBenefitsIdPage).get
          )
          Future.successful(BadRequest(updateOrRemoveCompanyBenefitDecision(viewModel)))
        },
        success => {
          val decision                          = success.getOrElse("")
          val benefitTypeFuture: Option[String] = request.userAnswers.get(EndCompanyBenefitsTypePage)

          benefitTypeFuture match {
            case Some(_) =>
              journeyCacheRepository.set(request.userAnswers.setOrException(BenefitDecisionPage, decision)).map { _ =>
                submitDecisionRedirect(decision, journeyStartRedirection)
              }
            case None    =>
              Future.successful(journeyStartRedirection)
          }
        }
      )
  }

}
