/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.auth.{AuthJourney, AuthedUser}
import play.api.mvc._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.service.EmploymentService
import uk.gov.hmrc.tai.viewModels.NoCYIncomeTaxErrorViewModel
import views.html.NoCYIncomeTaxErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NoCYIncomeTaxErrorController @Inject() (
  employmentService: EmploymentService,
  val auditConnector: AuditConnector,
  authenticate: AuthJourney,
  mcc: MessagesControllerComponents,
  noCYIncomeTaxErrorView: NoCYIncomeTaxErrorView
)(implicit ec: ExecutionContext)
    extends TaiBaseController(mcc) {

  def noCYIncomeTaxErrorPage(): Action[AnyContent] = authenticate.authWithValidatePerson.async { implicit request =>
    for {
      emp <- previousYearEmployments(request.taiUser.nino)
    } yield {
      implicit val user: AuthedUser = request.taiUser
      Ok(noCYIncomeTaxErrorView(NoCYIncomeTaxErrorViewModel(emp)))
    }
  }

  private def previousYearEmployments(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[Employment]] =
    employmentService.employments(nino, TaxYear().prev).value.map {
      case Right(employments) => employments
      case _                  => Nil
    }

}
