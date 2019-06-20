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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants._
import uk.gov.hmrc.tai.util.constants.BankAccountDecisionConstants._
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

import scala.concurrent.{ExecutionContext, Future}

class AddEmploymentJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(AddEmployment_JourneyKey, journeyCacheConnector)
class AddPensionProviderJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(AddPensionProvider_JourneyKey, journeyCacheConnector)
class CloseBankAccountJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(CloseBankAccountJourneyKey, journeyCacheConnector)
class CompanyCarJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(CompanyCar_JourneyKey, journeyCacheConnector)
class EndCompanyBenefitJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(EndCompanyBenefit_JourneyKey, journeyCacheConnector)
class EndEmploymentJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(EndEmployment_JourneyKey, journeyCacheConnector)
class TrackSuccessfulJourneyJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(TrackSuccessfulJourney_JourneyKey, journeyCacheConnector)
class UpdateBankAccountJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(UpdateBankAccountJourneyKey, journeyCacheConnector)
class UpdateBankAccountChoiceJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(UpdateBankAccountChoiceJourneyKey, journeyCacheConnector)
class UpdateEmploymentJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(UpdateEmployment_JourneyKey, journeyCacheConnector)
class UpdateNextYearsIncomeJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(UpdateNextYearsIncomeConstants.JOURNEY_KEY, journeyCacheConnector)
class UpdatePensionProviderJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(UpdatePensionProvider_JourneyKey, journeyCacheConnector)
class UpdatePreviousYearsIncomeJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(UpdatePreviousYearsIncome_JourneyKey, journeyCacheConnector)
class UpdateIncomeJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector) extends JourneyCacheService(UpdateIncome_JourneyKey, journeyCacheConnector)
