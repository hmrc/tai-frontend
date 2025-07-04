/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tai.service

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.PreviousYearsIncomeConnector
import uk.gov.hmrc.tai.model.domain.IncorrectIncome

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousYearsIncomeService @Inject() (previousYearsIncomeConnector: PreviousYearsIncomeConnector) {

  def incorrectIncome(nino: Nino, id: Int, incorrectIncome: IncorrectIncome)(implicit
    hc: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[String] =
    previousYearsIncomeConnector.incorrectIncome(nino, id, incorrectIncome) map {
      case Some(envId) => envId
      case _           =>
        throw new RuntimeException(
          s"No envelope id was generated when sending previous years income details for ${nino.nino}"
        )
    }

}
