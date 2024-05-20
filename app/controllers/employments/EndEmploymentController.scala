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

package controllers.employments

import controllers._
import controllers.auth.{AuthJourney, AuthedUser, DataRequest}
import pages.EndEmployment._
import pages._
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc._
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{DuplicateSubmissionWarningForm, EmploymentEndDateForm, IrregularPayForm, UpdateRemoveEmploymentForm}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.{Employment, EndEmployment}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.journeyCache.TrackSuccessfulJourneyConstants
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, IrregularPayConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, WithinSixWeeksViewModel}
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class EndEmploymentController @Inject() (
  auditService: AuditService,
  employmentService: EmploymentService,
  val auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  errorPagesHandler: ErrorPagesHandler,
  updateRemoveEmploymentDecision: UpdateRemoveEmploymentDecisionView,
  endEmploymentWithinSixWeeksError: EndEmploymentWithinSixWeeksErrorView,
  endEmploymentIrregularPaymentError: EndEmploymentIrregularPaymentErrorView,
  endEmploymentView: EndEmploymentView,
  canWeContactByPhone: CanWeContactByPhoneView,
  duplicateSubmissionWarning: DuplicateSubmissionWarningView,
  confirmation: ConfirmationView,
  addIncomeCheckYourAnswers: AddIncomeCheckYourAnswersView,
  authenticate: AuthJourney,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect with Logging {

  def cancel(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheNewRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino).map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  private def error5xxInBadRequest()(implicit request: Request[_]): Result =
    BadRequest(errorPagesHandler.error5xx(Messages("global.error.InternalServerError500.message")))

  private def telephoneNumberViewModel(employmentId: Int)(implicit messages: Messages) =
    CanWeContactByPhoneViewModel(
      messages("tai.endEmployment.preHeadingText"),
      messages("tai.canWeContactByPhone.title"),
      controllers.employments.routes.EndEmploymentController.endEmploymentPage().url,
      controllers.employments.routes.EndEmploymentController.submitTelephoneNumber().url,
      controllers.employments.routes.EndEmploymentController.cancel(employmentId).url
    )

  def employmentUpdateRemoveDecision: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers
        .get(EndEmploymentIdPage)
        .fold(
          Future.successful(error5xxInBadRequest())
        )(empId =>
          employmentService
            .employment(request.taiUser.nino, empId)
            .map(
              _.fold(
                error5xxInBadRequest()
              ) { employment =>
                Ok(
                  updateRemoveEmploymentDecision(
                    UpdateRemoveEmploymentForm
                      .form(employment.name)
                      .fill(request.userAnswers.get(EmploymentDecisionPage)),
                    employment.name,
                    empId
                  )
                )
              }
            )
        )
    }

  def onPageLoad(empId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      request.userAnswers
        .get(EndEmploymentIdPage)
        .fold(
          for {
            _      <- journeyCacheNewRepository.set(request.userAnswers.setOrException(EndEmploymentIdPage, empId))
            result <- checkDuplicateSubmission(empId)
          } yield result
        )(_ => checkDuplicateSubmission(empId))
    }

  private def checkDuplicateSubmission(empId: Int)(implicit hc: HeaderCarrier) =
    successfulJourneyCacheService
      .currentValue(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId")
      .map {
        case Some(_) => Redirect(controllers.employments.routes.EndEmploymentController.duplicateSubmissionWarning())
        case None => Redirect(controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision())
      }

  def handleEmploymentUpdateRemove: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      request.userAnswers
        .get(EndEmploymentIdPage)
        .fold(
          Future.successful(error5xxInBadRequest())
        )(empId =>
          employmentService
            .employment(user.nino, empId)
            .flatMap(
              _.fold(
                Future.successful(error5xxInBadRequest())
              )(employment =>
                UpdateRemoveEmploymentForm
                  .form(employment.name)
                  .bindFromRequest()
                  .fold(
                    formWithErrors =>
                      Future.successful(
                        BadRequest(
                          updateRemoveEmploymentDecision(
                            formWithErrors,
                            employment.name,
                            empId
                          )
                        )
                      ),
                    {
                      case Some(FormValuesConstants.YesValue) =>
                        Future.successful(
                          Redirect(
                            controllers.employments.routes.UpdateEmploymentController
                              .updateEmploymentDetails(empId)
                          )
                        )
                      case _ =>
                        hasIrregularPayment(employment, user.nino.nino)
                    }
                  )
              )
            )
        )
    }

  private def hasIrregularPayment(employment: Employment, nino: String)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val today = LocalDate.now
    val latestPaymentDate: Option[LocalDate] = for {
      latestAnnualAccount <- employment.latestAnnualAccount
      latestPayment       <- latestAnnualAccount.latestPayment
    } yield latestPayment.date

    latestPaymentDate.map { latestPaymentDate =>
      for {
        _ <- journeyCacheNewRepository.set(
               request.userAnswers.setOrException(EndEmploymentLatestPaymentPage, latestPaymentDate)
             )
      } yield
        if (latestPaymentDate.isAfter(today.minusWeeks(6).minusDays(1))) {
          auditService
            .createAndSendAuditEvent(
              AuditConstants.EndEmploymentWithinSixWeeksError,
              Map("nino" -> nino)
            )
          Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentError())
        } else {
          if (employment.latestAnnualAccount.exists(_.isIrregularPayment)) {
            auditService
              .createAndSendAuditEvent(AuditConstants.EndEmploymentIrregularPayment, Map("nino" -> nino))
            Redirect(controllers.employments.routes.EndEmploymentController.irregularPaymentError())
          } else {
            Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage())
          }
        }
    }
  }.getOrElse(
    Future.successful(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage()))
  )

  def endEmploymentError: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      (request.userAnswers.get(EndEmploymentIdPage), request.userAnswers.get(EndEmploymentLatestPaymentPage)) match {
        case (Some(empId), Some(latestPayment)) =>
          employmentService
            .employment(user.nino, empId)
            .map(
              _.fold(
                error5xxInBadRequest()
              )(employment =>
                Ok(
                  endEmploymentWithinSixWeeksError(
                    WithinSixWeeksViewModel(
                      latestPayment.plusWeeks(6).plusDays(1),
                      employment.name,
                      latestPayment,
                      empId
                    )
                  )
                )
              )
            )
        case _ =>
          Future.successful(error5xxInBadRequest())
      }
    }

  def irregularPaymentError: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers
        .get(EndEmploymentIdPage)
        .fold(
          Future.successful(error5xxInBadRequest())
        ) { empId =>
          employmentService
            .employment(request.taiUser.nino, empId)
            .map(
              _.fold(
                error5xxInBadRequest()
              )(employment =>
                Ok(
                  endEmploymentIrregularPaymentError(
                    IrregularPayForm.createForm,
                    EmploymentViewModel(employment.name, empId)
                  )
                )
              )
            )
        }
    }

  def handleIrregularPaymentError: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers.get(EndEmploymentIdPage) match {
        case Some(empId) =>
          IrregularPayForm.createForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                employmentService
                  .employment(request.taiUser.nino, empId)
                  .map(
                    _.fold(
                      error5xxInBadRequest()
                    )(employment =>
                      BadRequest(
                        endEmploymentIrregularPaymentError(
                          formWithErrors,
                          EmploymentViewModel(employment.name, empId)
                        )
                      )
                    )
                  ),
              {
                case Some(IrregularPayConstants.ContactEmployer) =>
                  for {
                    _ <- journeyCacheNewRepository.set(
                           request.userAnswers
                             .setOrException(EndEmploymentIrregularPaymentPage, IrregularPayConstants.ContactEmployer)
                         )
                  } yield Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
                case Some(value) =>
                  for {
                    _ <- journeyCacheNewRepository
                           .set(request.userAnswers.setOrException(EndEmploymentIrregularPaymentPage, value))
                  } yield Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage())
              }
            )
        case None =>
          Future.successful(error5xxInBadRequest())
      }
    }

  def endEmploymentPage: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      (request.userAnswers.get(EndEmploymentIdPage), request.userAnswers.get(EndEmploymentEndDatePage)) match {
        case (Some(empId), endDate) =>
          employmentService
            .employment(authUser.nino, empId)
            .map(
              _.fold(
                error5xxInBadRequest()
              ) { employment =>
                val formData = endDate
                  .map(date => EmploymentEndDateForm(employment.name).form.fill(date))
                  .getOrElse(EmploymentEndDateForm(employment.name).form)
                Ok(
                  endEmploymentView(
                    formData,
                    EmploymentViewModel(employment.name, empId)
                  )
                )
              }
            )
        case _ =>
          Future.successful(error5xxInBadRequest())
      }
    }

  def handleEndEmploymentPage(employmentId: Int): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino

      employmentService
        .employment(nino, employmentId)
        .flatMap(
          _.fold(
            Future.successful(error5xxInBadRequest())
          )(employment =>
            EmploymentEndDateForm(employment.name).form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(endEmploymentView(formWithErrors, EmploymentViewModel(employment.name, employmentId)))
                  ),
                date =>
                  for {
                    _ <-
                      journeyCacheNewRepository.set(request.userAnswers.setOrException(EndEmploymentEndDatePage, date))
                  } yield Redirect(controllers.employments.routes.EndEmploymentController.addTelephoneNumber())
              )
          )
        )

    }

  def addTelephoneNumber(): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      (
        request.userAnswers.get(EndEmploymentIdPage),
        request.userAnswers.get(EndEmploymentTelephoneQuestionPage),
        request.userAnswers.get(EndEmploymentTelephoneNumberPage)
      ) match {
        case (Some(empId), telephoneQuestion, telephoneNumber) =>
          Future.successful(
            Ok(
              canWeContactByPhone(
                Some(authUser),
                telephoneNumberViewModel(empId),
                YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneQuestion, telephoneNumber))
              )
            )
          )
        case _ =>
          Future.successful(error5xxInBadRequest())
      }
    }

  def submitTelephoneNumber(): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers
        .get(EndEmploymentIdPage)
        .fold(
          Future.successful(error5xxInBadRequest())
        )(empId =>
          YesNoTextEntryForm
            .form(
              Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
              Messages("tai.canWeContactByPhone.telephone.empty"),
              Some(TelephoneNumberConstraint.telephoneNumberSizeConstraint)
            )
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(canWeContactByPhone(Some(authUser), telephoneNumberViewModel(empId), formWithErrors))
                ),
              form =>
                for {
                  _ <- journeyCacheNewRepository.set(submitTelephoneCacheHandler(form))
                } yield Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentCheckYourAnswers())
            )
        )
    }

  private def submitTelephoneCacheHandler(
    form: YesNoTextEntryForm
  )(implicit request: DataRequest[_]): UserAnswers =
    form.yesNoChoice match {
      case Some(yes) if yes == FormValuesConstants.YesValue =>
        val questionCached = Messages(s"tai.label.${yes.toLowerCase}")
        request.userAnswers
          .setOrException(EndEmploymentTelephoneQuestionPage, questionCached)
          .setOrException(EndEmploymentTelephoneNumberPage, form.yesNoTextEntry.getOrElse(""))
      case _ =>
        val questionCached = Messages(
          s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}"
        )
        request.userAnswers
          .setOrException(EndEmploymentTelephoneQuestionPage, questionCached)
          .setOrException(EndEmploymentTelephoneNumberPage, "")
    }

  def endEmploymentCheckYourAnswers: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      (
        request.userAnswers.get(EndEmploymentIdPage),
        request.userAnswers.get(EndEmploymentEndDatePage),
        request.userAnswers.get(EndEmploymentTelephoneQuestionPage),
        request.userAnswers.get(EndEmploymentTelephoneNumberPage)
      ) match {
        case (Some(empId), Some(endDate), Some(telephoneQuestion), telephoneNumber) =>
          val model = IncomeCheckYourAnswersViewModel(
            employmentId = empId,
            preHeading = Messages("tai.endEmployment.preHeadingText"),
            incomeSourceEnd = endDate.toString,
            contactableByPhone = telephoneQuestion,
            phoneNumber = telephoneNumber,
            backLinkUrl = controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url,
            submissionUrl = controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url,
            cancelUrl = controllers.employments.routes.EndEmploymentController.cancel(empId).url
          )
          Future.successful(Ok(addIncomeCheckYourAnswers(model)))
        case _ =>
          Future.successful(error5xxInBadRequest())
      }
    }

  def confirmAndSendEndEmployment(): Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      val result = for {
        empId             <- request.userAnswers.get(EndEmploymentIdPage)
        endDate           <- request.userAnswers.get(EndEmploymentEndDatePage)
        telephoneQuestion <- request.userAnswers.get(EndEmploymentTelephoneQuestionPage)
        telephoneNumber   <- request.userAnswers.get(EndEmploymentTelephoneNumberPage)
        model = EndEmployment(endDate, telephoneQuestion, Some(telephoneNumber))
      } yield for {
        _ <- successfulJourneyCacheService.cache(
               Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-$empId" -> "true")
             )
        _ <- journeyCacheNewRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
        _ <- employmentService.endEmployment(authUser.nino, empId, model)
      } yield Redirect(controllers.employments.routes.EndEmploymentController.showConfirmationPage())
      result.getOrElse(
        Future.successful(error5xxInBadRequest())
      )
    }

  def duplicateSubmissionWarning: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers
        .get(EndEmploymentIdPage)
        .fold(
          Future.successful(error5xxInBadRequest())
        )(empId =>
          employmentService
            .employment(authUser.nino, empId)
            .map(
              _.fold(
                error5xxInBadRequest()
              )(employment =>
                Ok(
                  duplicateSubmissionWarning(
                    DuplicateSubmissionWarningForm.createForm,
                    employment.name,
                    empId
                  )
                )
              )
            )
        )
    }

  def submitDuplicateSubmissionWarning: Action[AnyContent] =
    authenticate.authWithDataRetrieval.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers
        .get(EndEmploymentIdPage)
        .fold(
          Future.successful(error5xxInBadRequest())
        )(empId =>
          employmentService
            .employment(authUser.nino, empId)
            .map(
              _.fold(
                error5xxInBadRequest()
              )(employment =>
                DuplicateSubmissionWarningForm.createForm
                  .bindFromRequest()
                  .fold(
                    formWithErrors => BadRequest(duplicateSubmissionWarning(formWithErrors, employment.name, empId)),
                    success =>
                      success.yesNoChoice match {
                        case Some(FormValuesConstants.YesValue) =>
                          Redirect(
                            controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision()
                          )
                        case Some(FormValuesConstants.NoValue) =>
                          Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
                      }
                  )
              )
            )
        )
    }

  def showConfirmationPage: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    Future.successful(Ok(confirmation()))
  }
}
