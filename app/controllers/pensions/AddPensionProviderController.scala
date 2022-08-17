/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.forms.pensions.{AddPensionProviderFirstPayForm, AddPensionProviderNumberForm, PensionAddDateForm, PensionProviderNameForm}
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, PensionProviderService}
import uk.gov.hmrc.tai.util.FutureOps.FutureEitherStringOps
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.{CheckYourAnswersViewModel, PensionNumberViewModel}
import views.html.CanWeContactByPhoneView
import views.html.pensions._

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.control.NonFatal

class AddPensionProviderController @Inject()(
  pensionProviderService: PensionProviderService,
  auditService: AuditService,
  val auditConnector: AuditConnector,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  can_we_contact_by_phone: CanWeContactByPhoneView, //TODO remove once backLink issue is resolved
  addPensionConfirmationView: AddPensionConfirmationView,
  addPensionCheckYourAnswersView: AddPensionCheckYourAnswersView,
  addPensionNumber: AddPensionNumberView,
  addPensionErrorView: AddPensionErrorView,
  addPensionReceivedFirstPayView: AddPensionReceivedFirstPayView,
  addPensionNameView: AddPensionNameView,
  addPensionStartDateView: AddPensionStartDateView,
  @Named("Add Pension Provider") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  implicit val templateRenderer: TemplateRenderer,
  errorPagesHandler: ErrorPagesHandler)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with AuditConstants with FormValuesConstants
    with EmptyCacheRedirect {

  private def contactPhonePensionProvider(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("add.missing.pension"),
      messages("tai.canWeContactByPhone.title"),
      controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url,
      controllers.pensions.routes.AddPensionProviderController.submitTelephoneNumber().url,
      controllers.pensions.routes.AddPensionProviderController.cancel().url
    )

  def cancel(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def addPensionProviderName(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.currentValue(AddPensionProvider_NameKey) map { pensionName =>
      implicit val user: AuthedUser = request.taiUser
      Ok(addPensionNameView(PensionProviderNameForm.form.fill(pensionName.getOrElse(""))))
    }
  }

  def submitPensionProviderName(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      PensionProviderNameForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(addPensionNameView(formWithErrors)))
        },
        pensionProviderName => {
          journeyCacheService
            .cache(Map(AddPensionProvider_NameKey -> pensionProviderName))
            .map(_ => Redirect(controllers.pensions.routes.AddPensionProviderController.receivedFirstPay()))
        }
      )
  }

  def receivedFirstPay(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .collectedJourneyValues(Seq(AddPensionProvider_NameKey), Seq(AddPensionProvider_FirstPaymentKey)) map {
      case Right((mandatoryValues, optionalVals)) =>
        Ok(
          addPensionReceivedFirstPayView(
            AddPensionProviderFirstPayForm.form.fill(optionalVals.head),
            mandatoryValues.head))
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitFirstPay(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    AddPensionProviderFirstPayForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          journeyCacheService.mandatoryJourneyValue(AddPensionProvider_NameKey).map {
            case Right(pensionProviderName) =>
              BadRequest(addPensionReceivedFirstPayView(formWithErrors, pensionProviderName))
            case Left(err) =>
              InternalServerError(err)
          }
        },
        yesNo => {
          journeyCacheService.cache(AddPensionProvider_FirstPaymentKey, yesNo.getOrElse("")) map { _ =>
            yesNo match {
              case Some(YesValue) =>
                journeyCacheService.cache("", "")
                Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate())
              case _ => Redirect(controllers.pensions.routes.AddPensionProviderController.cantAddPension())
            }
          }
        }
      )
  }

  def cantAddPension(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.mandatoryJourneyValue(AddPensionProvider_NameKey) map {
      case Right(pensionProviderName) =>
        auditService
          .createAndSendAuditEvent(AddPension_CantAddPensionProvider, Map("nino" -> request.taiUser.nino.toString()))
        implicit val user: AuthedUser = request.taiUser

        Ok(addPensionErrorView(pensionProviderName))
      case Left(err) =>
        InternalServerError(err)
    }
  }

  def addPensionProviderStartDate(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .collectedJourneyValues(Seq(AddPensionProvider_NameKey), Seq(AddPensionProvider_StartDateKey))
        .map {
          case Right((mandatorySequence, optionalVals)) =>
            val form = optionalVals.head match {
              case Some(userDateString) =>
                PensionAddDateForm(mandatorySequence.head).form.fill(LocalDate.parse(userDateString))
              case _ => PensionAddDateForm(mandatorySequence.head).form
            }
            Ok(addPensionStartDateView(form, mandatorySequence.head))
          case Left(_) => Redirect(taxAccountSummaryRedirect)
        }
        .recover {
          case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
        }
  }

  def submitPensionProviderStartDate(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService.currentCache flatMap { currentCache =>
        PensionAddDateForm(currentCache(AddPensionProvider_NameKey)).form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              Future.successful(
                BadRequest(addPensionStartDateView(formWithErrors, currentCache(AddPensionProvider_NameKey))))
            },
            date => {
              journeyCacheService.cache(AddPensionProvider_StartDateKey, date.toString) map { _ =>
                Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionNumber())
              }
            }
          )
      }
  }

  def addPensionNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService.currentCache map { cache =>
      val viewModel = PensionNumberViewModel(cache)

      val payrollNo = cache.get(AddPensionProvider_PayrollNumberChoice) match {
        case Some(YesValue) => cache.get(AddPensionProvider_PayrollNumberKey)
        case _              => None
      }

      Ok(
        addPensionNumber(
          AddPensionProviderNumberForm.form.fill(
            AddPensionProviderNumberForm(cache.get(AddPensionProvider_PayrollNumberChoice), payrollNo)),
          viewModel))
    }
  }

  def submitPensionNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    AddPensionProviderNumberForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          journeyCacheService.currentCache map { cache =>
            val viewModel = PensionNumberViewModel(cache)
            BadRequest(addPensionNumber(formWithErrors, viewModel))
          }
        },
        form => {
          val payrollNumberToCache = Map(
            AddPensionProvider_PayrollNumberChoice -> form.payrollNumberChoice.getOrElse(Messages("tai.label.no")),
            AddPensionProvider_PayrollNumberKey    -> form.payrollNumberEntry.getOrElse(Messages("tai.notKnown.response"))
          )
          journeyCacheService
            .cache(payrollNumberToCache)
            .map(_ => Redirect(controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber()))
        }
      )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService
      .optionalValues(AddPensionProvider_TelephoneQuestionKey, AddPensionProvider_TelephoneNumberKey) map { seq =>
      val telephoneNo = seq.head match {
        case Some(YesValue) => seq(1)
        case _              => None
      }
      val user = Some(request.taiUser)

      Ok(
        can_we_contact_by_phone(
          user,
          contactPhonePensionProvider,
          YesNoTextEntryForm.form().fill(YesNoTextEntryForm(seq.head, telephoneNo))))
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint))
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val user = Some(request.taiUser)
          Future.successful(BadRequest(can_we_contact_by_phone(user, contactPhonePensionProvider, formWithErrors)))
        },
        form => {
          val mandatoryData = Map(
            AddPensionProvider_TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == YesValue =>
              mandatoryData ++ Map(AddPensionProvider_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(AddPensionProvider_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.pensions.routes.AddPensionProviderController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheService
      .collectedJourneyValues(
        Seq(
          AddPensionProvider_NameKey,
          AddPensionProvider_StartDateKey,
          AddPensionProvider_PayrollNumberKey,
          AddPensionProvider_TelephoneQuestionKey),
        Seq(AddPensionProvider_TelephoneNumberKey)
      )
      .map {
        case Right((mandatoryValues, optionalVals)) =>
          val model = CheckYourAnswersViewModel(
            mandatoryValues.head,
            mandatoryValues(1),
            mandatoryValues(2),
            mandatoryValues(3),
            optionalVals.head
          )
          Ok(addPensionCheckYourAnswersView(model))
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }
      .recover {
        case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
      }
  }

  def submitYourAnswers: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      (mandatoryVals, optionalVals) <- journeyCacheService
                                        .collectedJourneyValues(
                                          Seq(
                                            AddPensionProvider_NameKey,
                                            AddPensionProvider_StartDateKey,
                                            AddPensionProvider_PayrollNumberKey,
                                            AddPensionProvider_TelephoneQuestionKey),
                                          Seq(AddPensionProvider_TelephoneNumberKey)
                                        )
                                        .getOrFail
      model = AddPensionProvider(
        mandatoryVals.head,
        LocalDate.parse(mandatoryVals(1)),
        mandatoryVals(2),
        mandatoryVals.last,
        optionalVals.head)
      _ <- pensionProviderService.addPensionProvider(user.nino, model)
      _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourney_AddPensionProviderKey, "true")
      _ <- journeyCacheService.flush()
    } yield {
      Redirect(controllers.pensions.routes.AddPensionProviderController.confirmation())
    }
  }

  def confirmation: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    Future.successful(Ok(addPensionConfirmationView()))
  }

}
