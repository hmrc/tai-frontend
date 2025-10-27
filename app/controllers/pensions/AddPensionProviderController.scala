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

package controllers.pensions

import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import pages.AddPayeRefPage
import pages.addPensionProvider.*
import play.api.i18n.Messages
import play.api.mvc.*
import repository.JourneyCacheRepository
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.forms.pensions.{AddPensionProviderFirstPayForm, AddPensionProviderNumberForm, PensionAddDateForm, PensionProviderNameForm}
import uk.gov.hmrc.tai.forms.{PayeRefForm, YesNoTextEntryForm}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service.{AuditService, PensionProviderService}
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.{CheckYourAnswersViewModel, PensionNumberViewModel}
import views.html.CanWeContactByPhoneView
import views.html.employments.PayeRefFormView
import views.html.pensions.*

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddPensionProviderController @Inject() (
  pensionProviderService: PensionProviderService,
  auditService: AuditService,
  val auditConnector: AuditConnector,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  canWeContactByPhone: CanWeContactByPhoneView, // TODO remove once backLink issue is resolved
  addPensionConfirmationView: AddPensionConfirmationView,
  addPensionCheckYourAnswersView: AddPensionCheckYourAnswersView,
  addPensionNumberView: AddPensionNumberView,
  addPensionErrorView: AddPensionErrorView,
  addPensionReceivedFirstPayView: AddPensionReceivedFirstPayView,
  addPensionNameView: AddPensionNameView,
  addPensionStartDateView: AddPensionStartDateView,
  payeRefFormView: PayeRefFormView,
  journeyCacheRepository: JourneyCacheRepository,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc)
    with EmptyCacheRedirect {

  private def contactPhonePensionProvider(implicit messages: Messages) =
    CanWeContactByPhoneViewModel(
      messages("add.missing.pension"),
      messages("tai.canWeContactByPhone.title"),
      controllers.pensions.routes.AddPensionProviderController.addPayeReference().url,
      controllers.pensions.routes.AddPensionProviderController.submitTelephoneNumber().url,
      controllers.pensions.routes.AddPensionProviderController.cancel().url
    )

  def cancel(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino) map { _ =>
      Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def addPensionProviderName(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    Ok(
      addPensionNameView(
        PensionProviderNameForm.form.fill(request.userAnswers.get(AddPensionProviderNamePage).getOrElse(""))
      )
    )
  }

  def submitPensionProviderName(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    PensionProviderNameForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(addPensionNameView(formWithErrors))),
        pensionProviderName =>
          for {
            _ <- journeyCacheRepository
                   .set(request.userAnswers.setOrException(AddPensionProviderNamePage, pensionProviderName))
          } yield Redirect(controllers.pensions.routes.AddPensionProviderController.receivedFirstPay())
      )
  }

  def receivedFirstPay(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    (
      request.userAnswers.get(AddPensionProviderNamePage),
      request.userAnswers.get(AddPensionProviderFirstPaymentPage)
    ) match {
      case (Some(mandatoryValues), optionalVals) =>
        Ok(
          addPensionReceivedFirstPayView(
            AddPensionProviderFirstPayForm.form.fill(optionalVals),
            mandatoryValues
          )
        )
      case (None, _)                             => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitFirstPay(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    AddPensionProviderFirstPayForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          request.userAnswers.get(AddPensionProviderNamePage) match {
            case Some(pensionProviderName) =>
              Future.successful(
                BadRequest(
                  addPensionReceivedFirstPayView(
                    formWithErrors,
                    pensionProviderName
                  )
                )
              )
            case None                      =>
              Future.successful(InternalServerError("No pension Data present in cache"))
          },
        yesNo =>
          for {
            _ <- journeyCacheRepository
                   .set(request.userAnswers.setOrException(AddPensionProviderFirstPaymentPage, yesNo.getOrElse("")))
          } yield yesNo match {
            case Some(FormValuesConstants.YesValue) =>
              Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate())
            case _                                  => Redirect(controllers.pensions.routes.AddPensionProviderController.cantAddPension())
          }
      )
  }

  def cantAddPension(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    request.userAnswers.get(AddPensionProviderNamePage) match {
      case Some(pensionProviderName) =>
        auditService
          .createAndSendAuditEvent(
            AuditConstants.AddPensionCantAddPensionProvider,
            Map("nino" -> request.taiUser.nino.toString())
          )
        implicit val user: AuthedUser = request.taiUser
        Ok(addPensionErrorView(pensionProviderName))
      case None                      => InternalServerError("Pension provider name missing from cache")
    }
  }

  def addPensionProviderStartDate(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    (
      request.userAnswers.get(AddPensionProviderNamePage),
      request.userAnswers.get(AddPensionProviderStartDatePage)
    ) match {
      case (Some(name), Some(startDate)) =>
        Ok(addPensionStartDateView(PensionAddDateForm(name).form.fill(LocalDate.parse(startDate)), name))
      case (Some(name), None)            => Ok(addPensionStartDateView(PensionAddDateForm(name).form, name))
      case (None, _)                     => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitPensionProviderStartDate(): Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      PensionAddDateForm(request.userAnswers.get(AddPensionProviderNamePage).getOrElse("")).form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                addPensionStartDateView(
                  formWithErrors,
                  request.userAnswers.get(AddPensionProviderNamePage).getOrElse("")
                )
              )
            ),
          date =>
            for {
              _ <- journeyCacheRepository
                     .set(request.userAnswers.setOrException(AddPensionProviderStartDatePage, date.toString))
            } yield Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionNumber())
        )
  }

  def addPensionNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val viewModel = PensionNumberViewModel(request.userAnswers)

    val payrollChoice = request.userAnswers.get(AddPensionProviderPayrollNumberChoicePage)

    val payrollNo = payrollChoice match {
      case Some(FormValuesConstants.YesValue) => request.userAnswers.get(AddPensionProviderPayrollNumberPage)
      case _                                  => None
    }

    Ok(
      addPensionNumberView(
        AddPensionProviderNumberForm.form.fill(
          AddPensionProviderNumberForm(payrollChoice, payrollNo)
        ),
        viewModel
      )
    )
  }

  def submitPensionNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    AddPensionProviderNumberForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val viewModel = PensionNumberViewModel(request.userAnswers)
          Future.successful(BadRequest(addPensionNumberView(formWithErrors, viewModel)))
        },
        form =>
          for {
            _ <- journeyCacheRepository.set(
                   request.userAnswers
                     .setOrException(
                       AddPensionProviderPayrollNumberChoicePage,
                       form.payrollNumberChoice.getOrElse(Messages("tai.label.no"))
                     )
                     .setOrException(
                       AddPensionProviderPayrollNumberPage,
                       form.payrollNumberEntry.getOrElse(Messages("tai.notKnown.response"))
                     )
                 )
          } yield Redirect(controllers.pensions.routes.AddPensionProviderController.addPayeReference())
      )
  }

  def addPayeReference(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    request.userAnswers.get(AddPensionProviderNamePage) match {
      case Some(companyName) =>
        val existing = request.userAnswers.get(AddPayeRefPage).getOrElse("")

        val form = PayeRefForm.form(companyName, "pension").fill(existing)
        Ok(payeRefFormView(form, companyName, "pension"))
      case None              =>
        Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def submitPayeReference(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    request.userAnswers.get(AddPensionProviderNamePage) match {
      case Some(companyName) =>
        PayeRefForm
          .form(companyName, "pension")
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(payeRefFormView(formWithErrors, companyName, "pension"))),
            value => {
              val updatedAnswers = request.userAnswers.setOrException(AddPayeRefPage, value)
              for {
                _ <- journeyCacheRepository.set(updatedAnswers)
              } yield Redirect(controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber())
            }
          )
      case None              => Future.successful(error5xxInBadRequest())
    }
  }

  private def error5xxInBadRequest()(implicit request: Request[_]) =
    BadRequest(errorPagesHandler.error5xx(Messages("global.error.InternalServerError500.message")))

  def addTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    val telephoneQuestion = request.userAnswers.get(AddPensionProviderTelephoneQuestionPage)
    val telephoneNo       = request.userAnswers.get(AddPensionProviderTelephoneQuestionPage) match {
      case Some(FormValuesConstants.YesValue) => request.userAnswers.get(AddPensionProviderTelephoneNumberPage)
      case _                                  => None
    }
    val user              = Some(request.taiUser)

    Ok(
      canWeContactByPhone(
        user,
        contactPhonePensionProvider,
        YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneQuestion, telephoneNo))
      )
    )
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val user = Some(request.taiUser)
          Future.successful(BadRequest(canWeContactByPhone(user, contactPhonePensionProvider, formWithErrors)))
        },
        form => {
          val telephoneNumberValue = form.yesNoChoice match {
            case Some(yn) if yn == FormValuesConstants.YesValue => form.yesNoTextEntry.getOrElse("")
            case _                                              => ""
          }

          for {
            _ <- journeyCacheRepository.set(
                   request.userAnswers
                     .setOrException(
                       AddPensionProviderTelephoneQuestionPage,
                       Messages(s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}")
                     )
                     .setOrException(AddPensionProviderTelephoneNumberPage, telephoneNumberValue)
                 )
          } yield Redirect(controllers.pensions.routes.AddPensionProviderController.checkYourAnswers())
        }
      )
  }

  def checkYourAnswers: Action[AnyContent] = authenticate.authWithDataRetrieval { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val uA                        = request.userAnswers
    (
      uA.get(AddPensionProviderNamePage),
      uA.get(AddPensionProviderStartDatePage),
      uA.get(AddPensionProviderPayrollNumberPage),
      uA.get(AddPayeRefPage),
      uA.get(AddPensionProviderTelephoneQuestionPage),
      uA.get(AddPensionProviderTelephoneNumberPage)
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
          CheckYourAnswersViewModel(name, startDate, payrollNumber, payeRef, telephoneQuestion, telephoneNumber)
        Ok(addPensionCheckYourAnswersView(model))
      case _ => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitYourAnswers: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val uA                        = request.userAnswers
    (
      uA.get(AddPensionProviderNamePage),
      uA.get(AddPensionProviderStartDatePage),
      uA.get(AddPensionProviderPayrollNumberPage),
      uA.get(AddPayeRefPage),
      uA.get(AddPensionProviderTelephoneQuestionPage),
      uA.get(AddPensionProviderTelephoneNumberPage)
    ) match {
      case (
            Some(name),
            Some(startDate),
            Some(payrollNumber),
            Some(payeRef),
            Some(telephoneQuestion),
            telephoneNumber
          ) =>
        val model = AddPensionProvider(
          name,
          LocalDate.parse(startDate),
          payrollNumber,
          payeRef,
          telephoneQuestion,
          telephoneNumber
        )
        for {
          _ <- pensionProviderService.addPensionProvider(user.nino, model)
          _ <- journeyCacheRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
          _ <- {
            // setting for tracking service
            val updatedUserAnswers =
              UserAnswers(request.userAnswers.sessionId, request.userAnswers.nino)
                .setOrException(AddPensionProviderPage, true)
            journeyCacheRepository.set(updatedUserAnswers)
          }
        } yield Redirect(controllers.pensions.routes.AddPensionProviderController.confirmation())
    }
  }

  def confirmation: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    Future.successful(Ok(addPensionConfirmationView()))
  }

}
