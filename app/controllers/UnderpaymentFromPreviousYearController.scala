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
import play.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.tax.TotalTax
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.viewModels.PreviousYearUnderpaymentViewModel
import views.html.previousYearUnderpayment

import scala.concurrent.Future
import scala.util.control.NonFatal


class UnderpaymentFromPreviousYearController @Inject()(codingComponentService: CodingComponentService,
                                                       employmentService: EmploymentService,
                                                       companyCarService: CompanyCarService,
                                                       taxAccountService: TaxAccountService,
                                                       authenticate: AuthAction,
                                                       override implicit val partialRetriever: FormPartialRetriever,
                                                       override implicit val templateRenderer: TemplateRenderer) extends TaiBaseController {

  def underpaymentExplanation = authenticate.async {
    implicit request =>

      val nino = request.taiUser.nino
      val year = TaxYear()
      val totalTaxFuture = taxAccountService.totalTax(nino, year)
      val employmentsFuture = employmentService.employments(nino, year.prev)
      val codingComponentsFuture = codingComponentService.taxFreeAmountComponents(nino, year)

      val view = for {
        employments <- employmentsFuture
        codingComponents <- codingComponentsFuture
        totalTax <- totalTaxFuture
      } yield {
        totalTax match {
          case TaiSuccessResponseWithPayload(totalTax: TotalTax) =>
            implicit val user = request.taiUser
            Ok(previousYearUnderpayment(PreviousYearUnderpaymentViewModel(codingComponents, employments, totalTax)))
          case _ => throw new RuntimeException("Failed to fetch total tax details")
        }
      }

      view.recoverWith {
        case NonFatal(e) => {
          Logger.warn(s"Exception: ${e.getClass()}", e)
          Future.successful(InternalServerError(error5xx(Messages("tai.technical.error.message"))))
        }
      }
  }
}
