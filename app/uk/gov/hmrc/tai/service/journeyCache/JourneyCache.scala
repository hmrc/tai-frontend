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

package uk.gov.hmrc.tai.service.journeyCache

import uk.gov.hmrc.tai.connectors.JourneyCacheConnector
import uk.gov.hmrc.tai.util.constants.journeyCache._

import javax.inject.Inject

class AddEmploymentJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(AddEmploymentConstants.JourneyKey, journeyCacheConnector)
class AddPensionProviderJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(AddPensionProviderConstants.JourneyKey, journeyCacheConnector)
class CompanyCarJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(CompanyCarConstants.JourneyKey, journeyCacheConnector)
class EndCompanyBenefitJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(EndCompanyBenefitConstants.JourneyKey, journeyCacheConnector)
class EndEmploymentJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(EndEmploymentConstants.JourneyKey, journeyCacheConnector)
class TrackSuccessfulJourneyJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(TrackSuccessfulJourneyConstants.JourneyKey, journeyCacheConnector)
class UpdateEmploymentJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(UpdateEmploymentConstants.JourneyKey, journeyCacheConnector)
class UpdateNextYearsIncomeJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(UpdateNextYearsIncomeConstants.JourneyKey, journeyCacheConnector)
class UpdatePensionProviderJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(UpdatePensionProviderConstants.JourneyKey, journeyCacheConnector)
class UpdatePreviousYearsIncomeJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(UpdatePreviousYearsIncomeConstants.JourneyKey, journeyCacheConnector)
class UpdateIncomeJourneyCacheService @Inject()(journeyCacheConnector: JourneyCacheConnector)
    extends JourneyCacheService(UpdateIncomeConstants.JourneyKey, journeyCacheConnector)
