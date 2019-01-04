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

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.{LocalTemplateRenderer, UserDetailsConnector}
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.service.benefits.{BenefitsService, CompanyCarService}
import uk.gov.hmrc.tai.util.constants.{BankAccountDecisionConstants, JourneyCacheConstants}

class TaiModule extends Module with JourneyCacheConstants with BankAccountDecisionConstants {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[FormPartialRetriever].toInstance(TaiHtmlPartialRetriever),
    bind[TemplateRenderer].toInstance(LocalTemplateRenderer),
    bind[WSHttpProxy].toInstance(WSHttpProxy),
    // Connectors
    bind[AuditConnector].toInstance(AuditConnector),
    bind[AuthConnector].toInstance(FrontendAuthConnector),
    bind[DelegationConnector].toInstance(FrontEndDelegationConnector),
    bind[UserDetailsConnector].toInstance(UserDetailsConnector),
    // Services
    bind[AuditService].toInstance(AuditService),
    bind[BbsiService].toInstance(BbsiService),
    bind[CodingComponentService].toInstance(CodingComponentService),
    bind[BenefitsService].toInstance(BenefitsService),
    bind[CompanyCarService].toInstance(CompanyCarService),
    bind[EmploymentService].toInstance(EmploymentService),
    bind[HasFormPartialService].toInstance(HasFormPartialService),
    bind[IncomeService].toInstance(IncomeService),
    bind[PersonService].toInstance(PersonService),
    bind[SessionService].toInstance(SessionService),
    bind[TaxAccountService].toInstance(TaxAccountService),
    bind[TaxCodeChangeService].toInstance(TaxCodeChangeService),
    bind[TrackingService].toInstance(TrackingService),
    // Journey Cache Services
    bind[JourneyCacheService].qualifiedWith("Add Employment").toInstance(JourneyCacheService(AddEmployment_JourneyKey)),
    bind[JourneyCacheService].qualifiedWith("Close Bank Account").toInstance(JourneyCacheService(CloseBankAccountJourneyKey)),
    bind[JourneyCacheService].qualifiedWith("Company Car").toInstance(JourneyCacheService(CompanyCar_JourneyKey)),
    bind[JourneyCacheService].qualifiedWith("Update Income").toInstance(JourneyCacheService(UpdateIncome_JourneyKey)),
    bind[JourneyCacheService].qualifiedWith("End Company Benefit").toInstance(JourneyCacheService(EndCompanyBenefit_JourneyKey)),
    bind[JourneyCacheService].qualifiedWith("End Employment").toInstance(JourneyCacheService(EndEmployment_JourneyKey)),
    bind[JourneyCacheService].qualifiedWith("Successful Journey").toInstance(JourneyCacheService(TrackSuccessfulJourney_JourneyKey)),
    bind[JourneyCacheService].qualifiedWith("Update Bank Account").toInstance(JourneyCacheService(UpdateBankAccountJourneyKey)),
    bind[JourneyCacheService].qualifiedWith("Update Bank Account Choice").toInstance(JourneyCacheService(UpdateBankAccountChoiceJourneyKey))
  )
}
