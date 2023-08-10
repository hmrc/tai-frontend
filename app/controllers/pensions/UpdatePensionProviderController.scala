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
import controllers.auth.{AuthAction, AuthedUser}
import controllers.{ErrorPagesHandler, TaiBaseController}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneRegex
import uk.gov.hmrc.tai.forms.pensions.{DuplicateSubmissionWarningForm, UpdateRemovePensionForm, WhatDoYouWantToTellUsForm}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{IncorrectPensionProvider, PensionIncome}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, PensionProviderService, TaxAccountService}
import uk.gov.hmrc.tai.util.FutureOps._
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel
import uk.gov.hmrc.tai.viewModels.pensions.update.UpdatePensionCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.pensions.DuplicateSubmissionWarningView
import views.html.pensions.update.{ConfirmationView, DoYouGetThisPensionIncomeView, UpdatePensionCheckYourAnswersView, WhatDoYouWantToTellUsView}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UpdatePensionProviderController @Inject() (
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
  errorPagesHandler: ErrorPagesHandler
)(implicit val ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect {

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
      }
    )

  def telephoneNumberViewModel(pensionId: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel =
    CanWeContactByPhoneViewModel(
      messages("tai.updatePension.preHeading"),
      messages("tai.canWeContactByPhone.title"),
      controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs().url,
      controllers.pensions.routes.UpdatePensionProviderController.submitTelephoneNumber().url,
      controllers.pensions.routes.UpdatePensionProviderController.cancel(pensionId).url
    )

  def doYouGetThisPension(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheService.collectedJourneyValues(
      Seq(UpdatePensionProviderConstants.IdKey, UpdatePensionProviderConstants.NameKey),
      Seq(UpdatePensionProviderConstants.ReceivePensionQuestionKey)
    ) map {
      case Right((mandatoryValues, optionalValues)) =>
        val model = PensionProviderViewModel(mandatoryValues.head.toInt, mandatoryValues(1))
        val form = UpdateRemovePensionForm.form.fill(optionalValues.head)
        Ok(doYouGetThisPensionIncome(model, form))
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def handleDoYouGetThisPension: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService
      .mandatoryJourneyValues(UpdatePensionProviderConstants.IdKey, UpdatePensionProviderConstants.NameKey) flatMap {
      case Right(mandatoryVals) =>
        UpdateRemovePensionForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val model = PensionProviderViewModel(mandatoryVals.head.toInt, mandatoryVals.last)
              implicit val user: AuthedUser = request.taiUser

              Future(BadRequest(doYouGetThisPensionIncome(model, formWithErrors)))
            },
            {
              case Some(FormValuesConstants.YesValue) =>
                journeyCacheService
                  .cache(UpdatePensionProviderConstants.ReceivePensionQuestionKey, Messages("tai.label.yes"))
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
        Seq(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey),
        Seq(UpdatePensionProviderConstants.DetailsKey)
      )
      .map {
        case Right((mandatoryValues, optionalValues)) =>
          implicit val user: AuthedUser = request.taiUser
          Ok(
            whatDoYouWantToTellUsView(
              mandatoryValues.head,
              mandatoryValues(1).toInt,
              WhatDoYouWantToTellUsForm.form.fill(optionalValues.head.getOrElse(""))
            )
          )
        case Left(_) => Redirect(taxAccountSummaryRedirect)
      }
  }

  def submitWhatDoYouWantToTellUs: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      WhatDoYouWantToTellUsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            journeyCacheService
              .mandatoryJourneyValues(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey)
              .getOrFail map { mandatoryValues =>
              implicit val user: AuthedUser = request.taiUser
              BadRequest(whatDoYouWantToTellUsView(mandatoryValues.head, mandatoryValues(1).toInt, formWithErrors))
            },
          pensionDetails =>
            journeyCacheService
              .cache(Map(UpdatePensionProviderConstants.DetailsKey -> pensionDetails))
              .map(_ => Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber()))
        )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    for {
      pensionId <- journeyCacheService.mandatoryJourneyValueAsInt(UpdatePensionProviderConstants.IdKey)
      telephoneCache <- journeyCacheService.optionalValues(
                          UpdatePensionProviderConstants.TelephoneQuestionKey,
                          UpdatePensionProviderConstants.TelephoneNumberKey
                        )
    } yield pensionId match {
      case Right(mandatoryPensionId) =>
        val user = Some(request.taiUser)

        Ok(
          canWeContactByPhone(
            user,
            telephoneNumberViewModel(mandatoryPensionId),
            YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache.head, telephoneCache(1)))
          )
        )
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }

  }

  def submitTelephoneNumber: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors =>
          journeyCacheService.currentCache map { currentCache =>
            val user = Some(request.taiUser)

            BadRequest(
              canWeContactByPhone(
                user,
                telephoneNumberViewModel(currentCache(UpdatePensionProviderConstants.IdKey).toInt),
                formWithErrors
              )
            )
          },
        form => {
          val mandatoryData = Map(
            UpdatePensionProviderConstants.TelephoneQuestionKey -> Messages(
              s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}"
            )
          )
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == FormValuesConstants.YesValue =>
              mandatoryData ++ Map(
                UpdatePensionProviderConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse("")
              )
            case _ => mandatoryData ++ Map(UpdatePensionProviderConstants.TelephoneNumberKey -> "")
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
          UpdatePensionProviderConstants.IdKey,
          UpdatePensionProviderConstants.NameKey,
          UpdatePensionProviderConstants.ReceivePensionQuestionKey,
          UpdatePensionProviderConstants.DetailsKey,
          UpdatePensionProviderConstants.TelephoneQuestionKey
        ),
        Seq(UpdatePensionProviderConstants.TelephoneNumberKey)
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
                optionalSeq.head
              )
            )
          )

        case Left(_) => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad())
      }
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    for {
      (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService
                                                 .collectedJourneyValues(
                                                   Seq(
                                                     UpdatePensionProviderConstants.IdKey,
                                                     UpdatePensionProviderConstants.DetailsKey,
                                                     UpdatePensionProviderConstants.TelephoneQuestionKey
                                                   ),
                                                   Seq(UpdatePensionProviderConstants.TelephoneNumberKey)
                                                 )
                                                 .getOrFail
      model = IncorrectPensionProvider(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
      _ <- pensionProviderService.incorrectPensionProvider(nino, mandatoryCacheSeq.head.toInt, model)
      _ <- successfulJourneyCacheService
             .cache(s"${TrackSuccessfulJourneyConstants.UpdatePensionKey}-${mandatoryCacheSeq.head}", true.toString)
      _ <- journeyCacheService.flush()
    } yield Redirect(controllers.pensions.routes.UpdatePensionProviderController.confirmation())
  }

  def confirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    Future.successful(Ok(confirmationView()))
  }

  private def redirectToWarningOrDecisionPage(
    journeyCacheFuture: Future[Map[String, String]],
    successfulJourneyCacheFuture: Future[Option[String]]
  ): Future[Result] =
    for {
      _                      <- journeyCacheFuture
      successfulJourneyCache <- successfulJourneyCacheFuture
    } yield successfulJourneyCache match {
      case Some(_) => Redirect(routes.UpdatePensionProviderController.duplicateSubmissionWarning())
      case _       => Redirect(routes.UpdatePensionProviderController.doYouGetThisPension())
    }

  def UpdatePension(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val cacheAndRedirect = (id: Int, taxCodeIncome: TaxCodeIncome) => {
      val successfulJourneyCacheFuture =
        successfulJourneyCacheService.currentValue(s"${TrackSuccessfulJourneyConstants.UpdatePensionKey}-$id")
      val journeyCacheFuture = journeyCacheService.cache(
        Map(
          UpdatePensionProviderConstants.IdKey   -> id.toString,
          UpdatePensionProviderConstants.NameKey -> taxCodeIncome.name
        )
      )

      redirectToWarningOrDecisionPage(journeyCacheFuture, successfulJourneyCacheFuture)
    }

    (taxAccountService.taxCodeIncomes(request.taiUser.nino, TaxYear()) flatMap {
      case Right(incomes) =>
        incomes.find(income =>
          income.employmentId.contains(id) &&
            income.componentType == PensionIncome
        ) match {
          case Some(taxCodeIncome) => cacheAndRedirect(id, taxCodeIncome)
          case _                   => throw new RuntimeException(s"Tax code income source is not available for id $id")
        }
      case _ => throw new RuntimeException("Tax code income source is not available")
    }).recover { case NonFatal(e) =>
      errorPagesHandler.internalServerError(e.getMessage)
    }

  }

  def duplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .mandatoryJourneyValues(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey) map {
      case Right(mandatoryValues) =>
        Ok(
          duplicateSubmissionWarningView(
            DuplicateSubmissionWarningForm.createForm,
            mandatoryValues.head,
            mandatoryValues(1).toInt
          )
        )
      case Left(_) => Redirect(taxAccountSummaryRedirect)
    }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .mandatoryJourneyValues(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey)
        .getOrFail flatMap { mandatoryValues =>
        DuplicateSubmissionWarningForm.createForm
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(
                  duplicateSubmissionWarningView(formWithErrors, mandatoryValues.head, mandatoryValues(1).toInt)
                )
              ),
            success =>
              success.yesNoChoice match {
                case Some(FormValuesConstants.YesValue) =>
                  Future.successful(
                    Redirect(controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension())
                  )
                case Some(FormValuesConstants.NoValue) =>
                  Future.successful(
                    Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(mandatoryValues(1).toInt))
                  )
              }
          )
      }
  }
}
