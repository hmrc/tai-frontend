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

package uk.gov.hmrc.tai.config

import com.google.inject.AbstractModule
import controllers.AuthClientAuthConnector
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors._
import uk.gov.hmrc.tai.service.journeyCache._

class TaiAuthModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[core.AuthConnector]).to(classOf[AuthClientAuthConnector])
  }
}

class TaiModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[FormPartialRetriever].toInstance(TaiHtmlPartialRetriever),
    bind[TemplateRenderer].toInstance(LocalTemplateRenderer),
    bind[WSHttpProxy].toInstance(WSHttpProxy),
    // Connectors
    bind[AuditConnector].toInstance(AuditConnector),
    bind[uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector].toInstance(FrontendAuthConnector),
    bind[DelegationConnector].toInstance(FrontEndDelegationConnector),
    bind[HttpHandler].toInstance(HttpHandler),
    bind[PensionProviderConnector].toInstance(PensionProviderConnector),
    bind[PersonConnector].toInstance(PersonConnector),
    bind[PreviousYearsIncomeConnector].toInstance(PreviousYearsIncomeConnector),
    bind[SessionConnector].toInstance(SessionConnector),
    bind[UserDetailsConnector].toInstance(UserDetailsConnector),
    bind[TaiConnector].toInstance(TaiConnector),
    bind[TaxAccountConnector].toInstance(TaxAccountConnector),
    bind[TaxCodeChangeConnector].toInstance(TaxCodeChangeConnector),
    bind[TrackingConnector].toInstance(TrackingConnector),
    // Journey Cache Services
    bind[JourneyCacheService].qualifiedWith("Add Employment").to(classOf[AddEmploymentJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Add Pension Provider").to(classOf[AddPensionProviderJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Close Bank Account").to(classOf[CloseBankAccountJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Company Car").to(classOf[CompanyCarJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("End Company Benefit").to(classOf[EndCompanyBenefitJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("End Employment").to(classOf[EndEmploymentJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Track Successful Journey").to(classOf[TrackSuccessfulJourneyJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Bank Account").to(classOf[UpdateBankAccountJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Bank Account Choice").to(classOf[UpdateBankAccountChoiceJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Employment").to(classOf[UpdateEmploymentJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Income").to(classOf[UpdateIncomeJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Next Years Income").to(classOf[UpdateNextYearsIncomeJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Pension Provider").to(classOf[UpdatePensionProviderJourneyCacheService]),
    bind[JourneyCacheService].qualifiedWith("Update Previous Years Income").to(classOf[UpdatePreviousYearsIncomeJourneyCacheService])
  )
}
