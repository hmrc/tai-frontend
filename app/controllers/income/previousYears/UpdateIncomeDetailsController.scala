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

package controllers.income.previousYears

import javax.inject.{Inject, Named}
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.forms.income.previousYears.{UpdateIncomeDetailsDecisionForm, UpdateIncomeDetailsForm}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IncorrectIncome
import uk.gov.hmrc.tai.service.PreviousYearsIncomeService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.income.previousYears.{UpdateHistoricIncomeDetailsViewModel, UpdateIncomeDetailsCheckYourAnswersViewModel}
import views.html.incomes.previousYears.CheckYourAnswers

import scala.Function.tupled
import scala.concurrent.Future

class UpdateIncomeDetailsController @Inject()(previousYearsIncomeService: PreviousYearsIncomeService,
                                              authenticate: AuthAction,
                                              validatePerson: ValidatePerson,
                                              @Named("Track Successful Journey") trackingJourneyCacheService: JourneyCacheService,
                                              @Named("Update Previous Years Income") journeyCacheService: JourneyCacheService,
                                              override implicit val partialRetriever: FormPartialRetriever,
                                              override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with JourneyCacheConstants
  with FormValuesConstants {

  def telephoneNumberViewModel(taxYear: Int)(implicit messages: Messages): CanWeContactByPhoneViewModel = CanWeContactByPhoneViewModel(
    messages("tai.income.previousYears.journey.preHeader"),
    messages("tai.canWeContactByPhone.title"),
    controllers.income.previousYears.routes.UpdateIncomeDetailsController.details.url,
    controllers.income.previousYears.routes.UpdateIncomeDetailsController.submitTelephoneNumber().url,
    controllers.routes.PayeControllerHistoric.payePage(TaxYear(taxYear)).url
  )

  def decision(taxYear: TaxYear): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      journeyCacheService.cache(Map(UpdatePreviousYearsIncome_TaxYearKey -> taxYear.year.toString)) map { _ =>
        implicit val user = request.taiUser
        Ok(views.html.incomes.previousYears.UpdateIncomeDetailsDecision(UpdateIncomeDetailsDecisionForm.form, taxYear))
      }
  }

  def submitDecision(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      UpdateIncomeDetailsDecisionForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.incomes.previousYears.UpdateIncomeDetailsDecision(formWithErrors, TaxYear().prev)))
        },
        decision => {
          decision match {
            case Some(NoValue) => Future.successful(Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.details()))
            case _ => Future.successful(Redirect(controllers.routes.PayeControllerHistoric.payePage(TaxYear().prev)))
          }
        }
      )
  }

  def details(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.currentCache map {
        currentCache =>
          Ok(views.html.incomes.previousYears.UpdateIncomeDetails(UpdateHistoricIncomeDetailsViewModel(currentCache(UpdatePreviousYearsIncome_TaxYearKey).toInt),
            UpdateIncomeDetailsForm.form))
      }
  }

  def submitDetails(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      UpdateIncomeDetailsForm.form.bindFromRequest.fold(
        formWithErrors => {
          journeyCacheService.currentCache map {
            currentCache =>
              BadRequest(views.html.incomes.previousYears.UpdateIncomeDetails(
                UpdateHistoricIncomeDetailsViewModel(currentCache(UpdatePreviousYearsIncome_TaxYearKey).toInt), formWithErrors))
          }
        },
        incomeDetails => {
          journeyCacheService.cache(Map(UpdatePreviousYearsIncome_IncomeDetailsKey -> incomeDetails))
            .map(_ => Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.telephoneNumber()))
        }
      )
  }

  def telephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.currentCache map { currentCache =>
        Ok(views.html.can_we_contact_by_phone(Some(user), telephoneNumberViewModel(currentCache(UpdatePreviousYearsIncome_TaxYearKey).toInt), YesNoTextEntryForm.form()))
      }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      YesNoTextEntryForm.form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
        formWithErrors => {
          journeyCacheService.currentCache map { currentCache =>
            BadRequest(views.html.can_we_contact_by_phone(Some(user), telephoneNumberViewModel(currentCache(UpdatePreviousYearsIncome_TaxYearKey).toInt), formWithErrors))
          }
        },
        form => {
          val mandatoryData = Map(UpdatePreviousYearsIncome_TelephoneQuestionKey -> form.yesNoChoice.getOrElse(NoValue))
          val dataForCache = form.yesNoChoice match {
            case Some(YesValue) => mandatoryData ++ Map(UpdatePreviousYearsIncome_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(UpdatePreviousYearsIncome_TelephoneNumberKey -> "")
          }
          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      journeyCacheService.collectedValues(
        Seq(
          UpdatePreviousYearsIncome_TaxYearKey,
          UpdatePreviousYearsIncome_IncomeDetailsKey,
          UpdatePreviousYearsIncome_TelephoneQuestionKey),
        Seq(
          UpdatePreviousYearsIncome_TelephoneNumberKey
        )) map tupled { (mandatorySeq, optionalSeq) => {
        Ok(CheckYourAnswers(UpdateIncomeDetailsCheckYourAnswersViewModel(
          TaxYear(mandatorySeq.head.toInt),
          mandatorySeq(1),
          mandatorySeq(2),
          optionalSeq.head)))
      }
      }
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      val nino = user.nino

      for {
        (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService.collectedValues(Seq(UpdatePreviousYearsIncome_TaxYearKey,
          UpdatePreviousYearsIncome_IncomeDetailsKey, UpdatePreviousYearsIncome_TelephoneQuestionKey),
          Seq(UpdatePreviousYearsIncome_TelephoneNumberKey))
        model = IncorrectIncome(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
        _ <- previousYearsIncomeService.incorrectIncome(nino, mandatoryCacheSeq.head.toInt, model)
        _ <- trackingJourneyCacheService.cache(TrackSuccessfulJourney_UpdatePreviousYearsIncomeKey, true.toString)
        _ <- journeyCacheService.flush
      } yield Redirect(controllers.income.previousYears.routes.UpdateIncomeDetailsController.confirmation())
  }

  def confirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser

      Future.successful(Ok(views.html.incomes.previousYears.UpdateIncomeDetailsConfirmation()))
  }

}
