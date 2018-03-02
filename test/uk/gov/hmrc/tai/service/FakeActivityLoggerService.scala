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

package testServices

import uk.gov.hmrc.tai.connectors.ActivityLoggerConnector
import uk.gov.hmrc.tai.service.ActivityLoggerService
import uk.gov.hmrc.domain.{Nino, TaxIds}

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class FakeActivityLoggerService extends ActivityLoggerService {

  def http: HttpResponse = HttpResponse(responseStatus = 200)
  override def activityLoggerConnector : ActivityLoggerConnector = ???

  override def updateIncome(nino: Nino)(implicit hc: HeaderCarrier) = {Future.successful(http)}
  override def viewIncome(nino: Nino)(implicit hc: HeaderCarrier) = {Future.successful(http)}

}

object FakeActivityLoggerService extends FakeActivityLoggerService

