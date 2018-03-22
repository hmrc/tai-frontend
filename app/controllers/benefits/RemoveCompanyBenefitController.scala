/*
 * Copyright 2018 HM Revenue & Customs
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

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import controllers.{AuthenticationConnectors, ServiceCheckLite, TaiBaseController}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.benefits.{CompanyBenefitTotalValueForm, RemoveCompanyBenefitStopDateForm}
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.model.domain.benefits.EndedCompanyBenefit
import uk.gov.hmrc.tai.service.benefits.BenefitsService
import uk.gov.hmrc.tai.service.{AuditService, JourneyCacheService, TaiService}
import uk.gov.hmrc.tai.util.{AuditConstants, FormHelper, FormValuesConstants, JourneyCacheConstants, _}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.benefit.{BenefitViewModel, RemoveCompanyBenefitCheckYourAnswersViewModel}
import uk.gov.hmrc.time.TaxYearResolver
import views.html.benefits.removeCompnanyBenefitCheckYourAnswers

import scala.Function.tupled
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

trait RemoveCompanyBenefitController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with AuditConstants
  with FormValuesConstants
  with RemoveCompanyBenefitStopDateConstants {

  def taiService: TaiService
  def auditService: AuditService
  def journeyCacheService: JourneyCacheService
  def trackingJourneyCacheService: JourneyCacheService
  def benefitsService: BenefitsService

  def stopDate: Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
                journeyCacheService.currentCache map { currentCache =>
                  Ok(views.html.benefits.removeCompanyBenefitStopDate(
                    RemoveCompanyBenefitStopDateForm.form,
                    currentCache(EndCompanyBenefit_BenefitNameKey),
                    currentCache(EndCompanyBenefit_EmploymentNameKey)))
                }
          }
  }

  def submitStopDate: Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>

          val startOfTaxYear = Dates.formatDate(TaxYearResolver.startOfCurrentTaxYear)

          RemoveCompanyBenefitStopDateForm.form.bindFromRequest.fold(
            formWithErrors => {
              journeyCacheService.mandatoryValues(EndCompanyBenefit_BenefitNameKey,EndCompanyBenefit_EmploymentNameKey) map  {
                mandatoryValues =>
                  BadRequest(views.html.benefits.removeCompanyBenefitStopDate(formWithErrors, mandatoryValues(0), mandatoryValues(1)))
              }

            },
            {
              case Some(BeforeTaxYearEnd) =>
                journeyCacheService.cache(Map(EndCompanyBenefit_BenefitStopDateKey ->
                  Messages("tai.remove.company.benefit.beforeTaxYearEnd",startOfTaxYear))) map { _ =>
                  Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.telephoneNumber())
                }
              case Some(OnOrAfterTaxYearEnd) =>
                journeyCacheService.cache(Map(EndCompanyBenefit_BenefitStopDateKey ->
                  Messages("tai.remove.company.benefit.onOrAfterTaxYearEnd",startOfTaxYear))) map { _ =>
                  Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.totalValueOfBenefit())
                }
            }
          )
  }


  def totalValueOfBenefit(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
              journeyCacheService.mandatoryValues(EndCompanyBenefit_EmploymentNameKey, EndCompanyBenefit_BenefitNameKey) flatMap  {
                mandatoryValues =>
                  Future.successful(Ok(views.html.benefits.
                    removeBenefitTotalValue(BenefitViewModel(mandatoryValues(0), mandatoryValues(1)), CompanyBenefitTotalValueForm.form)
                  ))
              }
            }
  }

  def submitBenefitValue(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          CompanyBenefitTotalValueForm.form.bindFromRequest.fold(
            formWithErrors => {
              journeyCacheService.mandatoryValues(EndCompanyBenefit_EmploymentNameKey, EndCompanyBenefit_BenefitNameKey) flatMap  {
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

  def telephoneNumber(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.currentCache map { currentCache =>
              val telephoneNumberViewModel = extractViewModelFromCache(currentCache)
              Ok(views.html.can_we_contact_by_phone(telephoneNumberViewModel, YesNoTextEntryForm.form()))
            }
          }
  }

  def submitTelephoneNumber(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          YesNoTextEntryForm.form(
            Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
            Messages("tai.canWeContactByPhone.telephone.empty"),

            Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
            formWithErrors => {
              journeyCacheService.currentCache map { currentCache =>
                val telephoneNumberViewModel = extractViewModelFromCache(currentCache)
                BadRequest(views.html.can_we_contact_by_phone(telephoneNumberViewModel, formWithErrors))
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

  def checkYourAnswers(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
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
              Ok(removeCompnanyBenefitCheckYourAnswers(
                RemoveCompanyBenefitCheckYourAnswersViewModel(
                  mandatorySeq(0),
                  mandatorySeq(1),
                  mandatorySeq(2),
                  optionalSeq(0),
                  mandatorySeq(3),
                  optionalSeq(1))))
              }
            }
          }
  }

  def submitYourAnswers(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
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
              _ <- benefitsService.endedCompanyBenefit(Nino(user.getNino), mandatoryCacheSeq.head.toInt , model)
              _ <- trackingJourneyCacheService.cache(TrackSuccessfulJourney_EndEmploymentBenefitKey, true.toString)
              _ <- journeyCacheService.flush
            } yield Redirect(controllers.benefits.routes.RemoveCompanyBenefitController.confirmation())
          }
  }

  def cancel: Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            for {
              mandatoryValues <- journeyCacheService.mandatoryValues(EndCompanyBenefit_RefererKey)
              _ <- journeyCacheService.flush
            }yield Redirect(mandatoryValues(0))
          }
  }

  def confirmation(): Action[AnyContent] = authorisedForTai(taiService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.benefits.removeCompanyBenefitConfirmation()))
          }
  }

  private def extractViewModelFromCache(cache: Map[String,String]) = {
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

object RemoveCompanyBenefitController extends RemoveCompanyBenefitController with AuthenticationConnectors {
  override val taiService: TaiService = TaiService
  override val auditService: AuditService = AuditService
  override val journeyCacheService: JourneyCacheService = JourneyCacheService(EndCompanyBenefit_JourneyKey)
  override val trackingJourneyCacheService: JourneyCacheService = JourneyCacheService(TrackSuccessfulJourney_JourneyKey)
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: PartialRetriever = TaiHtmlPartialRetriever
  override def benefitsService: BenefitsService = BenefitsService
}
