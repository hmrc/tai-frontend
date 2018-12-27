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

package controllers.pensions

import com.google.inject.Inject
import com.google.inject.name.Named
import controllers.auth.{TaiUser, WithAuthorisedForTaiLite}
import controllers.{ServiceCheckLite, TaiBaseController}
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.pensions.{UpdateRemovePensionForm, WhatDoYouWantToTellUsForm}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.model.domain.{IncorrectPensionProvider, PensionIncome}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel
import uk.gov.hmrc.tai.viewModels.pensions.update.UpdatePensionCheckYourAnswersViewModel

import scala.Function.tupled
import scala.concurrent.Future

class UpdatePensionProviderController @Inject()(val taxAccountService: TaxAccountService,
                                                pensionProviderService: PensionProviderService,
                                                val auditService: AuditService,
                                                val personService: PersonService,
                                                val delegationConnector: DelegationConnector,
                                                val authConnector: AuthConnector,
                                                @Named("Update Pension Provider") val journeyCacheService: JourneyCacheService,
                                                @Named("Successful Journey") val successfulJourneyCacheService: JourneyCacheService,
                                                override implicit val partialRetriever: FormPartialRetriever,
                                                override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with JourneyCacheConstants
  with FormValuesConstants {

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
    controllers.routes.IncomeSourceSummaryController.onPageLoad(pensionId).url
  )

  def doYouGetThisPension(id: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear()) flatMap {
            case TaiSuccessResponseWithPayload(incomes: Seq[TaxCodeIncome]) =>
              incomes.find(income => income.employmentId.contains(id) &&
                income.componentType == PensionIncome) match {
                case Some(taxCodeIncome) => cacheAndCreateView(id, taxCodeIncome)
                case _ => throw new RuntimeException(s"Tax code income source is not available for id $id")
              }
            case _ => throw new RuntimeException("Tax code income source is not available")
          }
        }
  }

  private def cacheAndCreateView(id: Int, taxCodeIncome: TaxCodeIncome)(implicit hc: HeaderCarrier,
                                                                        request: Request[AnyContent],
                                                                        user: TaiUser): Future[Result] = {
    for {
      updatedCache <- journeyCacheService.cache(Map(UpdatePensionProvider_IdKey -> id.toString,
        UpdatePensionProvider_NameKey -> taxCodeIncome.name))
    } yield {
      val model = PensionProviderViewModel(id, taxCodeIncome.name)
      val form = UpdateRemovePensionForm.form.fill(updatedCache.get(UpdatePensionProvider_ReceivePensionQuestionKey))
      Ok(views.html.pensions.update.doYouGetThisPensionIncome(model, form))
    }
  }

  def handleDoYouGetThisPension: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.mandatoryValues(UpdatePensionProvider_IdKey, UpdatePensionProvider_NameKey) flatMap { mandatoryVals =>
            UpdateRemovePensionForm.form.bindFromRequest().fold(
              formWithErrors => {
                val model = PensionProviderViewModel(mandatoryVals.head.toInt, mandatoryVals.last)
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
  }

  def whatDoYouWantToTellUs: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          for {
            (mandatoryValues, optionalValues) <- journeyCacheService.collectedValues(Seq(UpdatePensionProvider_NameKey), Seq(UpdatePensionProvider_DetailsKey))
          } yield {
            Ok(views.html.pensions.update.whatDoYouWantToTellUs(mandatoryValues(0),
              WhatDoYouWantToTellUsForm.form.fill(optionalValues(0).getOrElse(""))))
          }
        }
  }

  def submitWhatDoYouWantToTellUs: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          WhatDoYouWantToTellUsForm.form.bindFromRequest.fold(
            formWithErrors => {
              journeyCacheService.mandatoryValue(UpdatePensionProvider_NameKey) map { name =>
                BadRequest(views.html.pensions.update.whatDoYouWantToTellUs(name, formWithErrors))
              }
            },
            pensionDetails => {
              journeyCacheService.cache(Map(UpdatePensionProvider_DetailsKey -> pensionDetails))
                .map(_ => Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber()))
            }
          )
  }

  def addTelephoneNumber: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          for {
            pensionId <- journeyCacheService.mandatoryValueAsInt(UpdatePensionProvider_IdKey)
            telephoneCache <- journeyCacheService.optionalValues(UpdatePensionProvider_TelephoneQuestionKey, UpdatePensionProvider_TelephoneNumberKey)
          } yield {
            Ok(views.html.can_we_contact_by_phone(telephoneNumberViewModel(pensionId),
              YesNoTextEntryForm.form().fill(YesNoTextEntryForm(telephoneCache(0), telephoneCache(1)))))
          }
        }
  }

  def submitTelephoneNumber: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit person =>
      implicit request =>
        YesNoTextEntryForm.form(
          Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
          Messages("tai.canWeContactByPhone.telephone.empty"),
          Some(telephoneNumberSizeConstraint)).bindFromRequest().fold(
          formWithErrors => {
            journeyCacheService.currentCache map { currentCache =>
              BadRequest(views.html.can_we_contact_by_phone(telephoneNumberViewModel(currentCache(UpdatePensionProvider_IdKey).toInt), formWithErrors))
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

  def checkYourAnswers(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.collectedValues(
              Seq(
                UpdatePensionProvider_IdKey,
                UpdatePensionProvider_NameKey,
                UpdatePensionProvider_ReceivePensionQuestionKey,
                UpdatePensionProvider_DetailsKey,
                UpdatePensionProvider_TelephoneQuestionKey),
              Seq(UpdatePensionProvider_TelephoneNumberKey)
            ) map tupled { (mandatorySeq, optionalSeq) => {
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
  }

  def submitYourAnswers(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            for {
              (mandatoryCacheSeq, optionalCacheSeq) <-
                journeyCacheService.collectedValues(Seq(
                  UpdatePensionProvider_IdKey,
                  UpdatePensionProvider_DetailsKey,
                  UpdatePensionProvider_TelephoneQuestionKey),
                  Seq(UpdatePensionProvider_TelephoneNumberKey))
              model = IncorrectPensionProvider(mandatoryCacheSeq(1), mandatoryCacheSeq(2), optionalCacheSeq.head)
              _ <- pensionProviderService.incorrectPensionProvider(Nino(user.getNino), mandatoryCacheSeq.head.toInt, model)
              _ <- successfulJourneyCacheService.cache(TrackSuccessfulJourney_UpdatePensionKey, true.toString)
              _ <- journeyCacheService.flush
            } yield Redirect(controllers.pensions.routes.UpdatePensionProviderController.confirmation())
          }
  }

  def confirmation(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            Future.successful(Ok(views.html.pensions.update.confirmation()))
          }
  }
}
