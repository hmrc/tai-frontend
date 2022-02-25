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
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneRegex
import uk.gov.hmrc.tai.forms.pensions.{DuplicateSubmissionWarningForm, UpdateRemovePensionForm, WhatDoYouWantToTellUsForm}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{IncorrectPensionProvider, PensionIncome}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, PensionProviderService, TaxAccountService}
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel
import uk.gov.hmrc.tai.viewModels.pensions.update.UpdatePensionCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.pensions.DuplicateSubmissionWarningView
import views.html.pensions.update.{ConfirmationView, DoYouGetThisPensionIncomeView, UpdatePensionCheckYourAnswersView, WhatDoYouWantToTellUsView}

import javax.inject.{Inject, Named}
import scala.Function.tupled
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.tai.util.FutureOps._

class UpdatePensionProviderController @Inject()(
  taxAccountService: TaxAccountService,
  pensionProviderService: PensionProviderService,
  auditService: AuditService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  applicationConfig: ApplicationConfig,
  canWeContactByPhone: CanWeContactByPhoneView,
  doYouGetThisPensionIncome: DoYouGetThisPensionIncomeView,
  whatDoYouWantToTellUsView: WhatDoYouWantToTellUsView,
  updatePensionCheckYourAnswers: UpdatePensionCheckYourAnswersView,
  confirmationView: ConfirmationView,
  duplicateSubmissionWarningView: DuplicateSubmissionWarningView,
  @Named("Update Pension Provider") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
  errorPagesHandler: ErrorPagesHandler)(implicit val templateRenderer: TemplateRenderer, ec: ExecutionContext)
    extends TaiBaseController(mcc) with JourneyCacheConstants with FormValuesConstants with EmptyCacheRedirect {

  def cancel(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.flush() map { _ =>
      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
    }
  }

  def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) =>
      textContent match {
        case txt if txt.length < 8 || txt.length > 30 || telephoneRegex.findAllMatchIn(txt).isEmpty =>
          Invalid(messages("tai.canWeContactByPhone.telephone.invalid"))
        case _ => Valid
    })

  def telephoneNumberViewModel(pensionId: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("tai.updatePension.preHeading"),
      messages("tai.canWeContactByPhone.title"),
      "/thisBackLinkUrlIsNoLongerUsed",
      controllers.pensions.routes.UpdatePensionProviderController.submitTelephoneNumber().url,
      controllers.pensions.routes.UpdatePensionProviderController.cancel(pensionId).url
    )

  def doYouGetThisPension(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheService.collectedJourneyValues(
      Seq(UpdatePensionProvider_IdKey, UpdatePensionProvider_NameKey),
      Seq(UpdatePensionProvider_ReceivePensionQuestionKey)) map {
      case Right((mandatoryValues, optionalValues)) =>
        val model = PensionProviderViewModel(mandatoryValues.head.toInt, mandatoryValues(1))
        val form = UpdateRemovePensionForm.form.fill(optionalValues.head)
        Ok(doYouGetThisPensionIncome(model, form))
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def handleDoYouGetThisPension: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService.mandatoryJourneyValues(UpdatePensionProvider_IdKey, UpdatePensionProvider_NameKey) flatMap {
      case Right(mandatoryVals) =>
        UpdateRemovePensionForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val model = PensionProviderViewModel(mandatoryVals.head.toInt, mandatoryVals.last)
              implicit val user: AuthedUser = request.taiUser

              Future(BadRequest(doYouGetThisPensionIncome(model, formWithErrors)))
            }, {
              case Some(YesValue) =>
                journeyCacheService
                  .cache(UpdatePensionProvider_ReceivePensionQuestionKey, Messages("tai.label.yes"))
                  .map { _ =>
                    Redirect(controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs())
                  }
              case _ => Future.successful(Redirect(applicationConfig.incomeFromEmploymentPensionLinkUrl))
            }
          )
      case Left(message) => Future.successful(errorPagesHandler.internalServerError(message))
    }
  }

  def whatDoYouWantToTellUs: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService
      .collectedJourneyValues(
        Seq(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey),
        Seq(UpdatePensionProvider_DetailsKey))
      .map {
        case Right((mandatoryValues, optionalValues)) =>
          implicit val user: AuthedUser = request.taiUser
          Ok(
            whatDoYouWantToTellUsView(
              mandatoryValues.head,
              mandatoryValues(1).toInt,
              WhatDoYouWantToTellUsForm.form.fill(optionalValues.head.getOrElse(""))))
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }
  }

  def submitWhatDoYouWantToTellUs: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      WhatDoYouWantToTellUsForm.form.bindFromRequest.fold(
        formWithErrors => {
          journeyCacheService
            .mandatoryJourneyValues(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey)
            .getOrFail map { mandatoryValues =>
            implicit val user: AuthedUser = request.taiUser
            BadRequest(whatDoYouWantToTellUsView(mandatoryValues.head, mandatoryValues(1).toInt, formWithErrors))
          }
        },
        pensionDetails => {
          journeyCacheService
            .cache(Map(UpdatePensionProvider_DetailsKey -> pensionDetails))
            .map(_ => Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber()))
        }
      )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    for {
      pensionId <- journeyCacheService.mandatoryJourneyValueAsInt(UpdatePensionProvider_IdKey)
      telephoneCache <- journeyCacheService.optionalValues(
                         UpdatePensionProvider_TelephoneQuestionKey,
                         UpdatePensionProvider_TelephoneNumberKey)
    } yield {

      pensionId match {
        case Right(mandatoryPensionId) =>
          val user = Some(request.taiUser)

          Ok(
            canWeContactByPhone(
              user,
              telephoneNumberViewModel(mandatoryPensionId),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache.head, telephoneCache(1)))))
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }

    }
  }

  def submitTelephoneNumber: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint))
      .bindFromRequest()
      .fold(
        formWithErrors => {
          journeyCacheService.currentCache map { currentCache =>
            val user = Some(request.taiUser)

            BadRequest(
              canWeContactByPhone(
                user,
                telephoneNumberViewModel(currentCache(UpdatePensionProvider_IdKey).toInt),
                formWithErrors))
          }
        },
        form => {
          val mandatoryData = Map(
            UpdatePensionProvider_TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == YesValue =>
              mandatoryData ++ Map(UpdatePensionProvider_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(UpdatePensionProvider_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService
      .collectedJourneyValues(
        Seq(
          UpdatePensionProvider_IdKey,
          UpdatePensionProvider_NameKey,
          UpdatePensionProvider_ReceivePensionQuestionKey,
          UpdatePensionProvider_DetailsKey,
          UpdatePensionProvider_TelephoneQuestionKey
        ),
        Seq(UpdatePensionProvider_TelephoneNumberKey)
      )
      .map {
        case Right((mandatoryValues, optionalSeq)) =>
          implicit val user: AuthedUser = request.taiUser
          Ok(
            updatePensionCheckYourAnswers(
              UpdatePensionCheckYourAnswersViewModel(
                mandatoryValues.head.toInt,
                mandatoryValues(1),
                mandatoryValues(2),
                mandatoryValues(3),
                mandatoryValues(4),
                optionalSeq.head)))

        case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    for {
      (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService
                                                .collectedJourneyValues(
                                                  Seq(
                                                    UpdatePensionProvider_IdKey,
                                                    UpdatePensionProvider_DetailsKey,
                                                    UpdatePensionProvider_TelephoneQuestionKey),
                                                  Seq(UpdatePensionProvider_TelephoneNumberKey)
                                                )
                                                .getOrFail
      model = IncorrectPensionProvider(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
      _ <- pensionProviderService.incorrectPensionProvider(nino, mandatoryCacheSeq.head.toInt, model)
      _ <- successfulJourneyCacheService
            .cache(s"$TrackSuccessfulJourney_UpdatePensionKey-${mandatoryCacheSeq.head}", true.toString)
      _ <- journeyCacheService.flush
    } yield Redirect(controllers.pensions.routes.UpdatePensionProviderController.confirmation())
  }

  def confirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    Future.successful(Ok(confirmationView()))
  }

  private def redirectToWarningOrDecisionPage(
    journeyCacheFuture: Future[Map[String, String]],
    successfulJourneyCacheFuture: Future[Option[String]])(implicit hc: HeaderCarrier): Future[Result] =
    for {
      _                      <- journeyCacheFuture
      successfulJourneyCache <- successfulJourneyCacheFuture
    } yield {
      successfulJourneyCache match {
        case Some(_) => Redirect(routes.UpdatePensionProviderController.duplicateSubmissionWarning())
        case _       => Redirect(routes.UpdatePensionProviderController.doYouGetThisPension())
      }
    }

  def UpdatePension(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val cacheAndRedirect = (id: Int, taxCodeIncome: TaxCodeIncome) => {
      val successfulJourneyCacheFuture =
        successfulJourneyCacheService.currentValue(s"$TrackSuccessfulJourney_UpdatePensionKey-$id")
      val journeyCacheFuture = journeyCacheService.cache(
        Map(UpdatePensionProvider_IdKey -> id.toString, UpdatePensionProvider_NameKey -> taxCodeIncome.name))

      redirectToWarningOrDecisionPage(journeyCacheFuture, successfulJourneyCacheFuture)
    }

    (taxAccountService.taxCodeIncomes(request.taiUser.nino, TaxYear()) flatMap {
      case TaiSuccessResponseWithPayload(incomes: Seq[TaxCodeIncome]) =>
        incomes.find(
          income =>
            income.employmentId.contains(id) &&
              income.componentType == PensionIncome) match {
          case Some(taxCodeIncome) => cacheAndRedirect(id, taxCodeIncome)
          case _                   => throw new RuntimeException(s"Tax code income source is not available for id $id")
        }
      case _ => throw new RuntimeException("Tax code income source is not available")
    }).recover {
      case NonFatal(e) => errorPagesHandler.internalServerError(e.getMessage)
    }

  }

  def duplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService.mandatoryJourneyValues(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey) map {
      case Right(mandatoryValues) =>
        Ok(
          duplicateSubmissionWarningView(
            DuplicateSubmissionWarningForm.createForm,
            mandatoryValues.head,
            mandatoryValues(1).toInt))
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .mandatoryJourneyValues(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey)
        .getOrFail flatMap { mandatoryValues =>
        DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
          formWithErrors => {
            Future.successful(
              BadRequest(
                duplicateSubmissionWarningView(formWithErrors, mandatoryValues.head, mandatoryValues(1).toInt)))
          },
          success => {
            success.yesNoChoice match {
              case Some(YesValue) =>
                Future.successful(
                  Redirect(controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension()))
              case Some(NoValue) =>
                Future.successful(
                  Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(mandatoryValues(1).toInt)))
            }
          }
        )
      }
  }
}
