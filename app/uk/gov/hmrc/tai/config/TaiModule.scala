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

package uk.gov.hmrc.tai.config

import controllers.actions.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, DataRetrievalActionImpl, IdentifierAction, SessionIdentifierAction}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.tai.service.journeyCache._

import java.time.{Clock, ZoneOffset}

class TaiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[DataRetrievalAction].to(classOf[DataRetrievalActionImpl]).eagerly(),
    bind[DataRequiredAction].to(classOf[DataRequiredActionImpl]).eagerly(),
    bind[IdentifierAction].to(classOf[SessionIdentifierAction]).eagerly(),
    bind[Clock].toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC)),

    // Journey Cache Services
    bind[JourneyCacheService].qualifiedWith("Add Employment").to(classOf[AddEmploymentJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Add Pension Provider").to(classOf[AddPensionProviderJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Company Car").to(classOf[CompanyCarJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("End Company Benefit").to(classOf[EndCompanyBenefitJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("End Employment").to(classOf[EndEmploymentJourneyCacheService]),
    bind[JourneyCacheService]
      .qualifiedWith("Track Successful Journey")
      .to(classOf[TrackSuccessfulJourneyJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Employment").to(classOf[UpdateEmploymentJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Income").to(classOf[UpdateIncomeJourneyCacheService]),
    bind[JourneyCacheService]
      .qualifiedWith("Update Next Years Income")
      .to(classOf[UpdateNextYearsIncomeJourneyCacheService]),
    bind[JourneyCacheService]
      .qualifiedWith("Update Pension Provider")
      .to(classOf[UpdatePensionProviderJourneyCacheService]),
    bind[JourneyCacheService]
      .qualifiedWith("Update Previous Years Income")
      .to(classOf[UpdatePreviousYearsIncomeJourneyCacheService])
  )
}
