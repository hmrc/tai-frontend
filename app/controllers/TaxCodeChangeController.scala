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
import controllers.auth.AuthAction
import javax.inject.Singleton
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.config.FeatureTogglesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.viewModels.taxCodeChange.YourTaxFreeAmountViewModel
import uk.gov.hmrc.urls.Link

import scala.concurrent.Future

object ConnectorWithHttpValues {
  val http = new WSGet with HttpGet with WSPut with HttpPut with WSPost with HttpPost with WSDelete with HttpDelete with WSPatch with HttpPatch with HttpHooks {
    val hooks = NoneRequired
  }
}

@Singleton
class AuthClientAuthConnector extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")

  override def http: CorePost = ConnectorWithHttpValues.http
}

class TaxCodeChangeController @Inject()(val personService: PersonService,
                                        val codingComponentService: CodingComponentService,
                                        val employmentService: EmploymentService,
                                        val companyCarService: CompanyCarService,
                                        val taxCodeChangeService: TaxCodeChangeService,
                                        val taxAccountService: TaxAccountService,
                                        authenticate: AuthAction,
                                        override implicit val partialRetriever: FormPartialRetriever,
                                        override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController
  with FeatureTogglesConfig {

  def taxCodeComparison: Action[AnyContent] = (authenticate) {
    implicit request => Ok(notFoundView())

    //    implicit request =>
    //      if (taxCodeChangeEnabled) {
    //        val nino = request.taiUser.nino
    //
    //        for {
    //          taxCodeChange <- taxCodeChangeService.taxCodeChange(nino)
    //          scottishTaxRateBands <- taxAccountService.scottishBandRates(nino, TaxYear(), taxCodeChange.uniqueTaxCodes)
    //        } yield {
    //          val viewModel = TaxCodeChangeViewModel(taxCodeChange, scottishTaxRateBands)
    //          Ok(views.html.taxCodeChange.taxCodeComparison(viewModel, request.taiUser))
    //        }
    //      } else {
    //        Ok(notFoundView)
    //      }
  }

  def yourTaxFreeAmount: Action[AnyContent] = authenticate.async {
    implicit request =>
      if (taxCodeChangeEnabled) {
        val nino = request.taiUser.nino
        val employmentNameFuture = employmentService.employmentNames(nino, TaxYear())
        val taxCodeChangeFuture = taxCodeChangeService.taxCodeChange(nino)
        val codingComponentsFuture = codingComponentService.taxFreeAmountComponents(nino, TaxYear())

        for {
          employmentNames <- employmentNameFuture
          taxCodeChange <- taxCodeChangeFuture
          codingComponents <- codingComponentsFuture
          companyCarBenefits <- companyCarService.companyCarOnCodingComponents(nino, codingComponents)
        } yield {
          val viewModel = YourTaxFreeAmountViewModel(taxCodeChange.mostRecentTaxCodeChangeDate, codingComponents, employmentNames, companyCarBenefits)
          Ok(views.html.taxCodeChange.yourTaxFreeAmount(viewModel, request.taiUser))
        }

      } else {
        Future.successful(Ok(notFoundView("taxCodeChangeEnabled disabled")))
      }
  }


  def whatHappensNext: Action[AnyContent] = (authenticate) {
    implicit request =>
      if (taxCodeChangeEnabled) {
        Ok(views.html.taxCodeChange.whatHappensNext(request.taiUser))
      } else {
        Ok(notFoundView())
      }
    //    implicit request =>
    //      if (taxCodeChangeEnabled) {
    //        ServiceCheckLite.personDetailsCheck {
    //          Future.successful(Ok(views.html.taxCodeChange.whatHappensNext()))
    //        }
    //      }
    //      else {
    //        ServiceCheckLite.personDetailsCheck {
    //          Future.successful(Ok(notFoundView))
    //        }
    //      }
  }


  private def notFoundView(message: String = "")(implicit request: Request[_])
  = views.html.error_template_noauth(Messages("global.error.pageNotFound404.title"),
    Messages("tai.errorMessage.heading"),
    Messages("tai.errorMessage.frontend404", Link.toInternalPage(
      url = routes.TaxAccountSummaryController.onPageLoad().url,
      value = Some(Messages("tai.errorMessage.startAgain") + message)
    ).toHtml))

}
