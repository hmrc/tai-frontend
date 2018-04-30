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

import controllers.auth.WithAuthorisedForTaiLite
import controllers.{AuthenticationConnectors, ServiceCheckLite, TaiBaseController}
import play.api.Play.current
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{ApplicationConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.pensions.{UpdateRemovePensionForm, WhatDoYouWantToTellUsForm}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.PensionIncome
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.{JourneyCacheService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.util.{FormValuesConstants, JourneyCacheConstants}
import uk.gov.hmrc.tai.viewModels.CanWeContactByPhoneViewModel
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel

import scala.concurrent.Future

trait UpdatePensionProviderController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with JourneyCacheConstants
  with FormValuesConstants {

  def personService: PersonService

  def taxAccountService: TaxAccountService

  def journeyCacheService: JourneyCacheService

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
    implicit taiRoot =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear()) flatMap {
            case TaiSuccessResponseWithPayload(incomes: Seq[TaxCodeIncome]) =>
              incomes.find(income => income.employmentId.contains(id) &&
                income.componentType == PensionIncome) match {
                case Some(taxCodeIncome) =>
                  journeyCacheService.cache(Map(UpdatePensionProvider_IdKey -> id.toString,
                    UpdatePensionProvider_NameKey -> taxCodeIncome.name)).
                    map {
                      val model = PensionProviderViewModel(id, taxCodeIncome.name)
                      _ => Ok(views.html.pensions.doYouGetThisPensionIncome(model, UpdateRemovePensionForm.form))
                    }
                case _ => throw new RuntimeException(s"Tax code income source is not available for id $id")
              }
            case _ => throw new RuntimeException("Tax code income source is not available")
          }
        }
  }

  def handleDoYouGetThisPension: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.mandatoryValues(UpdatePensionProvider_IdKey, UpdatePensionProvider_NameKey) flatMap { mandatoryVals =>
            UpdateRemovePensionForm.form.bindFromRequest().fold(
              formWithErrors => {
                val model = PensionProviderViewModel(mandatoryVals.head.toInt, mandatoryVals.last)
                Future(BadRequest(views.html.pensions.doYouGetThisPensionIncome(model, formWithErrors)))
              },
              {
                case Some(YesValue) => Future.successful(
                  Redirect(controllers.pensions.routes.UpdatePensionProviderController.whatDoYouWantToTellUs()))
                case _ => Future.successful(Redirect(ApplicationConfig.incomeFromEmploymentPensionLinkUrl))
              }
            )

          }
        }
  }

  def whatDoYouWantToTellUs: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.mandatoryValue(UpdatePensionProvider_NameKey) flatMap { name =>
            Future.successful(Ok(views.html.pensions.update.whatDoYouWantToTellUs(name, WhatDoYouWantToTellUsForm.form)))
          }
        }
  }


  def submitWhatDoYouWantToTellUs: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request =>
          WhatDoYouWantToTellUsForm.form.bindFromRequest.fold(
            formWithErrors => {
              journeyCacheService.mandatoryValue(UpdatePensionProvider_NameKey) map { name =>
                BadRequest(views.html.pensions.update.whatDoYouWantToTellUs(name, formWithErrors))
              }
            },
            pensionDetails => {
              journeyCacheService.cache(Map(IncorrectPensionProvider_DetailsKey -> pensionDetails))
                .map(_ => Redirect(controllers.pensions.routes.UpdatePensionProviderController.addTelephoneNumber()))
            }
          )
  }

  def addTelephoneNumber: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.mandatoryValueAsInt(UpdatePensionProvider_IdKey) map { id =>
            Ok(views.html.can_we_contact_by_phone(telephoneNumberViewModel(id), YesNoTextEntryForm.form()))
          }
        }
  }

  def submitTelephoneNumber: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit taiRoot =>
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

  def checkYourAnswers: Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          Future.successful(Ok("TODO"))
        }
  }

}

object UpdatePensionProviderController extends UpdatePensionProviderController with AuthenticationConnectors {
  override val personService: PersonService = PersonService
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
  override val journeyCacheService = JourneyCacheService(UpdatePensionProvider_JourneyKey)
  override val taxAccountService: TaxAccountService = TaxAccountService
}