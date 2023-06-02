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
import uk.gov.hmrc.http.HeaderCarrier
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
import scala.Function.tupled
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.tai.util.FutureOps._

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
    journeyCacheService
      .flush()
      .map { _ =>
        Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
      }
      .getOrElse(Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId)))
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
      controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs.url,
      controllers.pensions.routes.UpdatePensionProviderController.submitTelephoneNumber.url,
      controllers.pensions.routes.UpdatePensionProviderController.cancel(pensionId).url
    )

  def doYouGetThisPension(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .collectedJourneyValues(
        Seq(UpdatePensionProviderConstants.IdKey, UpdatePensionProviderConstants.NameKey),
        Seq(UpdatePensionProviderConstants.ReceivePensionQuestionKey)
      )
      .fold(
        _ => Redirect(taxAccountSummaryRedirect),
        data => {
          val model = PensionProviderViewModel(data._1.head.toInt, data._1(1))
          val form = UpdateRemovePensionForm.form.fill(data._2.head)
          Ok(doYouGetThisPensionIncome(model, form))
        }
      )
  }

  def handleDoYouGetThisPension: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService
      .mandatoryJourneyValues(UpdatePensionProviderConstants.IdKey, UpdatePensionProviderConstants.NameKey)
      .foldF(
        error => Future.successful(errorPagesHandler.internalServerError(error.message)),
        result =>
          UpdateRemovePensionForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val model = PensionProviderViewModel(result.head.toInt, result.last)
                implicit val user: AuthedUser = request.taiUser

                Future.successful(BadRequest(doYouGetThisPensionIncome(model, formWithErrors)))
              },
              {
                case Some(FormValuesConstants.YesValue) =>
                  journeyCacheService
                    .cache(UpdatePensionProviderConstants.ReceivePensionQuestionKey, Messages("tai.label.yes"))
                    .map { _ =>
                      Redirect(controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs)
                    }
                    .getOrElse(
                      Redirect(controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs)
                    ) // TODO - Check correct behaviour
                case _ => Future.successful(Redirect(applicationConfig.incomeFromEmploymentPensionLinkUrl))
              }
            )
      )
  }

  def whatDoYouWantToTellUs: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    journeyCacheService
      .collectedJourneyValues(
        Seq(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey),
        Seq(UpdatePensionProviderConstants.DetailsKey)
      )
      .fold(
        _ => Redirect(taxAccountSummaryRedirect),
        data => {
          implicit val user: AuthedUser = request.taiUser
          Ok(
            whatDoYouWantToTellUsView(
              data._1.head,
              data._1(1).toInt,
              WhatDoYouWantToTellUsForm.form.fill(data._2.head.getOrElse(""))
            )
          )
        }
      )
  }

  def submitWhatDoYouWantToTellUs: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      WhatDoYouWantToTellUsForm.form.bindFromRequest.fold(
        formWithErrors =>
          journeyCacheService
            .mandatoryJourneyValues(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey)
            .fold(
              error => errorPagesHandler.internalServerError(error.message),
              data => {
                implicit val user: AuthedUser = request.taiUser
                BadRequest(whatDoYouWantToTellUsView(data.head, data(1).toInt, formWithErrors))
              }
            ),
        pensionDetails =>
          journeyCacheService
            .cache(Map(UpdatePensionProviderConstants.DetailsKey -> pensionDetails))
            .map(_ => Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber))
            .getOrElse(
              Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber)
            ) // TODO - Check correct behaviour
      )
  }

  def addTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson)
    .async { implicit request =>
      {
        for {
          pensionId <- journeyCacheService.mandatoryJourneyValueAsInt(UpdatePensionProviderConstants.IdKey)
          telephoneCache <- journeyCacheService.optionalValues(
                              UpdatePensionProviderConstants.TelephoneQuestionKey,
                              UpdatePensionProviderConstants.TelephoneNumberKey
                            )
        } yield {
          val user = Some(request.taiUser)
          Ok(
            canWeContactByPhone(
              user,
              telephoneNumberViewModel(pensionId),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache.head, telephoneCache(1)))
            )
          )
        }
      }.getOrElse(Redirect(taxAccountSummaryRedirect))
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
          {
            val user = Some(request.taiUser)
            journeyCacheService.currentCache.map { currentCache =>
              BadRequest(
                canWeContactByPhone(
                  user,
                  telephoneNumberViewModel(currentCache(UpdatePensionProviderConstants.IdKey).toInt),
                  formWithErrors
                )
              )
            }
          }.getOrElse(
            Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber)
          ), // TODO - Check correct behaviour
        form =>
          {
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
              Redirect(controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers)
            }
          }.getOrElse(
            Redirect(controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers)
          ) // TODO - Check correct behaviour
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
      .fold(
        _ => Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad),
        data => {
          implicit val user: AuthedUser = request.taiUser
          Ok(
            updatePensionCheckYourAnswers(
              UpdatePensionCheckYourAnswersViewModel(
                data._1.head.toInt,
                data._1(1),
                data._1(2),
                data._1(3),
                data._1(4),
                data._2.head
              )
            )
          )
        }
      )
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val nino = request.taiUser.nino

    journeyCacheService
      .collectedJourneyValues(
        Seq(
          UpdatePensionProviderConstants.IdKey,
          UpdatePensionProviderConstants.DetailsKey,
          UpdatePensionProviderConstants.TelephoneQuestionKey
        ),
        Seq(UpdatePensionProviderConstants.TelephoneNumberKey)
      )
      .map { cacheSeq =>
        val model = IncorrectPensionProvider(cacheSeq._1(1), cacheSeq._1(2), cacheSeq._2.head); (cacheSeq, model)
      }
      .flatMap { case (cacheSeq, model) =>
        pensionProviderService
          .incorrectPensionProvider(nino, cacheSeq._1.head.toInt, model)
          .flatMap(_ =>
            successfulJourneyCacheService
              .cache(s"${TrackSuccessfulJourneyConstants.UpdatePensionKey}-${cacheSeq._1.head}", true.toString)
              .flatMap(_ =>
                journeyCacheService.flush
                  .map(_ => Redirect(controllers.pensions.routes.UpdatePensionProviderController.confirmation))
              )
          )
      }
      .getOrElse(errorPagesHandler.internalServerError("Submission of answers failed"))
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
      case Some(_) => Redirect(routes.UpdatePensionProviderController.duplicateSubmissionWarning)
      case _       => Redirect(routes.UpdatePensionProviderController.doYouGetThisPension)
    }

  def updatePension(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    def cacheAndRedirect = (id: Int, taxCodeIncome: TaxCodeIncome) => {
      val successfulJourneyCacheFuture =
        successfulJourneyCacheService
          .currentValue(s"${TrackSuccessfulJourneyConstants.UpdatePensionKey}-$id")
          .getOrElse(None) // TODO - Check if correct behaviour
      val journeyCacheFuture = journeyCacheService
        .cache(
          Map(
            UpdatePensionProviderConstants.IdKey   -> id.toString,
            UpdatePensionProviderConstants.NameKey -> taxCodeIncome.name
          )
        )
        .getOrElse(Map.empty[String, String]) // TODO - Check if correct behaviour
      redirectToWarningOrDecisionPage(journeyCacheFuture, successfulJourneyCacheFuture)
    }

    taxAccountService
      .taxCodeIncomes(request.taiUser.nino, TaxYear())
      .foldF(
        error => Future.successful(errorPagesHandler.internalServerError(error.message)),
        result =>
          result.find(income => income.employmentId.contains(id) && income.componentType == PensionIncome) match {
            case Some(taxCodeIncome) => cacheAndRedirect(id, taxCodeIncome)
            case _ =>
              Future.successful(
                errorPagesHandler.internalServerError(s"Tax code income source is not available for id $id")
              ) // TODO - Check correct behaviour
          }
      )
  }

  def duplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    journeyCacheService
      .mandatoryJourneyValues(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey)
      .fold(
        _ => Redirect(taxAccountSummaryRedirect),
        mandatoryValues =>
          Ok(
            duplicateSubmissionWarningView(
              DuplicateSubmissionWarningForm.createForm,
              mandatoryValues.head,
              mandatoryValues(1).toInt
            )
          )
      )
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser
      journeyCacheService
        .mandatoryJourneyValues(UpdatePensionProviderConstants.NameKey, UpdatePensionProviderConstants.IdKey)
        .fold(
          error => errorPagesHandler.internalServerError(error.message),
          mandatoryValues =>
            DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(
                  duplicateSubmissionWarningView(formWithErrors, mandatoryValues.head, mandatoryValues(1).toInt)
                ),
              success =>
                success.yesNoChoice match {
                  case Some(FormValuesConstants.YesValue) =>
                    Redirect(controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension)
                  case Some(FormValuesConstants.NoValue) =>
                    Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(mandatoryValues(1).toInt))
                }
            )
        )
  }
}
