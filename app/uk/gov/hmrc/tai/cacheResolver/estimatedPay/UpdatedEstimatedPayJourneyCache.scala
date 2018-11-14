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

package uk.gov.hmrc.tai.cacheResolver.estimatedPay

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.service.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.concurrent.{ExecutionContext, Future}

trait UpdatedEstimatedPayJourneyCache extends JourneyCacheConstants{

  def journeyCache(key: String = "defaultCacheUpdate", cacheMap: Map[String, String])
                  (implicit hc: HeaderCarrier, ec: ExecutionContext, journeyCacheService: JourneyCacheService): Future[Map[String, String]] = {

    def yesNoAnswerResponse(cacheToUpdate: Map[String,String],keysToEmpty: List[String]) = {
      if (cacheMap(key) == "Yes") journeyCacheService.cache(cacheMap) else updateStaleCache(cacheToUpdate,keysToEmpty)
    }

    def updateStaleCache(cacheToUpdate: Map[String,String],keysToEmpty: List[String])(implicit hc: HeaderCarrier): Future[Map[String, String]] = {

      for{
        current <- journeyCacheService.currentCache
        updatedCacheMap = current.filterKeys( key => !keysToEmpty.contains(key)) ++ cacheToUpdate
        _ <- journeyCacheService.flush()
        updatedCache <- journeyCacheService.cache(updatedCacheMap)
      } yield updatedCache

    }

    key match {
      case UpdateIncome_PayslipDeductionsKey => yesNoAnswerResponse(Map(key -> cacheMap(key)), List(UpdateIncome_TaxablePayKey))
      case UpdateIncome_BonusPaymentsKey => yesNoAnswerResponse(Map(key -> cacheMap(key)), List(UpdateIncome_BonusOvertimeAmountKey))
      case _ => journeyCacheService.cache(cacheMap)
    }
  }
}