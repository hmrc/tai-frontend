/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Named}
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.pensions.{DuplicateSubmissionWarningForm, UpdateRemovePensionForm, WhatDoYouWantToTellUsForm}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{IncorrectPensionProvider, PensionIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel
import uk.gov.hmrc.tai.viewModels.pensions.update.UpdatePensionCheckYourAnswersViewModel

import scala.Function.tupled
import scala.concurrent.Future
import scala.util.control.NonFatal

class UpdatePensionProviderController @Inject()(taxAccountService: TaxAccountService,
                                                pensionProviderService: PensionProviderService,
                                                auditService: AuditService,
                                                authenticate: AuthAction,
                                                validatePerson: ValidatePerson,
                                                @Named("Update Pension Provider") journeyCacheService: JourneyCacheService,
                                                @Named("Track Successful Journey") successfulJourneyCacheService: JourneyCacheService,
                                                override implicit val partialRetriever: FormPartialRetriever,
                                                override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants
  with FormValuesConstants {

  def cancel(empId: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.flush() map { _ =>
        Redirect(controllers.routes.IncomeSourceSummaryController.onPageLoad(empId))
      }
  }

  def telephoneNumberSizeConstraint(implicit messages: Messages): Constraint[String] =
    Constraint[String]((textContent: String) => textContent match {
      case txt if txt.length < 8 || txt.length > 30 => Invalid(messages("tai.canWeContactByPhone.telephone.invalid"))
      case _ => Valid
    })

  def telephoneNumberViewModel(pensionId: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel = CanWeContactByPhoneViewModel(
    messages("tai.updatePension.preHeading"),
    messages("tai.canWeContactByPhone.title"),
    "/thisBackLinkUrlIsNoLongerUsed",
    controllers.pensions.routes.UpdatePensionProviderController.submitTelephoneNumber().url,
    controllers.pensions.routes.UpdatePensionProviderController.cancel(pensionId).url
  )

  def doYouGetThisPension(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.collectedValues(Seq(UpdatePensionProvider_IdKey,UpdatePensionProvider_NameKey), Seq(UpdatePensionProvider_ReceivePensionQuestionKey)) map tupled { (mandatoryValues, optionalValues) =>
        val model = PensionProviderViewModel(mandatoryValues.head.toInt, mandatoryValues(1))
        val form = UpdateRemovePensionForm.form.fill(optionalValues.head)
        Ok(views.html.pensions.update.doYouGetThisPensionIncome(model, form))
      }
  }


  def handleDoYouGetThisPension: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.mandatoryValues(UpdatePensionProvider_IdKey, UpdatePensionProvider_NameKey) flatMap { mandatoryVals =>
        UpdateRemovePensionForm.form.bindFromRequest().fold(
          formWithErrors => {
            val model = PensionProviderViewModel(mandatoryVals.head.toInt, mandatoryVals.last)
            implicit val user = request.taiUser

            Future(BadRequest(views.html.pensions.update.doYouGetThisPensionIncome(model, formWithErrors)))
          },
          {
            case Some(YesValue) =>
              journeyCacheService.cache(UpdatePensionProvider_ReceivePensionQuestionKey, Messages("tai.label.yes")).map { _ =>
                Redirect(controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs())
              }
            case _ => Future.successful(Redirect(ApplicationConfig.incomeFromEmploymentPensionLinkUrl))
          }
        )
      }
  }

  def whatDoYouWantToTellUs: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      for {
        (mandatoryValues, optionalValues) <- journeyCacheService.collectedValues(Seq(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey),
          Seq(UpdatePensionProvider_DetailsKey))
      } yield {
        implicit val user = request.taiUser

        Ok(views.html.pensions.update.whatDoYouWantToTellUs(mandatoryValues.head, mandatoryValues(1).toInt,
          WhatDoYouWantToTellUsForm.form.fill(optionalValues.head.getOrElse(""))))
      }
  }

  def submitWhatDoYouWantToTellUs: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      WhatDoYouWantToTellUsForm.form.bindFromRequest.fold(
        formWithErrors => {
          journeyCacheService.mandatoryValues(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey) map { mandatoryValues =>
            implicit val user = request.taiUser
            BadRequest(views.html.pensions.update.whatDoYouWantToTellUs(mandatoryValues.head, mandatoryValues(1).toInt, formWithErrors))
          }
        },
        pensionDetails => {
          journeyCacheService.cache(Map(UpdatePensionProvider_DetailsKey -> pensionDetails))
            .map(_ => Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber()))
        }
      )
  }

  def addTelephoneNumber: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      for {
        pensionId <- journeyCacheService.mandatoryValueAsInt(UpdatePensionProvider_IdKey)
        telephoneCache <- journeyCacheService.optionalValues(UpdatePensionProvider_TelephoneQuestionKey, UpdatePensionProvider_TelephoneNumberKey)
      } yield {
        val user = Some(request.taiUser)

        Ok(views.html.can_we_contact_by_phone(user, telephoneNumberViewModel(pensionId),
          YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache(0), telephoneCache(1)))))
      }
  }

  def submitTelephoneNumber: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      YesNoTextEntryForm.form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
        formWithErrors => {
          journeyCacheService.currentCache map { currentCache =>
            val user = Some(request.taiUser)
            BadRequest(views.html.can_we_contact_by_phone(user, telephoneNumberViewModel(currentCache(UpdatePensionProvider_IdKey).toInt), formWithErrors))
          }
        },
        form => {
          val mandatoryData = Map(UpdatePensionProvider_TelephoneQuestionKey -> Messages(s"tai.label.${form.yesNoChoice.getOrElse(NoValue).toLowerCase}"))
          val dataForCache = form.yesNoChoice match {
            case Some(yn) if yn == YesValue => mandatoryData ++ Map(UpdatePensionProvider_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(UpdatePensionProvider_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.pensions.routes.UpdatePensionProviderController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.collectedValues(
        Seq(
          UpdatePensionProvider_IdKey,
          UpdatePensionProvider_NameKey,
          UpdatePensionProvider_ReceivePensionQuestionKey,
          UpdatePensionProvider_DetailsKey,
          UpdatePensionProvider_TelephoneQuestionKey),
        Seq(UpdatePensionProvider_TelephoneNumberKey)
      ) map tupled { (mandatorySeq, optionalSeq) => {
        implicit val user = request.taiUser

        Ok(views.html.pensions.update.updatePensionCheckYourAnswers(UpdatePensionCheckYourAnswersViewModel(
          mandatorySeq.head.toInt,
          mandatorySeq(1),
          mandatorySeq(2),
          mandatorySeq(3),
          mandatorySeq(4),
          optionalSeq.head)))
      }
      }
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      val nino = request.taiUser.nino

      for {
        (mandatoryCacheSeq, optionalCacheSeq) <-
          journeyCacheService.collectedValues(Seq(
            UpdatePensionProvider_IdKey,
            UpdatePensionProvider_DetailsKey,
            UpdatePensionProvider_TelephoneQuestionKey),
            Seq(UpdatePensionProvider_TelephoneNumberKey))
        model = IncorrectPensionProvider(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
        _ <- pensionProviderService.incorrectPensionProvider(nino, mandatoryCacheSeq.head.toInt, model)
        _ <- successfulJourneyCacheService.cache(s"$TrackSuccessfulJourney_UpdatePensionKey-${mandatoryCacheSeq.head}", true.toString)
        _ <- journeyCacheService.flush
      } yield Redirect(controllers.pensions.routes.UpdatePensionProviderController.confirmation())
  }

  def confirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      Future.successful(Ok(views.html.pensions.update.confirmation()))
  }

  private def redirectToWarningOrDecisionPage(journeyCacheFuture: Future[Map[String, String]],
                                              successfulJourneyCacheFuture: Future[Option[String]])
                                             (implicit hc: HeaderCarrier): Future[Result] = {
    for {
      _ <- journeyCacheFuture
      successfulJourneyCache <- successfulJourneyCacheFuture
    } yield {
      successfulJourneyCache match {
        case Some(_) => Redirect(routes.UpdatePensionProviderController.duplicateSubmissionWarning())
        case _ => Redirect(routes.UpdatePensionProviderController.doYouGetThisPension())
      }
    }
  }

  def UpdatePension(id: Int): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      val cacheAndRedirect = (id: Int, taxCodeIncome: TaxCodeIncome) => {
        val successfulJourneyCacheFuture = successfulJourneyCacheService.currentValue(s"$TrackSuccessfulJourney_UpdatePensionKey-${id}")
        val journeyCacheFuture = journeyCacheService.cache(Map(UpdatePensionProvider_IdKey -> id.toString,
          UpdatePensionProvider_NameKey -> taxCodeIncome.name))

        redirectToWarningOrDecisionPage(journeyCacheFuture, successfulJourneyCacheFuture)
      }

      (taxAccountService.taxCodeIncomes(request.taiUser.nino, TaxYear()) flatMap {
        case TaiSuccessResponseWithPayload(incomes: Seq[TaxCodeIncome]) =>
          incomes.find(income => income.employmentId.contains(id) &&
            income.componentType == PensionIncome) match {
            case Some(taxCodeIncome) => cacheAndRedirect(id, taxCodeIncome)
            case _ => throw new RuntimeException(s"Tax code income source is not available for id $id")
          }
        case _ => throw new RuntimeException("Tax code income source is not available")
      }).recover {
        case NonFatal(e) => internalServerError(e.getMessage)
      }

  }

  def duplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey) map { mandatoryValues =>
        Ok(views.html.pensions.duplicateSubmissionWarning(DuplicateSubmissionWarningForm.createForm, mandatoryValues(0), mandatoryValues(1).toInt))
      }

  }

  def submitDuplicateSubmissionWarning: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      journeyCacheService.mandatoryValues(UpdatePensionProvider_NameKey, UpdatePensionProvider_IdKey) flatMap { mandatoryValues =>
        DuplicateSubmissionWarningForm.createForm.bindFromRequest.fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.pensions.
              duplicateSubmissionWarning(formWithErrors, mandatoryValues(0), mandatoryValues(1).toInt)))
          },
          success => {
            success.yesNoChoice match {
              case Some(YesValue) => Future.successful(Redirect(controllers.pensions.routes.UpdatePensionProviderController.
                doYouGetThisPension()))
              case Some(NoValue) =>
                Future.successful(Redirect(controllers.routes.IncomeSourceSummaryController.
                onPageLoad(mandatoryValues(1).toInt)))
            }
          }
        )
      }
  }
}
