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

package controllers

import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiNoCompanyCarFoundResponse, TaiSuccessResponseWithPayload}
import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import uk.gov.hmrc.tai.forms.UpdateOrRemoveCarForm
import uk.gov.hmrc.tai.forms.benefits.DateForm
import uk.gov.hmrc.tai.viewModels.benefit.{CompanyCarCheckAnswersViewModel, CompanyCarChoiceViewModel}
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{JourneyCacheService, PersonService, SessionService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.{ApplicationConfig, FeatureTogglesConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.tai.util.ViewModelHelper._
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.concurrent.Future

trait CompanyCarController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with Auditable
  with JourneyCacheConstants
  with FeatureTogglesConfig{

  def personService: PersonService
  def companyCarService: CompanyCarService
  def journeyCacheService : JourneyCacheService
  def sessionService: SessionService

  def redirectCompanyCarSelection(employmentId: Int): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.cache(CompanyCar_EmployerIdKey, employmentId.toString) map {
             _ => Redirect(controllers.routes.CompanyCarController.getCompanyCarDetails())
            }
          }
  }

  def getCompanyCarDetails: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            for {
              empId <- companyCarService.companyCarEmploymentId
              response <- companyCarService.beginJourney(Nino(user.getNino), empId)
            } yield response match {
                case TaiSuccessResponseWithPayload(x: Map[String, String]) => Ok(views.html.benefits.updateCompanyCar(UpdateOrRemoveCarForm.createForm, CompanyCarChoiceViewModel(x)))
                case TaiNoCompanyCarFoundResponse(_) => Redirect(ApplicationConfig.companyCarServiceUrl)
            }
          }
  }

  def handleUserJourneyChoice: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>

          UpdateOrRemoveCarForm.createForm.bindFromRequest.fold(
            formWithErrors => {
              journeyCacheService.mandatoryValues(CompanyCar_CarModelKey, CompanyCar_CarProviderKey) flatMap { seq =>
                Future.successful(BadRequest(views.html.benefits.updateCompanyCar(formWithErrors, CompanyCarChoiceViewModel(seq(0), seq(1)))))
              }
            },
            formData => {
                formData.whatDoYouWantToDo match {
                  case Some("removeCar") if !companyCarForceRedirectEnabled =>
                    Future.successful(Redirect(controllers.routes.CompanyCarController.getCompanyCarEndDate()))
                  case _ =>
                    sessionService.invalidateCache() map (_ => Redirect(ApplicationConfig.companyCarServiceUrl))
                }
              }
          )
  }

  def getCompanyCarEndDate: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
              journeyCacheService.currentValueAsDate(CompanyCar_DateGivenBackKey) map {
                case Some(date) =>
                  Ok(views.html.benefits.companyCarEndDate(DateForm(Messages("tai.companyCar.endDate.blank")).form.fill(date)))
                case _ =>
                  Ok(views.html.benefits.companyCarEndDate(DateForm(Messages("tai.companyCar.endDate.blank")).form))
              }
          }
  }

  def handleCompanyCarEndDate: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          journeyCacheService.currentCache flatMap { cachedData =>
            val startDate = cachedData.get(CompanyCar_DateStartedKey)
            DateForm.verifyDate(DateForm(Messages("tai.companyCar.endDate.blank")).form.bindFromRequest, startDate).fold(

              formWithErrors => {
                Future.successful(BadRequest(views.html.benefits.companyCarEndDate(formWithErrors)))
              },

              formData => {
                journeyCacheService.cache(Map(CompanyCar_DateGivenBackKey -> formData.toString)) flatMap { res =>
                  cachedData.get(CompanyCar_HasActiveFuelBenefitdKey) match {
                    case Some(hasFuelBen) if isTrue(hasFuelBen) =>
                      Future.successful(Redirect(controllers.routes.CompanyCarController.getFuelBenefitEndDate()))
                    case _ =>
                      Future.successful(Redirect(controllers.routes.CompanyCarController.checkYourAnswers()))
                  }
                }
              }
            )
          }
  }

  def getFuelBenefitEndDate: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.currentValueAsDate(CompanyCar_DateFuelBenefitStoppedKey) map {
              case Some(date) =>
                Ok(views.html.benefits.fuelBenefitEndDate(DateForm(Messages("tai.companyCar.fuelBenefitEndDate.blank")).form.fill(date)))
              case _ =>
                Ok(views.html.benefits.fuelBenefitEndDate(DateForm(Messages("tai.companyCar.fuelBenefitEndDate.blank")).form))
            }
          }
  }

  def handleFuelBenefitEndDate: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          journeyCacheService.currentCache flatMap { cachedData =>
            val checkDate = cachedData.get(CompanyCar_DateFuelBenefitStartedKey).orElse(cachedData.get(CompanyCar_DateStartedKey))
            DateForm.verifyDate(DateForm(Messages("tai.companyCar.fuelBenefitEndDate.blank")).form.bindFromRequest, checkDate).fold(
              formWithErrors => {
                Future.successful(BadRequest(views.html.benefits.fuelBenefitEndDate((formWithErrors))))
              },
              formData => {
                journeyCacheService.cache(CompanyCar_DateFuelBenefitStoppedKey, formData.toString) flatMap { _ =>
                  Future.successful(Redirect(controllers.routes.CompanyCarController.checkYourAnswers()))
                }
              }
            )
          }
  }

  def checkYourAnswers: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {
            journeyCacheService.currentCache.map{cache=>
              val viewModel = CompanyCarCheckAnswersViewModel(cache, TaxYear(TaxYearResolver.currentTaxYear))
              Ok(views.html.benefits.companyCarCheckYourAnswers(viewModel))
            }
          }
  }

  def handleCheckYourAnswers: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          companyCarService.withdrawCompanyCarAndFuel(Nino(user.getNino), request.headers.get("Referer").getOrElse("NA")).map{_ =>
            Redirect(controllers.routes.CompanyCarController.confirmation())
          }
  }

  def confirmation: Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit person =>
        implicit request =>
          ServiceCheckLite.personDetailsCheck {

            Future.successful(Ok(views.html.benefits.companyCarConfirmation()))
          }
  }

}
// $COVERAGE-OFF$
object CompanyCarController extends CompanyCarController with AuthenticationConnectors {

  override val personService: PersonService = PersonService
  override val companyCarService: CompanyCarService = CompanyCarService
  override val sessionService: SessionService = SessionService
  override val journeyCacheService : JourneyCacheService = JourneyCacheService(CompanyCar_JourneyKey)
  override implicit val templateRenderer: TemplateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}
// $COVERAGE-ON$