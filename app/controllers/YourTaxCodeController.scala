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

package controllers

import com.google.inject.Inject
import controllers.actions.DeceasedActionFilter
import controllers.auth.AuthAction
import play.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.service.{TaxAccountService, TaxCodeChangeService}
import uk.gov.hmrc.tai.viewModels.{TaxCodeViewModel, TaxCodeViewModelPreviousYears}

import scala.concurrent.Future
import scala.util.control.NonFatal

class YourTaxCodeController @Inject()(taxAccountService: TaxAccountService,
                                      taxCodeChangeService: TaxCodeChangeService,
                                      authenticate: AuthAction,
                                      filterDeceased: DeceasedActionFilter,
                                      override implicit val partialRetriever: FormPartialRetriever,
                                      override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with FeatureTogglesConfig {

  def taxCodes(year: TaxYear = TaxYear()): Action[AnyContent] = (authenticate andThen filterDeceased).async {
    implicit request =>
      val nino = request.taiUser.nino

      (for {
        TaiSuccessResponseWithPayload(taxCodeIncomes: Seq[TaxCodeIncome]) <- taxAccountService.taxCodeIncomes(nino, year)
        scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, year, taxCodeIncomes.map(_.taxCode))
      } yield {
        val taxCodeViewModel = TaxCodeViewModel.apply(taxCodeIncomes, scottishTaxRateBands)
        implicit val user = request.taiUser
        Ok(views.html.taxCodeDetails(taxCodeViewModel))
      }) recoverWith {
        case NonFatal(e) => {
          Logger.warn(s"Exception: ${e.getClass()}", e)
          errorPage
        }
      }
  }

  def prevTaxCodes(year: TaxYear): Action[AnyContent] = (authenticate andThen filterDeceased).async {
    implicit request =>
      if (taxCodeChangeEnabled) {

        val nino = request.taiUser.nino

        (for {
          taxCodeRecords <- taxCodeChangeService.lastTaxCodeRecordsInYearPerEmployment(nino, year)
          scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, year, taxCodeRecords.map(_.taxCode))
        } yield {
          val taxCodeViewModel = TaxCodeViewModelPreviousYears(taxCodeRecords, scottishTaxRateBands, year)
          implicit val user = request.taiUser
          Ok(views.html.taxCodeDetailsPreviousYears(taxCodeViewModel))
        }) recoverWith {
          case NonFatal(e) => {
            Logger.warn(s"Exception: ${e.getClass()}", e)
            errorPage
          }
        }
      } else {
        Future.successful(NotFound)
      }
  }

  private def errorPage()(implicit request: Request[_]) = {
    Future.successful(InternalServerError(error5xx(Messages("tai.technical.error.message"))))
  }
}
