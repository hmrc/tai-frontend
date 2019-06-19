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

package uk.gov.hmrc.tai.connectors

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.model.domain.IncorrectIncome

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PreviousYearsIncomeConnector @Inject() (httpHandler: HttpHandler) extends DefaultServicesConfig {

  val serviceUrl: String = baseUrl("tai")

  def previousYearsIncomeServiceUrl(nino: Nino, year: Int) = s"$serviceUrl/tai/$nino/employments/years/$year/update"

  def incorrectIncome(nino: Nino, year: Int, incorrectIncome: IncorrectIncome)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpHandler.postToApi[IncorrectIncome](previousYearsIncomeServiceUrl(nino, year), incorrectIncome).map { response =>
      (response.json \ "data").asOpt[String]
    }
  }

}
