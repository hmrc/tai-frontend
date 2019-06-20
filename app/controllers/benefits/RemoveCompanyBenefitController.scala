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

package controllers.benefits

import com.google.inject.name.Named
import javax.inject.Inject
import controllers.TaiBaseController
import controllers.actions.ValidatePerson
import controllers.auth.AuthAction
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.benefits.{CompanyBenefitTotalValueForm, RemoveCompanyBenefitStopDateForm}
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.benefits.EndedCompanyBenefit
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.FormHelper
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants, RemoveCompanyBenefitStopDateConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.benefit.{BenefitViewModel, RemoveCompanyBenefitCheckYourAnswersViewModel}
import views.html.benefits.removeCompanyBenefitCheckYourAnswers

import scala.Function.tupled
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class RemoveCompanyBenefitController @Inject()(@Named("End Company Benefit") journeyCacheService: JourneyCacheService,
                                               @Named("Track Successful Journey") trackingJourneyCacheService: JourneyCacheService,
                                               benefitsService: BenefitsService,
                                               authenticate: AuthAction,
                                               validatePerson: ValidatePerson,
                                               implicit val templateRenderer: TemplateRenderer,
                                               implicit val partialRetriever: FormPartialRetriever)
  extends TaiBaseController
    with JourneyCacheConstants
    with FormValuesConstants
    with RemoveCompanyBenefitStopDateConstants {

  def stopDate: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      journeyCacheService.currentCache map { currentCache =>

        val form = RemoveCompanyBenefitStopDateForm.form.fill(currentCache.get(EndCompanyBenefit_BenefitStopDateKey))

        Ok(views.html.benefits.removeCompanyBenefitStopDate(
          form,
          currentCache(EndCompanyBenefit_BenefitNameKey),
          currentCache(EndCompanyBenefit_EmploymentNameKey)))
      }
  }

  def submitStopDate: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      RemoveCompanyBenefitStopDateForm.form.bindFromRequest.fold(
        formWithErrors => {
          journeyCacheService.mandatoryValues(EndCompanyBenefit_BenefitNameKey, EndCompanyBenefit_EmploymentNameKey) map {
            mandatoryValues =>
              BadRequest(views.html.benefits.removeCompanyBenefitStopDate(formWithErrors, mandatoryValues(0), mandatoryValues(1)))
          }
        },
        {
          case Some(BeforeTaxYearEnd) =>
            journeyCacheService.cache(EndCompanyBenefit_BenefitStopDateKey, BeforeTaxYearEnd) map { _ =>
              Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber())
            }
          case Some(OnOrAfterTaxYearEnd) =>
            journeyCacheService.cache(EndCompanyBenefit_BenefitStopDateKey, OnOrAfterTaxYearEnd) map { _ =>
              Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit())
            }
        }
      )
  }

  def totalValueOfBenefit(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      val mandatoryKeys = Seq(EndCompanyBenefit_EmploymentNameKey, EndCompanyBenefit_BenefitNameKey)
      val optionalKeys = Seq(EndCompanyBenefit_BenefitValueKey)

      journeyCacheService.collectedValues(mandatoryKeys, optionalKeys) flatMap
        tupled {
          (mandatoryValues, optionalValues) => {
            val form = CompanyBenefitTotalValueForm.form.fill((optionalValues(0)).getOrElse(""))

            Future.successful(Ok(views.html.benefits.
              removeBenefitTotalValue(BenefitViewModel(mandatoryValues(0), mandatoryValues(1)), form)
            ))
          }
        }
  }

  def submitBenefitValue(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      CompanyBenefitTotalValueForm.form.bindFromRequest.fold(
        formWithErrors => {
          journeyCacheService.mandatoryValues(EndCompanyBenefit_EmploymentNameKey, EndCompanyBenefit_BenefitNameKey) flatMap {
            mandatoryValues =>
              Future.successful(BadRequest(views.html.benefits.
                removeBenefitTotalValue(BenefitViewModel(mandatoryValues(0), mandatoryValues(1)), formWithErrors)
              ))
          }
        },
        totalValue => {
          val rounded = BigDecimal(FormHelper.stripNumber(totalValue)).setScale(0, RoundingMode.UP)
          journeyCacheService.cache(Map(EndCompanyBenefit_BenefitValueKey -> rounded.toString()))
            .map(_ => Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber()))
        }
      )
  }

  def telephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      val user = request.taiUser

      journeyCacheService.currentCache map { currentCache =>
        val telephoneNumberViewModel = extractViewModelFromCache(currentCache)
        val form = YesNoTextEntryForm.form().fill(
          YesNoTextEntryForm(currentCache.get(EndCompanyBenefit_TelephoneQuestionKey), currentCache.get(EndCompanyBenefit_TelephoneNumberKey))
        )

        Ok(views.html.can_we_contact_by_phone(Some(user), telephoneNumberViewModel, form))
      }
  }

  def submitTelephoneNumber(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      val user = request.taiUser

      YesNoTextEntryForm.form(
        Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
        Messages("tai.canWeContactByPhone.telephone.empty"),

        Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
        formWithErrors => {
          journeyCacheService.currentCache map { currentCache =>
            val telephoneNumberViewModel = extractViewModelFromCache(currentCache)
            BadRequest(views.html.can_we_contact_by_phone(Some(user), telephoneNumberViewModel, formWithErrors))
          }
        },
        form => {
          val mandatoryData = Map(EndCompanyBenefit_TelephoneQuestionKey -> form.yesNoChoice.getOrElse(NoValue))

          val dataForCache = form.yesNoChoice match {
            case Some(YesValue) => mandatoryData ++ Map(EndCompanyBenefit_TelephoneNumberKey -> form.yesNoTextEntry.getOrElse(""))
            case _ => mandatoryData ++ Map(EndCompanyBenefit_TelephoneNumberKey -> "")
          }

          journeyCacheService.cache(dataForCache) map { _ =>
            Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.checkYourAnswers())
          }
        }
      )
  }

  def checkYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      journeyCacheService.collectedValues(
        Seq(
          EndCompanyBenefit_EmploymentNameKey,
          EndCompanyBenefit_BenefitNameKey,
          EndCompanyBenefit_BenefitStopDateKey,
          EndCompanyBenefit_TelephoneQuestionKey,
          EndCompanyBenefit_RefererKey),
        Seq(
          EndCompanyBenefit_BenefitValueKey,
          EndCompanyBenefit_TelephoneNumberKey
        )) map tupled { (mandatorySeq, optionalSeq) => {


        val stopDate = {
          val startOfTaxYear = Dates.formatDate(TaxYear().start)

          mandatorySeq(2) match {
            case OnOrAfterTaxYearEnd => Messages("tai.remove.company.benefit.onOrAfterTaxYearEnd", startOfTaxYear)
            case BeforeTaxYearEnd=> Messages("tai.remove.company.benefit.beforeTaxYearEnd", startOfTaxYear)
          }
        }

        Ok(removeCompanyBenefitCheckYourAnswers(
          RemoveCompanyBenefitCheckYourAnswersViewModel(
            mandatorySeq(0),
            mandatorySeq(1),
            stopDate,
            optionalSeq(0),
            mandatorySeq(3),
            optionalSeq(1))))
      }
      }
  }

  def submitYourAnswers(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>

      implicit val user = request.taiUser

      for {
        (mandatoryCacheSeq, optionalCacheSeq) <- journeyCacheService.collectedValues(
          Seq(
            EndCompanyBenefit_EmploymentIdKey,
            EndCompanyBenefit_EmploymentNameKey,
            EndCompanyBenefit_BenefitTypeKey,
            EndCompanyBenefit_BenefitStopDateKey,
            EndCompanyBenefit_TelephoneQuestionKey),
          Seq(
            EndCompanyBenefit_BenefitValueKey,
            EndCompanyBenefit_TelephoneNumberKey))
        model = EndedCompanyBenefit(
          mandatoryCacheSeq(2),
          Messages("tai.noLongerGetBenefit"),
          mandatoryCacheSeq(3),
          optionalCacheSeq(0),
          mandatoryCacheSeq(4),
          optionalCacheSeq(1))
        _ <- benefitsService.endedCompanyBenefit(Nino(user.getNino), mandatoryCacheSeq.head.toInt, model)
        _ <- trackingJourneyCacheService.cache(TrackSuccessfulJourney_EndEmploymentBenefitKey, true.toString)
        _ <- journeyCacheService.flush
      } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.confirmation())
  }

  def cancel: Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      for {
        mandatoryValues <- journeyCacheService.mandatoryValues(EndCompanyBenefit_RefererKey)
        _ <- journeyCacheService.flush
      } yield Redirect(mandatoryValues(0))
  }

  def confirmation(): Action[AnyContent] = (authenticate andThen validatePerson).async {
    implicit request =>
      implicit val user = request.taiUser
      Future.successful(Ok(views.html.benefits.removeCompanyBenefitConfirmation()))
  }

  private def extractViewModelFromCache(cache: Map[String, String]) = {
    val backUrl =
      if (cache.contains(EndCompanyBenefit_BenefitValueKey)) {
        controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit().url
      } else {
        controllers.benefits.routes.RemoveCompanyBenefitController.stopDate.url
      }

    CanWeContactByPhoneViewModel(
      Messages("tai.benefits.ended.journey.preHeader"),
      Messages("tai.canWeContactByPhone.title"),
      backUrl,
      controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber().url,
      controllers.benefits.routes.RemoveCompanyBenefitController.cancel.url
    )
  }

}
