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
import pages.AddPayeRefPage
import pages.addEmployment.*
import play.api.i18n.Messages
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.*
import repository.JourneyCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint
import uk.gov.hmrc.tai.forms.employments.*
import uk.gov.hmrc.tai.forms.{PayeRefForm, YesNoTextEntryForm}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.AddEmployment
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.employments.NewCachePayrollNumberViewModel
import uk.gov.hmrc.tai.viewModels.income.IncomeCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.employments.*
import views.html.incomes.AddIncomeCheckYourAnswersView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddEmploymentController @Inject() (
  auditService: AuditService,
  employmentService: EmploymentService,
  authenticate: AuthJourney,
  journeyCacheRepository: JourneyCacheRepository,
  val auditConnector: AuditConnector,
  mcc: MessagesControllerComponents,
  addEmploymentStartDateForm: AddEmploymentStartDateFormView,
  addEmploymentNameForm: AddEmploymentNameFormView,
  addEmploymentFirstPayForm: AddEmploymentFirstPayFormView,
  addEmploymentErrorPage: AddEmploymentErrorPageView,
  addEmploymentPayrollNumberForm: AddEmploymentPayrollNumberFormView,
  canWeContactByPhone: CanWeContactByPhoneView,
  confirmationView: ConfirmationView,
  addIncomeCheckYourAnswers: AddIncomeCheckYourAnswersView,
  payeRefFormView: PayeRefFormView,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with EmptyCacheRedirect {

  def cancel(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino) map { _ =>
      Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def telephoneNumberViewModel(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("add.missing.employment"),
      messages("tai.canWeContactByPhone.title"),
      controllers.employments.routes.AddEmploymentController.addPayeReference().url,
      controllers.employments.routes.AddEmploymentController.submitTelephoneNumber().url,
      controllers.employments.routes.AddEmploymentController.cancel().url
    )

  private def error5xxInBadRequest()(implicit request: Request[_]): Result =
    BadRequest(errorPagesHandler.error5xx(Messages("global.error.InternalServerError500.message")))

  def addEmploymentName(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    Ok(
      addEmploymentNameForm(EmploymentNameForm.form.fill(request.userAnswers.get(AddEmploymentNamePage).getOrElse("")))
    )
  }

  def submitEmploymentName(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    EmploymentNameForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          implicit val user: AuthedUser = request.taiUser

          Future.successful(BadRequest(addEmploymentNameForm(formWithErrors)))
        },
        employmentName =>
          for {
            _ <-
              journeyCacheRepository.set(request.userAnswers.setOrException(AddEmploymentNamePage, employmentName))
          } yield Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentStartDate())
      )
  }

  def addEmploymentStartDate(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    (
      request.userAnswers.get(AddEmploymentNamePage),
      request.userAnswers.get(AddEmploymentStartDatePage)
    ) match {
      case (Some(name), Some(date)) =>
        Ok(addEmploymentStartDateForm(EmploymentAddDateForm(name).form.fill(date), name))
      case (Some(name), None)       =>
        Ok(addEmploymentStartDateForm(EmploymentAddDateForm(name).form, name))
      case _                        => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
    }
  }

  def submitEmploymentStartDate(): Action[AnyContent] = authenticate.authWithDataRetrieval async { implicit request =>
    request.userAnswers
      .get(AddEmploymentNamePage)
      .fold(
        Future.successful(error5xxInBadRequest())
      )(cachedValue =>
        EmploymentAddDateForm(cachedValue).form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              implicit val user: AuthedUser = request.taiUser

              Future.successful(BadRequest(addEmploymentStartDateForm(formWithErrors, cachedValue)))
            },
            date => {
              val startDateBoundary = LocalDate.now.minusWeeks(6)
              val startDateAnswers  = request.userAnswers.setOrException(AddEmploymentStartDatePage, date)
              val wholeAnswers      = if (date.isAfter(startDateBoundary)) {
                startDateAnswers.setOrException(AddEmploymentStartDateWithinSixWeeksPage, FormValuesConstants.YesValue)
              } else {
                startDateAnswers.setOrException(AddEmploymentStartDateWithinSixWeeksPage, FormValuesConstants.NoValue)
              }
              for {
                _ <- journeyCacheRepository.set(wholeAnswers)
              } yield
                if (date.isAfter(startDateBoundary)) {
                  Redirect(controllers.employments.routes.AddEmploymentController.receivedFirstPay())
                } else {
                  Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
                }
            }
          )
      )
  }

  def receivedFirstPay(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val authedUser: AuthedUser = request.taiUser
    (request.userAnswers.get(AddEmploymentNamePage), request.userAnswers.get(AddEmploymentReceivedFirstPayPage)) match {
      case (Some(name), bool) =>
        Ok(addEmploymentFirstPayForm(AddEmploymentFirstPayForm.form.fill(bool), name))
      case _                  =>
        error5xxInBadRequest()
    }
  }

  def submitFirstPay(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    request.userAnswers
      .get(AddEmploymentNamePage)
      .fold(
        Future.successful(error5xxInBadRequest())
      )(employmentName =>
        AddEmploymentFirstPayForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              implicit val user: AuthedUser = request.taiUser
              Future.successful(BadRequest(addEmploymentFirstPayForm(formWithErrors, employmentName)))
            },
            firstPayYesNo =>
              for {
                _ <-
                  journeyCacheRepository
                    .set(
                      request.userAnswers.setOrException(AddEmploymentReceivedFirstPayPage, firstPayYesNo.getOrElse(""))
                    )
              } yield firstPayYesNo match {
                case Some(FormValuesConstants.YesValue) =>
                  Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber())
                case _                                  => Redirect(controllers.employments.routes.AddEmploymentController.sixWeeksError())
              }
          )
      )
  }

  def sixWeeksError(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    request.userAnswers
      .get(AddEmploymentNamePage)
      .fold(error5xxInBadRequest()) { employmentName =>
        auditService
          .createAndSendAuditEvent(AuditConstants.AddEmploymentCantAddEmployer, Map("nino" -> user.nino.toString()))
        Ok(addEmploymentErrorPage(employmentName))
      }
  }

  def addEmploymentPayrollNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    val viewModel                 = NewCachePayrollNumberViewModel(request.userAnswers)
    implicit val user: AuthedUser = request.taiUser

    (
      request.userAnswers.get(AddEmploymentPayrollQuestionPage),
      request.userAnswers.get(AddEmploymentPayrollNumberPage)
    ) match {
      case (Some(payrollChoice), a) if payrollChoice.equals(FormValuesConstants.YesValue) =>
        Ok(
          addEmploymentPayrollNumberForm(
            AddEmploymentPayrollNumberForm.form.fill(AddEmploymentPayrollNumberForm(Some(payrollChoice), a)),
            viewModel
          )
        )
      case (optionalChoice, _)                                                            =>
        Ok(
          addEmploymentPayrollNumberForm(
            AddEmploymentPayrollNumberForm.form.fill(AddEmploymentPayrollNumberForm(optionalChoice, None)),
            viewModel
          )
        )
    }
  }

  def submitEmploymentPayrollNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      val viewModel                 = NewCachePayrollNumberViewModel(request.userAnswers)
      implicit val user: AuthedUser = request.taiUser

      AddEmploymentPayrollNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(addEmploymentPayrollNumberForm(formWithErrors, viewModel))),
          form =>
            for {
              _ <- journeyCacheRepository
                     .set(
                       request.userAnswers
                         .setOrException(AddEmploymentPayrollQuestionPage, form.payrollNumberChoice.getOrElse(""))
                         .setOrException(
                           AddEmploymentPayrollNumberPage,
                           form.payrollNumberEntry
                             .getOrElse(Messages("tai.addEmployment.employmentPayrollNumber.notKnown"))
                         )
                     )
            } yield Redirect(controllers.employments.routes.AddEmploymentController.addPayeReference())
        )
  }

  def addPayeReference(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    request.userAnswers.get(AddEmploymentNamePage) match {
      case Some(companyName) =>
        val existing: String = request.userAnswers.get(AddPayeRefPage).getOrElse("")

        val form = PayeRefForm.form.fill(existing)
        Ok(payeRefFormView(form, companyName, "employment"))
      case None              =>
        Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def submitPayeReference(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    request.userAnswers.get(AddEmploymentNamePage) match {
      case Some(companyName) =>
        PayeRefForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(payeRefFormView(formWithErrors, companyName, "employment"))),
            value => {
              val updatedAnswers = request.userAnswers.setOrException(AddPayeRefPage, value)
              for {
                _ <- journeyCacheRepository.set(updatedAnswers)
              } yield Redirect(controllers.employments.routes.AddEmploymentController.addTelephoneNumber())
            }
          )
      case None              => Future.successful(error5xxInBadRequest())
    }
  }

  def addTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (
      request.userAnswers.get(AddEmploymentTelephoneQuestionPage),
      request.userAnswers.get(AddEmploymentTelephoneNumberPage)
    ) match {
      case (Some(FormValuesConstants.YesValue), telNoToDisplay) =>
        Ok(
          canWeContactByPhone(
            Some(user),
            telephoneNumberViewModel,
            YesNoTextEntryForm.form().fill(YesNoTextEntryForm(Some(FormValuesConstants.YesValue), telNoToDisplay))
          )
        )
      case (response, _)                                        =>
        Ok(
          canWeContactByPhone(
            Some(user),
            telephoneNumberViewModel,
            YesNoTextEntryForm.form().fill(YesNoTextEntryForm(response, None))
          )
        )
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(TelephoneNumberConstraint.telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors => {
          implicit val user: AuthedUser = request.taiUser

          Future.successful(BadRequest(canWeContactByPhone(Some(user), telephoneNumberViewModel, formWithErrors)))
        },
        form => {
          val userAnswers = form.yesNoChoice match {
            case Some(yn) if yn == FormValuesConstants.YesValue =>
              request.userAnswers
                .setOrException(AddEmploymentTelephoneNumberPage, form.yesNoTextEntry.getOrElse(""))
                .setOrException(
                  AddEmploymentTelephoneQuestionPage,
                  form.yesNoChoice.getOrElse(FormValuesConstants.NoValue)
                )
            case _                                              =>
              request.userAnswers.setOrException(
                AddEmploymentTelephoneQuestionPage,
                form.yesNoChoice.getOrElse(FormValuesConstants.NoValue)
              )
          }
          for {
            _ <- journeyCacheRepository.set(userAnswers)
          } yield Redirect(controllers.employments.routes.AddEmploymentController.addEmploymentCheckYourAnswers())
        }
      )
  }

  def addEmploymentCheckYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    (
      request.userAnswers.get(AddEmploymentNamePage),
      request.userAnswers.get(AddEmploymentStartDatePage),
      request.userAnswers.get(AddEmploymentPayrollNumberPage),
      request.userAnswers.get(AddPayeRefPage),
      request.userAnswers.get(AddEmploymentTelephoneQuestionPage),
      request.userAnswers.get(AddEmploymentTelephoneNumberPage)
    ) match {
      case (
            Some(name),
            Some(startDate),
            Some(payrollNumber),
            Some(payeRef),
            Some(telephoneQuestion),
            telephoneNumber
          ) =>
        val model =
          IncomeCheckYourAnswersViewModel(
            Messages("add.missing.employment"),
            name,
            startDate.toString,
            payrollNumber,
            payeRef,
            telephoneQuestion,
            telephoneNumber,
            controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url,
            controllers.employments.routes.AddEmploymentController.submitYourAnswers().url,
            controllers.employments.routes.AddEmploymentController.cancel().url
          )
        Ok(addIncomeCheckYourAnswers(model))

      case _ =>
        Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitYourAnswers: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    (
      request.userAnswers.get(AddEmploymentNamePage),
      request.userAnswers.get(AddEmploymentStartDatePage),
      request.userAnswers.get(AddEmploymentPayrollNumberPage),
      request.userAnswers.get(AddPayeRefPage),
      request.userAnswers.get(AddEmploymentTelephoneQuestionPage),
      request.userAnswers.get(AddEmploymentTelephoneNumberPage)
    ) match {
      case (
            Some(name),
            Some(startDate),
            Some(payrollNumber),
            Some(payeRef),
            Some(telephoneQuestion),
            telephoneNumber
          ) =>
        val model = AddEmployment(
          name,
          startDate,
          payrollNumber,
          payeRef,
          telephoneQuestion,
          telephoneNumber
        )
        for {
          _ <- employmentService.addEmployment(user.nino, model)
          _ <- journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
          _ <- {
            // setting for tracking service
            val updatedUserAnswers =
              UserAnswers(request.userAnswers.sessionId, request.userAnswers.nino)
                .setOrException(AddEmploymentPage, true)
            journeyCacheRepository.set(updatedUserAnswers)
          }
        } yield Redirect(controllers.employments.routes.AddEmploymentController.confirmation())
      case _ => Future.successful(error5xxInBadRequest())
    }
  }

  def confirmation: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    Future.successful(Ok(confirmationView()))

  }

}
