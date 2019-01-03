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

package uk.gov.hmrc.tai.service.journeyCache

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants._
import uk.gov.hmrc.tai.util.constants.BankAccountDecisionConstants._
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

import scala.concurrent.{ExecutionContext, Future}

class AddEmploymentJourneyCacheService extends JourneyCacheService(AddEmployment_JourneyKey, JourneyCacheConnector)
class AddPensionProviderJourneyCacheService extends JourneyCacheService(AddPensionProvider_JourneyKey, JourneyCacheConnector)
class CloseBankAccountJourneyCacheService extends JourneyCacheService(CloseBankAccountJourneyKey, JourneyCacheConnector)
class CompanyCarJourneyCacheService extends JourneyCacheService(CompanyCar_JourneyKey, JourneyCacheConnector)
class EndCompanyBenefitJourneyCacheService extends JourneyCacheService(EndCompanyBenefit_JourneyKey, JourneyCacheConnector)
class EndEmploymentJourneyCacheService extends JourneyCacheService(EndEmployment_JourneyKey, JourneyCacheConnector)
class TrackSuccessfulJourneyJourneyCacheService extends JourneyCacheService(TrackSuccessfulJourney_JourneyKey, JourneyCacheConnector)
class UpdateBankAccountJourneyCacheService extends JourneyCacheService(UpdateBankAccountJourneyKey, JourneyCacheConnector)
class UpdateBankAccountChoiceJourneyCacheService extends JourneyCacheService(UpdateBankAccountChoiceJourneyKey, JourneyCacheConnector)
class UpdateEmploymentJourneyCacheService extends JourneyCacheService(UpdateEmployment_JourneyKey, JourneyCacheConnector)
class UpdateNextYearsIncomeJourneyCacheService extends JourneyCacheService(UpdateNextYearsIncomeConstants.JOURNEY_KEY, JourneyCacheConnector)
class UpdatePensionProviderJourneyCacheService extends JourneyCacheService(UpdatePensionProvider_JourneyKey, JourneyCacheConnector)
class UpdatePreviousYearsIncomeJourneyCacheService extends JourneyCacheService(UpdatePreviousYearsIncome_JourneyKey, JourneyCacheConnector)
class UpdateIncomeJourneyCacheService extends JourneyCacheService(UpdateIncome_JourneyKey, JourneyCacheConnector)

class UpdatedEstimatedPayJourneyCacheService @Inject()(journeyCacheService: JourneyCacheService)
  extends JourneyCacheService(UpdateIncome_JourneyKey, JourneyCacheConnector) {

  def journeyCache(key: String = "defaultCacheUpdate", cacheMap: Map[String, String])
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Map[String, String]] = {

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
