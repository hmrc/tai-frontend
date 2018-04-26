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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.{ApplicationConfig, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.domain.PensionIncome
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.{JourneyCacheService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.util.{FormValuesConstants, JourneyCacheConstants}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.tai.forms.pensions.UpdateRemovePensionForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.viewModels.pensions.PensionProviderViewModel

import scala.concurrent.Future

trait IncorrectPensionProviderController extends TaiBaseController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with JourneyCacheConstants
  with FormValuesConstants {

  def personService: PersonService
  def taxAccountService: TaxAccountService
  def journeyCacheService: JourneyCacheService

  def decision(id: Int): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          taxAccountService.taxCodeIncomes(Nino(user.getNino), TaxYear()) flatMap {
            case TaiSuccessResponseWithPayload(incomes: Seq[TaxCodeIncome]) =>
              incomes.find( income => income.employmentId.contains(id) &&
                income.componentType == PensionIncome) match {
                case Some(taxCodeIncome) =>
                  journeyCacheService.cache(Map(IncorrectPensionProvider_IdKey -> id.toString,
                    IncorrectPensionProvider_NameKey -> taxCodeIncome.name)).
                    map {
                      val model = PensionProviderViewModel(id, taxCodeIncome.name)
                      _ => Ok(views.html.pensions.incorrectPensionDecision(model, UpdateRemovePensionForm.form))
                    }
                case _ => throw new RuntimeException(s"Tax code income source is not available for id $id")
              }
            case _ => throw new RuntimeException("Tax code income source is not available")
          }
        }

  }

  def handleDecision(): Action[AnyContent] = authorisedForTai(personService).async { implicit user =>
    implicit taiRoot =>
      implicit request =>
        ServiceCheckLite.personDetailsCheck {
          journeyCacheService.mandatoryValues(IncorrectPensionProvider_NameKey, IncorrectPensionProvider_IdKey) flatMap { mandatoryVals =>
            UpdateRemovePensionForm.form.bindFromRequest().fold(
              formWithErrors => {
                val model = PensionProviderViewModel(mandatoryVals.head.toInt, mandatoryVals.last)
                Future(BadRequest(views.html.pensions.incorrectPensionDecision(model, formWithErrors)))
              },
              {
                case Some(YesValue) => Future.successful(Ok(""))
                case _ => Future.successful(Redirect(ApplicationConfig.incomeFromEmploymentPensionLinkUrl))
              }
            )

          }
        }
  }
}

object IncorrectPensionProviderController extends IncorrectPensionProviderController with AuthenticationConnectors {
  override val personService: PersonService = PersonService
  override implicit val templateRenderer = LocalTemplateRenderer
  override implicit val partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
  override val journeyCacheService = JourneyCacheService(IncorrectPensionProvider_JourneyKey)
  override val taxAccountService: TaxAccountService = TaxAccountService
}