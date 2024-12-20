/*
 * Copyright 2024 HM Revenue & Customs
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
import pages.TrackSuccessfulJourneyUpdatePensionPage
import pages.updatePensionProvider._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneRegex
import uk.gov.hmrc.tai.forms.pensions.{DuplicateSubmissionWarningForm, UpdateRemovePensionForm, WhatDoYouWantToTellUsForm}
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{IncorrectPensionProvider, PensionIncome}
import uk.gov.hmrc.tai.service.{PensionProviderService, TaxAccountService}
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.journeyCache.EmptyCacheRedirect
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel
import uk.gov.hmrc.tai.viewModels.pensions.update.UpdatePensionCheckYourAnswersViewModel
import views.html.CanWeContactByPhoneView
import views.html.pensions.DuplicateSubmissionWarningView
import views.html.pensions.update._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UpdatePensionProviderController @Inject() (
  taxAccountService: TaxAccountService,
  pensionProviderService: PensionProviderService,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  applicationConfig: ApplicationConfig,
  canWeContactByPhone: CanWeContactByPhoneView,
  doYouGetThisPensionIncome: DoYouGetThisPensionIncomeView,
  whatDoYouWantToTellUsView: WhatDoYouWantToTellUsView,
  updatePensionCheckYourAnswers: UpdatePensionCheckYourAnswersView,
  confirmationView: ConfirmationView,
  duplicateSubmissionWarningView: DuplicateSubmissionWarningView,
  journeyCacheNewRepository: JourneyCacheNewRepository,
  errorPagesHandler: ErrorPagesHandler
)(implicit val ec: ExecutionContext)
    extends TaiBaseController(mcc) with EmptyCacheRedirect {

  def cancel(empId: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    journeyCacheNewRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino).map { _ =>
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

  def doYouGetThisPension(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    val userAnswers = request.userAnswers
    val idOpt = userAnswers.get(UpdatePensionProviderIdPage)
    val nameOpt = userAnswers.get(UpdatePensionProviderNamePage)
    val receivePensionOpt = userAnswers.get(UpdatePensionProviderReceivePensionPage)

    (idOpt, nameOpt, receivePensionOpt) match {
      case (Some(id), Some(name), receivePensionOpt) =>
        val model = PensionProviderViewModel(id, name)
        val form = UpdateRemovePensionForm.form.fill(receivePensionOpt)
        Future.successful(Ok(doYouGetThisPensionIncome(model, form)))
      case _ =>
        Future.successful(Redirect(taxAccountSummaryRedirect))
    }
  }

  def handleDoYouGetThisPension: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers

    val idOpt = userAnswers.get(UpdatePensionProviderIdPage)
    val nameOpt = userAnswers.get(UpdatePensionProviderNamePage)

    (idOpt, nameOpt) match {
      case (Some(id), Some(name)) =>
        UpdateRemovePensionForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val model = PensionProviderViewModel(id, name)
              implicit val user: AuthedUser = request.taiUser

              Future(BadRequest(doYouGetThisPensionIncome(model, formWithErrors)))
            },
            {
              case Some(FormValuesConstants.YesValue) =>
                val updatedAnswers =
                  userAnswers.setOrException(UpdatePensionProviderReceivePensionPage, Messages("tai.label.yes"))
                journeyCacheNewRepository
                  .set(updatedAnswers)
                  .map { _ =>
                    Redirect(controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs())
                  }
              case _ => Future.successful(Redirect(applicationConfig.incomeFromEmploymentPensionLinkUrl))
            }
          )
      case _ =>
        Future.successful(errorPagesHandler.internalServerError(s"Mandatory values missing: id=$idOpt, name=$nameOpt"))
    }
  }

  def whatDoYouWantToTellUs: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers
    val nameOpt = userAnswers.get(UpdatePensionProviderNamePage)
    val idOpt = userAnswers.get(UpdatePensionProviderIdPage)
    val detailsOpt = userAnswers.get(UpdatePensionProviderDetailsPage)

    (nameOpt, idOpt, detailsOpt) match {
      case (Some(name), Some(id), details) =>
        implicit val user: AuthedUser = request.taiUser
        val form = WhatDoYouWantToTellUsForm.form.fill(details.getOrElse(""))
        Future.successful(Ok(whatDoYouWantToTellUsView(name, id, form)))
      case _ => Future.successful(Redirect(taxAccountSummaryRedirect))
    }
  }

  def submitWhatDoYouWantToTellUs: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers

    WhatDoYouWantToTellUsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val nameOpt = userAnswers.get(UpdatePensionProviderNamePage)
          val idOpt = userAnswers.get(UpdatePensionProviderIdPage)
          (nameOpt, idOpt) match {
            case (Some(name), Some(id)) =>
              implicit val user: AuthedUser = request.taiUser
              Future.successful(BadRequest(whatDoYouWantToTellUsView(name, id, formWithErrors)))
            case _ =>
              Future.successful(
                errorPagesHandler.internalServerError(s"Mandatory values missing: id=$idOpt, name=$nameOpt")
              )
          }
        },
        pensionDetails => {
          val updatedAnswers = request.userAnswers.setOrException(UpdatePensionProviderDetailsPage, pensionDetails)
          journeyCacheNewRepository.set(updatedAnswers).map { _ =>
            Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber())
          }
        }
      )
  }

  def addTelephoneNumber(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers
    val pensionIdOpt = userAnswers.get(UpdatePensionProviderIdPage)
    val telephoneQuestionOpt = userAnswers.get(UpdatePensionProviderPhoneQuestionPage)
    val telephoneNumberOpt = userAnswers.get(UpdatePensionProviderPhoneNumberPage)

    pensionIdOpt match {
      case Some(pensionId) =>
        val user = Some(request.taiUser)
        val form = YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneQuestionOpt, telephoneNumberOpt))
        Future.successful(Ok(canWeContactByPhone(user, telephoneNumberViewModel(pensionId), form)))

      case _ => Future.successful(Redirect(taxAccountSummaryRedirect))
    }

  }

  def submitTelephoneNumber: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)
      )
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val pensionId = request.userAnswers.get(UpdatePensionProviderIdPage).getOrElse(0)
          val user = Some(request.taiUser)

          Future.successful(
            BadRequest(canWeContactByPhone(user, telephoneNumberViewModel(pensionId), formWithErrors))
          )
        },
        form => {
          val phoneQuestionData = Messages(
            s"tai.label.${form.yesNoChoice.getOrElse(FormValuesConstants.NoValue).toLowerCase}"
          )
          val phoneNumberData = form.yesNoChoice.fold("") {
            case FormValuesConstants.YesValue => form.yesNoTextEntry.getOrElse("")
            case _                            => ""
          }

          val updatedAnswers = request.userAnswers
            .setOrException(UpdatePensionProviderPhoneQuestionPage, phoneQuestionData)
            .setOrException(UpdatePensionProviderPhoneNumberPage, phoneNumberData)

          journeyCacheNewRepository.set(updatedAnswers).map { _ =>
            Redirect(controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers

    val pensionIdOpt = userAnswers.get(UpdatePensionProviderIdPage)
    val pensionNameOpt = userAnswers.get(UpdatePensionProviderNamePage)
    val receivePensionQuestionOpt = userAnswers.get(UpdatePensionProviderReceivePensionPage)
    val detailsOpt = userAnswers.get(UpdatePensionProviderDetailsPage)
    val phoneQuestionOpt = userAnswers.get(UpdatePensionProviderPhoneQuestionPage)
    val phoneNumberOpt = userAnswers.get(UpdatePensionProviderPhoneNumberPage)

    (pensionIdOpt, pensionNameOpt, receivePensionQuestionOpt, detailsOpt, phoneQuestionOpt, phoneNumberOpt) match {
      case (
            Some(pensionId),
            Some(pensionName),
            Some(receivePensionQuestion),
            Some(details),
            Some(phoneQuestion),
            phoneNumberOpt
          ) =>
        implicit val user: AuthedUser = request.taiUser
        Future.successful(
          Ok(
            updatePensionCheckYourAnswers(
              UpdatePensionCheckYourAnswersViewModel(
                pensionId,
                pensionName,
                receivePensionQuestion,
                details,
                phoneQuestion,
                phoneNumberOpt
              )
            )
          )
        )

      case _ => Future.successful(Redirect(taxAccountSummaryRedirect))
    }
  }

  def submitYourAnswers(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val nino = request.taiUser.nino
    val userAnswers = request.userAnswers

    val pensionIdOpt = userAnswers.get(UpdatePensionProviderIdPage)
    val detailsOpt = userAnswers.get(UpdatePensionProviderDetailsPage)
    val phoneQuestionOpt = userAnswers.get(UpdatePensionProviderPhoneQuestionPage)
    val phoneNumberOpt = userAnswers.get(UpdatePensionProviderPhoneNumberPage)

    (pensionIdOpt, detailsOpt, phoneQuestionOpt, phoneNumberOpt) match {
      case (Some(pensionId), Some(details), Some(phoneQuestion), phoneNumberOpt) =>
        val model = IncorrectPensionProvider(details, phoneQuestion, phoneNumberOpt)
        for {
          _ <- pensionProviderService.incorrectPensionProvider(nino, pensionId, model)
          _ <- journeyCacheNewRepository.clear(request.userAnswers.sessionId, request.userAnswers.nino)
          _ <- {
            val newUserAnswers = UserAnswers(request.userAnswers.sessionId, request.userAnswers.nino)
              .setOrException(TrackSuccessfulJourneyUpdatePensionPage(pensionId), "true")
            journeyCacheNewRepository.set(newUserAnswers)
          }
        } yield Redirect(controllers.pensions.routes.UpdatePensionProviderController.confirmation())

      case _ =>
        Future.successful(
          errorPagesHandler.internalServerError("Mandatory values missing from UserAnswers")
        )
    }
  }

  def confirmation(): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
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

  def UpdatePension(id: Int): Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    val userAnswers = request.userAnswers
    val cacheAndRedirect = (id: Int, taxCodeIncome: TaxCodeIncome) => {
      val successfulJourneyCacheFuture = Future.successful(userAnswers.get(TrackSuccessfulJourneyUpdatePensionPage(id)))

      val updatedAnswers = userAnswers
        .setOrException(UpdatePensionProviderIdPage, id)
        .setOrException(UpdatePensionProviderNamePage, taxCodeIncome.name)

      val journeyCacheFuture = journeyCacheNewRepository
        .set(updatedAnswers)
        .map(_ =>
          Map(
            UpdatePensionProviderIdPage.toString   -> id.toString,
            UpdatePensionProviderNamePage.toString -> taxCodeIncome.name
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

  def duplicateSubmissionWarning: Action[AnyContent] = authenticate.authWithDataRetrieval.async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val userAnswers = request.userAnswers
    val nameOpt = userAnswers.get(UpdatePensionProviderNamePage)
    val idOpt = userAnswers.get(UpdatePensionProviderIdPage)
    (nameOpt, idOpt) match {
      case (Some(name), Some(id)) =>
        Future.successful(Ok(duplicateSubmissionWarningView(DuplicateSubmissionWarningForm.createForm, name, id)))
      case _ => Future.successful(Redirect(taxAccountSummaryRedirect))
    }
  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = authenticate.authWithDataRetrieval.async {
    implicit request =>
      implicit val user: AuthedUser = request.taiUser

      val userAnswers = request.userAnswers
      val nameOpt = userAnswers.get(UpdatePensionProviderNamePage)
      val idOpt = userAnswers.get(UpdatePensionProviderIdPage)

      (nameOpt, idOpt) match {
        case (Some(name), Some(id)) =>
          DuplicateSubmissionWarningForm.createForm
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(duplicateSubmissionWarningView(formWithErrors, name, id))
                ),
              success =>
                success.yesNoChoice match {
                  case Some(FormValuesConstants.YesValue) =>
                    Future.successful(
                      Redirect(controllers.pensions.routes.UpdatePensionProviderController.doYouGetThisPension())
                    )
                  case Some(FormValuesConstants.NoValue) =>
                    Future.successful(
                      Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(id))
                    )
                }
            )
        case _ => Future.successful(errorPagesHandler.internalServerError("Mandatory values missing from UserAnswers"))
      }
  }
}
