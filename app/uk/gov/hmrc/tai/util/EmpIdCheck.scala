/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

import com.google.inject.Inject
import controllers.auth.DataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{MessagesControllerComponents, Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.EmploymentService
import views.html.IdNotFound

import scala.concurrent.{ExecutionContext, Future}

class EmpIdCheck @Inject (
  employmentsService: EmploymentService,
  idNotFound: IdNotFound,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends Results
    with I18nSupport {

  override def messagesApi: MessagesApi = mcc.messagesApi

  def checkValidId(empId: Int, taxYear: TaxYear = TaxYear())(implicit
    request: DataRequest[_]
  ): Future[Option[Result]] = {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    employmentsService
      .employments(request.taiUser.nino, taxYear)
      .map { employments =>
        if (employments.exists(_.sequenceNumber == empId)) {
          None
        } else {
          Some(NotFound(idNotFound()))
        }
      }
  }
}
