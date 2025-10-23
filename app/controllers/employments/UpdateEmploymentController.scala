/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.employments

import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.updateEmployment._
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repository.JourneyCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.UpdateEmploymentDetailsForm
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.util.{EmpIdCheck, Referral}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, UpdateEmploymentCheckYourAnswersViewModel}
import views.html.CanWeContactByPhoneView
import views.html.employments.ConfirmationView
import views.html.employments.update.{UpdateEmploymentCheckYourAnswersView, WhatDoYouWantToTellUsView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UpdateEmploymentController @Inject() (
  employmentService: EmploymentService,
  val auditConnector: AuditConnector,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  whatDoYouWantToTellUs: WhatDoYouWantToTellUsView,
  canWeContactByPhone: CanWeContactByPhoneView,
  updateEmploymentCheckYourAnswersView: UpdateEmploymentCheckYourAnswersView,
  confirmationView: ConfirmationView,
  errorPagesHandler: ErrorPagesHandler,
  journeyCacheRepository: JourneyCacheRepository,
  empIdCheck: EmpIdCheck
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with Referral
    with EmptyCacheRedirect {

  def cancel(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino).map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  def telephoneNumberViewModel(id: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("tai.updateEmployment.whatDoYouWantToTellUs.preHeading"),
      messages("tai.canWeContactByPhone.title"),
      controllers.employments.routes.UpdateEmploymentController.updateEmploymentDetails(id).url,
      controllers.employments.routes.UpdateEmploymentController.submitTelephoneNumber().url,
      controllers.employments.routes.UpdateEmploymentController.cancel(id).url
    )

  def updateEmploymentDetails(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      empIdCheck.checkValidId(empId).flatMap {
        case Some(result) => Future.successful(result)
        case _            =>
          (for {
            employment   <- employmentService.employmentOnly(user.nino, empId)
            futureResult <- employment match {
                              case Some(emp) =>
                                for {
                                  _ <- journeyCacheRepository
                                         .set(
                                           request.userAnswers
                                             .setOrException(UpdateEmploymentIdPage, empId)
                                             .setOrException(UpdateEmploymentNamePage, emp.name)
                                         )
                                } yield Ok(
                                  whatDoYouWantToTellUs(
                                    EmploymentViewModel(emp.name, empId),
                                    UpdateEmploymentDetailsForm.form.fill(
                                      request.userAnswers.get(UpdateEmploymentDetailsPage).getOrElse("")
                                    )
                                  )
                                )

                              case _ =>
                                Future.successful(
                                  errorPagesHandler.internalServerError("Error during employment details retrieval")
                                )
                            }
          } yield futureResult).recover { case NonFatal(exception) =>
            errorPagesHandler.internalServerError(exception.getMessage)
          }
      }

  }

  def submitUpdateEmploymentDetails(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      UpdateEmploymentDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            implicit val user: AuthedUser = request.taiUser
            Future.successful(
              BadRequest(
                whatDoYouWantToTellUs(
                  EmploymentViewModel(request.userAnswers.get(UpdateEmploymentNamePage).getOrElse(""), empId),
                  formWithErrors
                )
              )
            )
          },
          employmentDetails =>
            journeyCacheRepository
              .set(request.userAnswers.setOrException(UpdateEmploymentDetailsPage, employmentDetails))
              .map(_ => Redirect(controllers.employments.routes.UpdateEmploymentController.addTelephoneNumber()))
        )
  }

  def addTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val employmentId              = request.userAnswers.get(UpdateEmploymentIdPage)
    val telephoneQuestion         = request.userAnswers.get(UpdateEmploymentTelephoneQuestionPage)
    val telephoneNumber           = request.userAnswers.get(UpdateEmploymentTelephoneNumberPage)

    employmentId match {
      case Some(empId) =>
        Future.successful(
          Ok(
            canWeContactByPhone(
              Some(user),
              telephoneNumberViewModel(empId),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneQuestion, telephoneNumber))
            )
          )
        )
      case _           => Future.successful(Redirect(taxAccountSummaryRedirect))
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    def setNoValueForTelephoneQuestion(form: YesNoTextEntryForm) =
      request.userAnswers
        .setOrException(
          UpdateEmploymentTelephoneQuestionPage,
          Messages(s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}")
        )

    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.enter.number"),
        Some(TelephoneNumberConstraint.telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val employmentId              = request.userAnswers.get(UpdateEmploymentIdPage)
          implicit val user: AuthedUser = request.taiUser
          Future.successful(
            BadRequest(canWeContactByPhone(Some(user), telephoneNumberViewModel(employmentId.get), formWithErrors))
          )
        },
        form => {
          val yesNoTask = form.yesNoChoice match {
            case Some(yn) if yn == FormValuesConstants.YesValue =>
              journeyCacheRepository.set(
                setNoValueForTelephoneQuestion(form)
                  .setOrException(UpdateEmploymentTelephoneNumberPage, form.yesNoTextEntry.getOrElse(""))
              )
            case _                                              =>
              journeyCacheRepository.set(
                setNoValueForTelephoneQuestion(form)
                  .setOrException(UpdateEmploymentTelephoneNumberPage, "")
              )
          }
          yesNoTask.map(_ =>
            Redirect(controllers.employments.routes.UpdateEmploymentController.updateEmploymentCheckYourAnswers())
          )
        }
      )
  }

  def updateEmploymentCheckYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser   = request.taiUser
      val employmentId                = request.userAnswers.get(UpdateEmploymentIdPage)
      val employmentName              = request.userAnswers.get(UpdateEmploymentNamePage)
      val employmentDetails           = request.userAnswers.get(UpdateEmploymentDetailsPage)
      val employmentTelephoneQuestion = request.userAnswers.get(UpdateEmploymentTelephoneQuestionPage)
      val employmentTelephoneNumber   = request.userAnswers.get(UpdateEmploymentTelephoneNumberPage)

      (employmentId, employmentName, employmentDetails, employmentTelephoneQuestion, employmentTelephoneNumber) match {

        case (Some(empId), Some(empName), Some(empDetails), Some(empTelQuestion), _) =>
          Future.successful(
            Ok(
              updateEmploymentCheckYourAnswersView(
                UpdateEmploymentCheckYourAnswersViewModel(
                  empId,
                  empName,
                  empDetails,
                  empTelQuestion,
                  employmentTelephoneNumber
                )
              )
            )
          )
        case _                                                                       =>
          Future.successful(Redirect(taxAccountSummaryRedirect))
      }
  }

  def submitYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser   = request.taiUser
    val employmentId                = request.userAnswers.get(UpdateEmploymentIdPage)
    val employmentDetails           = request.userAnswers.get(UpdateEmploymentDetailsPage)
    val employmentTelephoneQuestion = request.userAnswers.get(UpdateEmploymentTelephoneQuestionPage)
    val employmentTelephoneNumber   = request.userAnswers.get(UpdateEmploymentTelephoneNumberPage)

    (employmentId, employmentDetails, employmentTelephoneQuestion) match {

      case (Some(empId), Some(empDetails), Some(empTelQuestion)) =>
        val model = IncorrectIncome(empDetails, empTelQuestion, employmentTelephoneNumber)
        for {
          _ <- employmentService.incorrectEmployment(user.nino, empId, model)
          _ <- journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
          _ <- {
            // setting for tracking service
            val updatedUserAnswers =
              UserAnswers(request.userAnswers.sessionId, request.userAnswers.nino)
                .setOrException(UpdateEndEmploymentPage(empId), true)
            journeyCacheRepository.set(updatedUserAnswers)
          }
        } yield Redirect(controllers.employments.routes.UpdateEmploymentController.confirmation())

      case _ => Future.successful(Redirect(taxAccountSummaryRedirect))
    }
  }

  def confirmation: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    Future.successful(Ok(confirmationView()))
  }
}
