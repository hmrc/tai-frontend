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

package controllers.pensions

import controllers.actions.ValidatePerson
import controllers.auth.{AuthJourney, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.forms.pensions.{AddPensionProviderFirstPayForm, AddPensionProviderNumberForm, PensionAddDateForm, PensionProviderNameForm}
import uk.gov.hmrc.tai.model.domain.AddPensionProvider
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, PensionProviderService}
import uk.gov.hmrc.tai.util.FutureOps.FutureEitherStringOps
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.constants.{AuditConstants, FormValuesConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.{CheckYourAnswersViewModel, PensionNumberViewModel}
import views.html.CanWeContactByPhoneView
import views.html.pensions._

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AddPensionProviderController @Inject() (
  pensionProviderService: PensionProviderService,
  auditService: AuditService,
  val auditConnector: AuditConnector,
  authenticate: AuthJourney,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  canWeContactByPhone: CanWeContactByPhoneView, // TODO remove once backLink issue is resolved
  addPensionConfirmationView: AddPensionConfirmationView,
  addPensionCheckYourAnswersView: AddPensionCheckYourAnswersView,
  addPensionNumber: AddPensionNumberView,
  addPensionErrorView: AddPensionErrorView,
  addPensionReceivedFirstPayView: AddPensionReceivedFirstPayView,
  addPensionNameView: AddPensionNameView,
  addPensionStartDateView: AddPensionStartDateView,
  @Named("Add Pension Provider") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  errorPagesHandler: ErrorPagesHandler
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect {

  private def contactPhonePensionProvider(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("add.missing.pension"),
      messages("tai.canWeContactByPhone.title"),
      controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url,
      controllers.pensions.routes.AddPensionProviderController.submitTelephoneNumber().url,
      controllers.pensions.routes.AddPensionProviderController.cancel().url
    )

  def cancel(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
    }
  }

  def addPensionProviderName(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.currentValue(AddPensionProviderConstants.NameKey) map { pensionName =>
      implicit val user: AuthedUser = request.taiUser
      Ok(addPensionNameView(PensionProviderNameForm.form.fill(pensionName.getOrElse(""))))
    }
  }

  def submitPensionProviderName(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    PensionProviderNameForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(addPensionNameView(formWithErrors))),
        pensionProviderName =>
          journeyCacheService
            .cache(Map(AddPensionProviderConstants.NameKey -> pensionProviderName))
            .map(_ => Redirect(controllers.pensions.routes.AddPensionProviderController.receivedFirstPay()))
      )
  }

  def receivedFirstPay(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .collectedJourneyValues(
        Seq(AddPensionProviderConstants.NameKey),
        Seq(AddPensionProviderConstants.FirstPaymentKey)
      ) map {
      case Right((mandatoryValues, optionalVals)) =>
        Ok(
          addPensionReceivedFirstPayView(
            AddPensionProviderFirstPayForm.form.fill(optionalVals.head),
            mandatoryValues.head
          )
        )
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitFirstPay(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    AddPensionProviderFirstPayForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheService.mandatoryJourneyValue(AddPensionProviderConstants.NameKey).map {
            case Right(pensionProviderName) =>
              BadRequest(addPensionReceivedFirstPayView(formWithErrors, pensionProviderName))
            case Left(err) =>
              InternalServerError(err)
          },
        yesNo =>
          journeyCacheService.cache(AddPensionProviderConstants.FirstPaymentKey, yesNo.getOrElse("")) map { _ =>
            yesNo match {
              case Some(FormValuesConstants.YesValue) =>
                journeyCacheService.cache("", "")
                Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate())
              case _ => Redirect(controllers.pensions.routes.AddPensionProviderController.cantAddPension())
            }
          }
      )
  }

  def cantAddPension(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService.mandatoryJourneyValue(AddPensionProviderConstants.NameKey) map {
      case Right(pensionProviderName) =>
        auditService
          .createAndSendAuditEvent(
            AuditConstants.AddPensionCantAddPensionProvider,
            Map("nino" -> request.taiUser.nino.toString())
          )
        implicit val user: AuthedUser = request.taiUser

        Ok(addPensionErrorView(pensionProviderName))
      case Left(err) =>
        InternalServerError(err)
    }
  }

  def addPensionProviderStartDate(): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .collectedJourneyValues(Seq(AddPensionProviderConstants.NameKey), Seq(AddPensionProviderConstants.StartDateKey))
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
        .recover { case NonFatal(e) =>
          errorPagesHandler.internalServerError(e.getMessage)
        }
  }

  def submitPensionProviderStartDate(): Action[AnyContent] = authenticate.authWithValidatePerson.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService.currentCache flatMap { currentCache =>
        PensionAddDateForm(currentCache(AddPensionProviderConstants.NameKey)).form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(addPensionStartDateView(formWithErrors, currentCache(AddPensionProviderConstants.NameKey)))
              ),
            date =>
              journeyCacheService.cache(AddPensionProviderConstants.StartDateKey, date.toString) map { _ =>
                Redirect(controllers.pensions.routes.AddPensionProviderController.addPensionNumber())
              }
          )
      }
  }

  def addPensionNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService.currentCache map { cache =>
      val viewModel = PensionNumberViewModel(cache)

      val payrollNo = cache.get(AddPensionProviderConstants.PayrollNumberChoice) match {
        case Some(FormValuesConstants.YesValue) => cache.get(AddPensionProviderConstants.PayrollNumberKey)
        case _                                  => None
      }

      Ok(
        addPensionNumber(
          AddPensionProviderNumberForm.form.fill(
            AddPensionProviderNumberForm(cache.get(AddPensionProviderConstants.PayrollNumberChoice), payrollNo)
          ),
          viewModel
        )
      )
    }
  }

  def submitPensionNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    AddPensionProviderNumberForm.form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheService.currentCache map { cache =>
            val viewModel = PensionNumberViewModel(cache)
            BadRequest(addPensionNumber(formWithErrors, viewModel))
          },
        form => {
          val payrollNumberToCache = Map(
            AddPensionProviderConstants.PayrollNumberChoice -> form.payrollNumberChoice
              .getOrElse(Messages("tai.label.no")),
            AddPensionProviderConstants.PayrollNumberKey -> form.payrollNumberEntry
              .getOrElse(Messages("tai.notKnown.response"))
          )
          journeyCacheService
            .cache(payrollNumberToCache)
            .map(_ => Redirect(controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber()))
        }
      )
  }

  def addTelephoneNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    journeyCacheService
      .optionalValues(
        AddPensionProviderConstants.TelephoneQuestionKey,
        AddPensionProviderConstants.TelephoneNumberKey
      ) map { seq =>
      val telephoneNo = seq.head match {
        case Some(FormValuesConstants.YesValue) => seq(1)
        case _                                  => None
      }
      val user = Some(request.taiUser)

      Ok(
        canWeContactByPhone(
          user,
          contactPhonePensionProvider,
          YesNoTextEntryForm.form().fill(YesNoTextEntryForm(seq.head, telephoneNo))
        )
      )
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
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
          val mandatoryData = Map(
            AddPensionProviderConstants.TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}"
            )
          )
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == FormValuesConstants.YesValue =>
              mandatoryData ++ Map(AddPensionProviderConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(AddPensionProviderConstants.TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.pensions.routes.AddPensionProviderController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheService
      .collectedJourneyValues(
        Seq(
          AddPensionProviderConstants.NameKey,
          AddPensionProviderConstants.StartDateKey,
          AddPensionProviderConstants.PayrollNumberKey,
          AddPensionProviderConstants.TelephoneQuestionKey
        ),
        Seq(AddPensionProviderConstants.TelephoneNumberKey)
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
      .recover { case NonFatal(e) =>
        errorPagesHandler.internalServerError(e.getMessage)
      }
  }

  def submitYourAnswers: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      (mandatoryVals, optionalVals) <- journeyCacheService
                                         .collectedJourneyValues(
                                           Seq(
                                             AddPensionProviderConstants.NameKey,
                                             AddPensionProviderConstants.StartDateKey,
                                             AddPensionProviderConstants.PayrollNumberKey,
                                             AddPensionProviderConstants.TelephoneQuestionKey
                                           ),
                                           Seq(AddPensionProviderConstants.TelephoneNumberKey)
                                         )
                                         .getOrFail
      model = AddPensionProvider(
                mandatoryVals.head,
                LocalDate.parse(mandatoryVals(1)),
                mandatoryVals(2),
                mandatoryVals.last,
                optionalVals.head
              )
      _ <- pensionProviderService.addPensionProvider(user.nino, model)
      _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourneyConstants.AddPensionProviderKey, "true")
      _ <- journeyCacheService.flush()
    } yield Redirect(controllers.pensions.routes.AddPensionProviderController.confirmation())
  }

  def confirmation: Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    Future.successful(Ok(addPensionConfirmationView()))
  }

}
