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

import com.google.inject.name.Named
import controllers._
import controllers.actions.{ActionJourney, ValidatePerson}
import controllers.auth.{AuthAction, AuthedUser, DataRequest}
import pages._
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{DuplicateSubmissionWarningForm, EmploymentEndDateForm, IrregularPayForm, UpdateRemoveEmploymentForm}
import uk.gov.hmrc.tai.model.domain.{Employment, EndEmployment}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, WithinSixWeeksViewModel}
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

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
  @Named("End Employment") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  actionJourney: ActionJourney,
  authAction: AuthAction, // TODO - Use journey
  validatePerson: ValidatePerson
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect with Logging {

  def cancel(empId: Int): Action[AnyContent] = (authAction andThen validatePerson).async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  private def telephoneNumberViewModel(employmentId: Int)(implicit messages: Messages) =
    CanWeContactByPhoneViewModel(
      messages("tai.endEmployment.preHeadingText"),
      messages("tai.canWeContactByPhone.title"),
      controllers.employments.routes.EndEmploymentController.endEmploymentPage().url,
      controllers.employments.routes.EndEmploymentController.submitTelephoneNumber().url,
      controllers.employments.routes.EndEmploymentController.cancel(employmentId).url
    )

  def employmentUpdateRemoveDecision: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
          employmentService.employment(request.taiUser.nino, empId).map {
            case Some(employment) =>
              Ok(
                updateRemoveEmploymentDecision(
                  UpdateRemoveEmploymentForm
                    .form(employment.name)
                    .fill(request.userAnswers.get(EmploymentUpdateRemovePage)),
                  employment.name,
                  empId
                )
              )
            case None =>
              Redirect(taxAccountSummaryRedirect)
          }
        case None =>
          Future.successful(Redirect(taxAccountSummaryRedirect))
      }
    }

  def onPageLoad(empId: Int): Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(_) =>
          Future.successful(
            Redirect(controllers.employments.routes.EndEmploymentController.duplicateSubmissionWarning())
          )
        case None =>
          request.userAnswers.set(EmploymentIdKeyPage, empId) match {
            case Failure(exception) => throw exception
            case Success(_) =>
              Future.successful(
                Redirect(controllers.employments.routes.EndEmploymentController.employmentUpdateRemoveDecision())
              )
          }
      }
    }

  // TODO - Rename
  def handleEmploymentUpdateRemove: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
          employmentService.employment(user.nino, empId).flatMap {
            case Some(employment) =>
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
                            .updateEmploymentDetails(empId) // TODO - Does this need to be a route
                        )
                      )
                    case _ =>
                      hasIrregularPayment(employment, user.nino.nino)
                  }
                )
            case None =>
              Future.successful(
                InternalServerError(errorPagesHandler.error4xxPageWithLink("No employment found"))
              ) // TODO - Employment request failed case
          }
        case None =>
          Future.successful(
            InternalServerError(errorPagesHandler.error4xxPageWithLink("No employment id"))
          ) // TODO - No EmpId case, correct response?
      }
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
      request.userAnswers
        .set(EmploymentLatestPaymentKeyPage, latestPaymentDate) // TODO - Needs test
        .map { _ =>
          if (latestPaymentDate.isAfter(today.minusWeeks(6).minusDays(1))) {
            auditService // TODO - Verify
              .createAndSendAuditEvent(
                AuditConstants.EndEmploymentWithinSixWeeksError,
                Map("nino" -> nino)
              )
            Future.successful(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentError()))
          } else {
            if (employment.latestAnnualAccount.exists(_.isIrregularPayment)) {
              auditService
                .createAndSendAuditEvent(AuditConstants.EndEmploymentIrregularPayment, Map("nino" -> nino))
              Future.successful(
                Redirect(controllers.employments.routes.EndEmploymentController.irregularPaymentError())
              ) // TODO - Does this need to be a route
            } else {
              Future.successful(
                Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage())
              ) // TODO - Does this need to be a route
            }
          }
        }
        .getOrElse {
          Future.successful(
            InternalServerError(errorPagesHandler.error4xxPageWithLink("Caching failed"))
          ) // TODO - Failed, correct response?
        }
    }
  }.getOrElse(
    Future.successful(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage()))
  ) // TODO - No latest payment date, what to do?

  def endEmploymentError: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      (request.userAnswers.get(EmploymentIdKeyPage), request.userAnswers.get(EmploymentLatestPaymentKeyPage)) match {
        case (Some(empId), Some(latestPayment)) =>
          employmentService.employment(user.nino, empId).map {
            case Some(employment) =>
              Ok(
                endEmploymentWithinSixWeeksError( // TODO - OK and Error?
                  WithinSixWeeksViewModel(latestPayment.plusWeeks(6).plusDays(1), employment.name, latestPayment, empId)
                )
              )
            case None =>
              InternalServerError(errorPagesHandler.error5xx("Service failed")) // TODO - Fix incorrect arg
          }
        case _ =>
          Future.successful(InternalServerError(errorPagesHandler.error5xx("Cache failed"))) // TODO - Fix incorrect arg
      }
    }

  def irregularPaymentError: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
          employmentService.employment(request.taiUser.nino, empId).map {
            case Some(employment) =>
              Ok(
                endEmploymentIrregularPaymentError(
                  IrregularPayForm.createForm, // TODO - Option or not?
                  EmploymentViewModel(employment.name, empId)
                )
              )
            case None => InternalServerError(errorPagesHandler.error5xx("Could not retrieve employment data"))
          }
        case None =>
          Future.successful(InternalServerError(errorPagesHandler.error5xx("Could not retrieve employment id")))
      }
    }

  def handleIrregularPaymentError: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
          IrregularPayForm.createForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                employmentService.employment(request.taiUser.nino, empId).map {
                  case Some(employment) =>
                    BadRequest(
                      endEmploymentIrregularPaymentError(
                        formWithErrors,
                        EmploymentViewModel(employment.name, empId)
                      )
                    )
                  case None => InternalServerError(errorPagesHandler.error5xx("Could not retrieve employment data"))
                },
              {
                case Some(data) =>
                  request.userAnswers.set(EmploymentIrregularPaymentKeyPage, data)
                  Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
                case _ =>
                  Future
                    .successful(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage()))
              }
            )
      }
    }

  def endEmploymentPage: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      (request.userAnswers.get(EmploymentIdKeyPage), request.userAnswers.get(EmploymentEndDateKeyPage)) match {
        case (Some(empId), endDate) =>
          employmentService.employment(authUser.nino, empId).map {
            case Some(employment) => // TODO - Make form take optional
              val formData = endDate
                .map(date => EmploymentEndDateForm(employment.name).form.fill(date))
                .getOrElse(EmploymentEndDateForm(employment.name).form)
              Ok(
                endEmploymentView(
                  formData,
                  EmploymentViewModel(employment.name, empId)
                )
              )
            case None => InternalServerError(errorPagesHandler.error5xx("Could not retrieve employment data"))
          }
        case _ =>
          Future.successful(
            InternalServerError(errorPagesHandler.error5xx("Could not retrieve employment id"))
          ) // TODO - Originally returned to beginning of journey. Is this better?
      }
    }

  def handleEndEmploymentPage(): Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val user: AuthedUser = request.taiUser
      val nino = user.nino
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
          employmentService.employment(nino, empId).flatMap {
            case Some(employment) =>
              EmploymentEndDateForm(employment.name).form
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(endEmploymentView(formWithErrors, EmploymentViewModel(employment.name, empId)))
                    ),
                  date => {
                    request.userAnswers.set(EmploymentEndDateKeyPage, date)
                    Future
                      .successful(Redirect(controllers.employments.routes.EndEmploymentController.addTelephoneNumber()))
                  }
                )
            case _ =>
              Future.successful(InternalServerError(errorPagesHandler.error4xxPageWithLink("No employment found"))) // TODO - 5xx
          }
        case _ =>
          Future.successful(InternalServerError(errorPagesHandler.error4xxPageWithLink("No cache data found")))
      }
    }

  def addTelephoneNumber(): Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      (
        request.userAnswers.get(EmploymentIdKeyPage),
        request.userAnswers.get(EmploymentTelephoneQuestionKeyPage), // TODO - Why is the question cached?
        request.userAnswers.get(EmploymentTelephoneNumberKeyPage)
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
          Future.successful(Redirect(taxAccountSummaryRedirect)) // TODO - This doesn't seem right as a failure case
      }
    }

  def submitTelephoneNumber(): Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
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
              form => {
                val cache = form.yesNoChoice match {
                  case Some(yes) if yes == FormValuesConstants.YesValue =>
                    val questionCached = Messages(s"tai.label.${yes.toLowerCase}")
                    request.userAnswers.set(EmploymentTelephoneQuestionKeyPage, questionCached)
                    request.userAnswers.set(EmploymentTelephoneNumberKeyPage, form.yesNoTextEntry.getOrElse(""))
                  case _ =>
                    val questionCached =
                      Messages(s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}")
                    request.userAnswers.set(EmploymentTelephoneQuestionKeyPage, questionCached)
                    request.userAnswers.set(EmploymentTelephoneNumberKeyPage, form.yesNoTextEntry.getOrElse(""))
                }
                cache
                  .map(_ =>
                    Future.successful(
                      Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentCheckYourAnswers())
                    )
                  )
                  .getOrElse(
                    Future.successful(Redirect(taxAccountSummaryRedirect))
                  ) // TODO - Another strange choice of error handling
              }
            )
        case _ =>
          Future.successful(Redirect(taxAccountSummaryRedirect)) // TODO - Another strange choice of error handling
      }
    }

  def endEmploymentCheckYourAnswers: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      (
        request.userAnswers.get(EmploymentIdKeyPage),
        request.userAnswers.get(EmploymentEndDateKeyPage),
        request.userAnswers.get(EmploymentTelephoneQuestionKeyPage), // TODO - Still need looking into
        request.userAnswers.get(EmploymentTelephoneNumberKeyPage)
      ) match {
        case (Some(empId), Some(endDate), Some(telephoneQuestion), telephoneNumber) =>
          val model = IncomeCheckYourAnswersViewModel(
            employmentId = empId,
            preHeading = Messages("tai.endEmployment.preHeadingText"),
            incomeSourceEnd = endDate.format(DateTimeFormatter.ofPattern("dd MM yyyy")), // TODO - Check pattern
            contactableByPhone = telephoneQuestion,
            phoneNumber = telephoneNumber,
            backLinkUrl = controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url,
            submissionUrl = controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url,
            cancelUrl = controllers.employments.routes.EndEmploymentController.cancel(empId).url
          )
          Future.successful(Ok(addIncomeCheckYourAnswers(model)))
        case _ =>
          Future.successful(Redirect(taxAccountSummaryRedirect)) // TODO - Another strange choice of error handling
      }
    }

  def confirmAndSendEndEmployment(): Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      val result = for {
        empId             <- request.userAnswers.get(EmploymentIdKeyPage)
        endDate           <- request.userAnswers.get(EmploymentEndDateKeyPage)
        telephoneQuestion <- request.userAnswers.get(EmploymentTelephoneQuestionKeyPage)
        telephoneNumber   <- request.userAnswers.get(EmploymentTelephoneNumberKeyPage)
        model = EndEmployment(endDate, telephoneQuestion, Some(telephoneNumber))
//        _ <- successfulJourneyCacheService.cache(
//          Map(s"${TrackSuccessfulJourneyConstants.UpdateEndEmploymentKey}-${mandatoryCacheSeq.head}" -> "true")
//        )   // TODO - What was this for?
//        _ <- request.userAnswers.  FLUSH   // TODO - Do we actually need to clear journey cache if it times out anyway?
      } yield employmentService.endEmployment(authUser.nino, empId, model).map { _ =>
        Redirect(controllers.employments.routes.EndEmploymentController.showConfirmationPage())
      }
      result.getOrElse(
        Future.successful(InternalServerError(errorPagesHandler.error5xx("End employment failed")))
      ) // TODO - Might need expanding upon
    }

  def duplicateSubmissionWarning: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
          employmentService.employment(authUser.nino, empId).map {
            case Some(employment) =>
              Ok(
                duplicateSubmissionWarning(
                  DuplicateSubmissionWarningForm.createForm,
                  employment.name,
                  empId
                )
              )
            case None => InternalServerError(errorPagesHandler.error5xx("Get employment failed"))
          }
        case None => Future.successful(InternalServerError(errorPagesHandler.error5xx("No employment id found")))
      }
    }

  def submitDuplicateSubmissionWarning: Action[AnyContent] =
    actionJourney.setJourneyCache.async { implicit request =>
      implicit val authUser: AuthedUser = request.taiUser
      request.userAnswers.get(EmploymentIdKeyPage) match {
        case Some(empId) =>
          employmentService.employment(authUser.nino, empId).map {
            case Some(employment) =>
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
            case _ =>
              InternalServerError(
                errorPagesHandler.error5xx("Get employment failed")
              ) // TODO - Might need expanding upon
          }
        case None => Future.successful(InternalServerError(errorPagesHandler.error5xx("No employment id found")))
      }
    }

  def showConfirmationPage: Action[AnyContent] = (authAction andThen validatePerson).async { implicit request =>
    Future.successful(Ok(confirmation()))
  }
}
