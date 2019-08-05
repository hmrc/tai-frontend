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

package uk.gov.hmrc.tai.model.domain.income

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.concurrent.{ExecutionContext, Future}

final case class IncomeSource(id: Int, name: String)

object IncomeSource extends JourneyCacheConstants {
  def create(journeyCacheService: JourneyCacheService)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[IncomeSource] = {
    val idFuture = journeyCacheService.mandatoryValueAsInt(UpdateIncome_IdKey)
    val nameFuture = journeyCacheService.mandatoryValue(UpdateIncome_NameKey)
    for {
      id   <- idFuture
      name <- nameFuture
    } yield {
      IncomeSource(id, name)
    }
  }
}
