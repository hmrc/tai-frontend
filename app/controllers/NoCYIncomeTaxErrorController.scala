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

import controllers.audit.Auditable
import controllers.auth.WithAuthorisedForTaiLite
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.DelegationAwareActions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.TaiHtmlPartialRetriever
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService}
import uk.gov.hmrc.tai.viewModels.NoCYIncomeTaxErrorViewModel
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future


trait NoCYIncomeTaxErrorController extends FrontendController
  with DelegationAwareActions
  with WithAuthorisedForTaiLite
  with ErrorPagesHandler
  with Auditable {

  def personService: PersonService

  def employmentService: EmploymentService

  def noCYIncomeTaxErrorPage(): Action[AnyContent] = authorisedForTai(personService).async {
    implicit user =>
      implicit taiRoot =>
        implicit request => {
          ServiceCheckLite.personDetailsCheck {
            for {
              emp <- previousYearEmployments(Nino(user.getNino))
            } yield {
              Ok(views.html.noCYIncomeTaxErrorPage(NoCYIncomeTaxErrorViewModel(emp)))
            }
          }
        }
  }

  private def previousYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[Employment]] = {
    employmentService.employments(nino, TaxYear(TaxYearResolver.currentTaxYear - 1)) recover {
      case _ => Nil
    }
  }

}

object NoCYIncomeTaxErrorController extends NoCYIncomeTaxErrorController with AuthenticationConnectors {
  override val personService = PersonService
  override val employmentService = EmploymentService

  override implicit def templateRenderer = LocalTemplateRenderer

  override implicit def partialRetriever: FormPartialRetriever = TaiHtmlPartialRetriever
}
