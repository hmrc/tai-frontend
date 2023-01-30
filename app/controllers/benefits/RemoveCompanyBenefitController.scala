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

package controllers.benefits

import com.google.inject.name.Named
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.{AuthAction, AuthedUser}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.benefits.{CompanyBenefitTotalValueForm, RemoveCompanyBenefitStopDateForm}
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.benefits.EndedCompanyBenefit
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.FutureOps._
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, RemoveCompanyBenefitStopDateConstants}
import uk.gov.hmrc.tai.util.constants.journeyCache._
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.benefit.{BenefitViewModel, RemoveCompanyBenefitsCheckYourAnswersViewModel}
import views.html.CanWeContactByPhoneView
import views.html.benefits.{RemoveBenefitTotalValueView, RemoveCompanyBenefitCheckYourAnswersView, RemoveCompanyBenefitConfirmationView, RemoveCompanyBenefitStopDateView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class RemoveCompanyBenefitController @Inject()(
  @Named("End Company Benefit") journeyCacheService: JourneyCacheService,
  @Named("Track Successful Journey") trackingJourneyCacheService: JourneyCacheService,
  benefitsService: BenefitsService,
  authenticate: AuthAction,
  validatePerson: ValidatePerson,
  mcc: MessagesControllerComponents,
  langUtils: LanguageUtils,
  removeCompanyBenefitCheckYourAnswers: RemoveCompanyBenefitCheckYourAnswersView,
  removeCompanyBenefitStopDate: RemoveCompanyBenefitStopDateView,
  removeBenefitTotalValue: RemoveBenefitTotalValueView,
  canWeContactByPhone: CanWeContactByPhoneView,
  removeCompanyBenefitConfirmation: RemoveCompanyBenefitConfirmationView,
  implicit val templateRenderer: TemplateRenderer)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def stopDate: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheService.currentCache map { currentCache =>
      val form =
        RemoveCompanyBenefitStopDateForm.form.fill(currentCache.get(EndCompanyBenefitConstants.BenefitStopDateKey))

      Ok(
        removeCompanyBenefitStopDate(
          form,
          currentCache(EndCompanyBenefitConstants.BenefitNameKey),
          currentCache(EndCompanyBenefitConstants.EmploymentNameKey)))
    }
  }

  def submitStopDate: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    RemoveCompanyBenefitStopDateForm.form.bindFromRequest.fold(
      formWithErrors => {
        journeyCacheService
          .mandatoryJourneyValues(
            EndCompanyBenefitConstants.BenefitNameKey,
            EndCompanyBenefitConstants.EmploymentNameKey)
          .getOrFail
          .map { mandatoryJourneyValues =>
            BadRequest(
              removeCompanyBenefitStopDate(formWithErrors, mandatoryJourneyValues.head, mandatoryJourneyValues(1)))
          }
      }, {
        case Some(RemoveCompanyBenefitStopDateConstants.BeforeTaxYearEnd) =>
          for {
            current <- journeyCacheService.currentCache
            _       <- journeyCacheService.flush()
            filtered = current.filterKeys(_ != EndCompanyBenefitConstants.BenefitValueKey)
            _ <- journeyCacheService.cache(filtered ++ Map(
                  EndCompanyBenefitConstants.BenefitStopDateKey -> RemoveCompanyBenefitStopDateConstants.BeforeTaxYearEnd))
          } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber)

        case Some(RemoveCompanyBenefitStopDateConstants.OnOrAfterTaxYearEnd) =>
          journeyCacheService.cache(
            EndCompanyBenefitConstants.BenefitStopDateKey,
            RemoveCompanyBenefitStopDateConstants.OnOrAfterTaxYearEnd) map { _ =>
            Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit)
          }
      }
    )
  }

  def totalValueOfBenefit(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    val mandatoryKeys = Seq(EndCompanyBenefitConstants.EmploymentNameKey, EndCompanyBenefitConstants.BenefitNameKey)
    val optionalKeys = Seq(EndCompanyBenefitConstants.BenefitValueKey)

    journeyCacheService.collectedJourneyValues(mandatoryKeys, optionalKeys).getOrFail.map {
      case (mandatoryJourneyValues, optionalValues) =>
        val form = CompanyBenefitTotalValueForm.form.fill(optionalValues.head.getOrElse(""))
        Ok(removeBenefitTotalValue(BenefitViewModel(mandatoryJourneyValues.head, mandatoryJourneyValues(1)), form))
    }
  }

  def submitBenefitValue(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    CompanyBenefitTotalValueForm.form.bindFromRequest.fold(
      formWithErrors => {
        journeyCacheService
          .mandatoryJourneyValues(
            EndCompanyBenefitConstants.EmploymentNameKey,
            EndCompanyBenefitConstants.BenefitNameKey)
          .getOrFail
          .flatMap { mandatoryJourneyValues =>
            Future.successful(
              BadRequest(
                removeBenefitTotalValue(
                  BenefitViewModel(mandatoryJourneyValues.head, mandatoryJourneyValues(1)),
                  formWithErrors)))
          }
      },
      totalValue => {
        val rounded = BigDecimal(FormHelper.stripNumber(totalValue)).setScale(0, RoundingMode.UP)
        journeyCacheService
          .cache(Map(EndCompanyBenefitConstants.BenefitValueKey -> rounded.toString()))
          .map(_ => Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber))
      }
    )
  }

  def telephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val user = request.taiUser
    journeyCacheService.currentCache map { currentCache =>
      val telephoneNumberViewModel = extractViewModelFromCache(currentCache)
      val form = YesNoTextEntryForm
        .form()
        .fill(
          YesNoTextEntryForm(
            currentCache.get(EndCompanyBenefitConstants.TelephoneQuestionKey),
            currentCache.get(EndCompanyBenefitConstants.TelephoneNumberKey))
        )

      Ok(canWeContactByPhone(Some(user), telephoneNumberViewModel, form))
    }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    val user = request.taiUser
    YesNoTextEntryForm
      .form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),
        Some(telephoneNumberSizeConstraint))
      .bindFromRequest()
      .fold(
        formWithErrors => {
          journeyCacheService.currentCache map { currentCache =>
            val telephoneNumberViewModel = extractViewModelFromCache(currentCache)
            BadRequest(canWeContactByPhone(Some(user), telephoneNumberViewModel, formWithErrors))
          }
        },
        form => {
          val mandatoryData =
            Map(
              EndCompanyBenefitConstants.TelephoneQuestionKey -> form.yesNoChoice.getOrElse(
                FormValuesConstants.NoValue))

          val dataForCache = form.yesNoChoice match {
            case Some(FormValuesConstants.YesValue) =>
              mandatoryData ++ Map(EndCompanyBenefitConstants.TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(EndCompanyBenefitConstants.TelephoneNumberKey -> "")
          }

          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers)
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    journeyCacheService
      .collectedJourneyValues(
        Seq(
          EndCompanyBenefitConstants.EmploymentNameKey,
          EndCompanyBenefitConstants.BenefitNameKey,
          EndCompanyBenefitConstants.BenefitStopDateKey,
          EndCompanyBenefitConstants.TelephoneQuestionKey,
          EndCompanyBenefitConstants.RefererKey
        ),
        Seq(
          EndCompanyBenefitConstants.BenefitValueKey,
          EndCompanyBenefitConstants.TelephoneNumberKey
        )
      )
      .map {
        case Left(_) =>
          Redirect(controllers.routes.TaxAccountSummaryController.onPageLoad)
        case Right((mandatoryJourneyValues, optionalSeq)) =>
          val stopDate = {
            val startOfTaxYear = langUtils.Dates.formatDate(TaxYear().start)

            mandatoryJourneyValues(2) match {
              case RemoveCompanyBenefitStopDateConstants.OnOrAfterTaxYearEnd =>
                Messages("tai.remove.company.benefit.onOrAfterTaxYearEnd", startOfTaxYear)
              case RemoveCompanyBenefitStopDateConstants.BeforeTaxYearEnd =>
                Messages("tai.remove.company.benefit.beforeTaxYearEnd", startOfTaxYear)
            }
          }

          Ok(
            removeCompanyBenefitCheckYourAnswers(
              RemoveCompanyBenefitsCheckYourAnswersViewModel(
                mandatoryJourneyValues.head,
                mandatoryJourneyValues(1),
                stopDate,
                optionalSeq.head,
                mandatoryJourneyValues(3),
                optionalSeq(1))))
      }
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser

    for {
      (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService
                                                .collectedJourneyValues(
                                                  Seq(
                                                    EndCompanyBenefitConstants.EmploymentIdKey,
                                                    EndCompanyBenefitConstants.EmploymentNameKey,
                                                    EndCompanyBenefitConstants.BenefitTypeKey,
                                                    EndCompanyBenefitConstants.BenefitStopDateKey,
                                                    EndCompanyBenefitConstants.TelephoneQuestionKey
                                                  ),
                                                  Seq(
                                                    EndCompanyBenefitConstants.BenefitValueKey,
                                                    EndCompanyBenefitConstants.TelephoneNumberKey)
                                                )
                                                .getOrFail
      model = EndedCompanyBenefit(
        mandatoryCacheSeq(2),
        mandatoryCacheSeq(3),
        optionalCacheSeq.head,
        mandatoryCacheSeq(4),
        optionalCacheSeq(1))
      _ <- benefitsService.endedCompanyBenefit(user.nino, mandatoryCacheSeq.head.toInt, model)
      _ <- trackingJourneyCacheService.cache(TrackSuccessfulJourneyConstants.EndEmploymentBenefitKey, true.toString)
      _ <- journeyCacheService.flush
    } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.confirmation)
  }

  def cancel: Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    for {
      mandatoryJourneyValues <- journeyCacheService
                                 .mandatoryJourneyValues(EndCompanyBenefitConstants.RefererKey)
                                 .getOrFail
      _ <- journeyCacheService.flush
    } yield Redirect(mandatoryJourneyValues.head)
  }

  def confirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async { implicit request =>
    implicit val user: AuthedUser = request.taiUser
    Future.successful(Ok(removeCompanyBenefitConfirmation()))
  }

  private def extractViewModelFromCache(cache: Map[String, String])(implicit messages: Messages) = {
    val backUrl =
      if (cache.contains(EndCompanyBenefitConstants.BenefitValueKey)) {
        controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit.url
      } else {
        controllers.benefits.routes.RemoveCompanyBenefitController.stopDate.url
      }

    CanWeContactByPhoneViewModel(
      messages("tai.benefits.ended.journey.preHeader"),
      messages("tai.canWeContactByPhone.title"),
      backUrl,
      controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber.url,
      controllers.benefits.routes.RemoveCompanyBenefitController.cancel.url
    )
  }

}
