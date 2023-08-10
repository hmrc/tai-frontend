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
import controllers.actions.ActionJourney
import controllers.auth.{AuthedUser, DataRequest}
import pages._
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.{DuplicateSubmissionWarningForm, EmploymentEndDateForm, IrregularPayForm, UpdateRemoveEmploymentForm}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.{Employment, EndEmployment}
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, IrregularPayConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.{EmploymentViewModel, WithinSixWeeksViewModel}
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments._
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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
  actionJourney: ActionJourney
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect with Logging {

  private def flushJourney(userAnswers: UserAnswers): Try[Unit] = // TODO - Test and get second opinion
    for {
      _ <- userAnswers.remove(EmploymentIdKeyPage)
      _ <- userAnswers.remove(EmploymentUpdateRemovePage)
      _ <- userAnswers.remove(EmploymentNameKeyPage)
      _ <- userAnswers.remove(EmploymentIrregularPaymentKeyPage)
      _ <- userAnswers.remove(EmploymentEndDateKeyPage)
      _ <- userAnswers.remove(EmploymentTelephoneNumberKeyPage)
      _ <- userAnswers.remove(EmploymentTelephoneQuestionKeyPage)
      _ <- userAnswers.remove(EmploymentLatestPaymentKeyPage)
    } yield ()

  def cancel(empId: Int): Action[AnyContent] = actionJourney.setJourneyCache.async {
    implicit request => // TODO - Needs live test
      flushJourney(request.userAnswers) match {
        case Failure(e) =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink(s"Cache flush failed with exception: $e"))
          )
        case Success(_) =>
          Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
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
              BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
          }
        case None =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id"))
          )
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
            case Failure(e) =>
              Future.successful(
                BadRequest(
                  errorPagesHandler.error4xxPageWithLink(
                    s"Caching $empId to ${EmploymentIdKeyPage.toString} failed with exception: $e"
                  )
                )
              )
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
                BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
              )
          }
        case None =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id"))
          )
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
              )
            } else {
              Future.successful(
                Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage())
              ) // TODO - Does this need to be a route
            }
          }
        }
        .getOrElse {
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id"))
          )
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
              BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
          }
        case (empId, latestPayment) =>
          Future.successful(
            BadRequest(
              errorPagesHandler.error5xx(
                s"Cache failed, values retrieved are: $empId for employer id and $latestPayment for latest payment"
              )
            )
          )
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
            case None =>
              BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
          }
        case None =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id"))
          )
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
                  case None => BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
                },
              {
                case Some(IrregularPayConstants.ContactEmployer) =>
                  request.userAnswers.set(EmploymentIrregularPaymentKeyPage, IrregularPayConstants.ContactEmployer)
                  Future.successful(Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad()))
                case Some(value) =>
                  request.userAnswers.set(EmploymentIrregularPaymentKeyPage, value)
                  Future
                    .successful(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage()))
                case _ => // TODO - Not possible but needed if to prevent non-exhaustive match error?
                  Future
                    .successful(Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentPage()))
              }
            )
        case None =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id"))
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
            case None => BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
          }
        case (empId, endDate) =>
          Future.successful(
            BadRequest(
              errorPagesHandler.error5xx(
                s"Cache failed, values retrieved are: $empId for employer id and $endDate for end date"
              )
            )
          )
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
                  date =>
                    request.userAnswers.set(EmploymentEndDateKeyPage, date) match {
                      case Failure(e) =>
                        Future.successful(
                          BadRequest(
                            errorPagesHandler.error4xxPageWithLink(
                              s"Caching $date to ${EmploymentEndDateKeyPage.toString} failed with exception: $e"
                            )
                          )
                        )
                      case Success(_) =>
                        Future.successful(
                          Redirect(controllers.employments.routes.EndEmploymentController.addTelephoneNumber())
                        )
                    }
                )
            case _ =>
              Future.successful(
                BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
              )
          }
        case _ =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id"))
          )
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
        case (empId, telephoneQuestion, telephoneNumber) =>
          Future.successful(
            BadRequest(
              errorPagesHandler.error5xx(
                s"Cache failed, values retrieved are: $empId for employer id, " +
                  s"$telephoneQuestion for telephone questions, and $telephoneNumber for telephone number"
              )
            )
          )
      }
    }

  def submitTelephoneNumber()
    : Action[AnyContent] = // TODO - Is this actually submitting a phone number? Cannot find the logic for this
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
                    request.userAnswers.set(EmploymentTelephoneNumberKeyPage, "")
                }
                cache match {
                  case Failure(e) =>
                    Future.successful(
                      BadRequest(
                        errorPagesHandler.error4xxPageWithLink(
                          s"Caching ${form.yesNoChoice} to ${EmploymentTelephoneNumberKeyPage.toString}" +
                            s" and ${EmploymentTelephoneQuestionKeyPage.toString} value failed with exception: $e"
                        )
                      )
                    )
                  case Success(_) =>
                    Future.successful(
                      Redirect(controllers.employments.routes.EndEmploymentController.endEmploymentCheckYourAnswers())
                    )
                }
              }
            )
        case _ =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id"))
          )
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
            incomeSourceEnd = endDate.toString, // TODO - Check pattern
            contactableByPhone = telephoneQuestion,
            phoneNumber = telephoneNumber,
            backLinkUrl = controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url,
            submissionUrl = controllers.employments.routes.EndEmploymentController.confirmAndSendEndEmployment().url,
            cancelUrl = controllers.employments.routes.EndEmploymentController.cancel(empId).url
          )
          Future.successful(Ok(addIncomeCheckYourAnswers(model)))
        case (empId, endDate, telephoneQuestion, telephoneNumber) =>
          Future.successful(
            BadRequest(
              errorPagesHandler.error5xx(
                s"Cache failed, values retrieved are: $empId for employer id, $endDate for end date, " +
                  s"$telephoneQuestion for telephone questions, and $telephoneNumber for telephone number"
              )
            )
          )
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
      } yield flushJourney(request.userAnswers) match {
        case Failure(e) =>
          Future.successful(
            BadRequest(errorPagesHandler.error4xxPageWithLink(s"Cache flush failed with exception: $e"))
          )
        case Success(_) =>
          employmentService.endEmployment(authUser.nino, empId, model).map { _ =>
            Redirect(controllers.employments.routes.EndEmploymentController.showConfirmationPage())
          }
      }
      result.getOrElse(
        Future.successful(
          BadRequest(errorPagesHandler.error4xxPageWithLink("End employment failed"))
        )
      )
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
            case None => BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
          }
        case None => Future.successful(BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id")))
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
            case _ => BadRequest(errorPagesHandler.error4xxPageWithLink("No employment found"))
          }
        case None => Future.successful(BadRequest(errorPagesHandler.error4xxPageWithLink("No employment id")))
      }
    }

  def showConfirmationPage: Action[AnyContent] = actionJourney.authAndValidate.async { implicit request =>
    Future.successful(Ok(confirmation()))
  }
}
