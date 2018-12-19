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

package uk.gov.hmrc.tai.config

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service._
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

class TaiModule extends Module with JourneyCacheConstants {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[PersonService].toInstance(PersonService),
    bind[FormPartialRetriever].toInstance(TaiHtmlPartialRetriever),
    bind[TemplateRenderer].toInstance(LocalTemplateRenderer),
    bind[TaxCodeChangeService].toInstance(TaxCodeChangeService),
    bind[CodingComponentService].toInstance(CodingComponentService),
    bind[EmploymentService].toInstance(EmploymentService),
    bind[CompanyCarService].toInstance(CompanyCarService),
    bind[TaxAccountService].toInstance(TaxAccountService),
    bind[AuditConnector].toInstance(AuditConnector),
    bind[AuthConnector].toInstance(FrontendAuthConnector),
    bind[DelegationConnector].toInstance(FrontEndDelegationConnector),
    bind[AuditService].toInstance(AuditService),
    bind[TrackingService].toInstance(TrackingService),
    bind[HasFormPartialService].toInstance(HasFormPartialService),
    bind[SessionService].toInstance(SessionService),
    bind[JourneyCacheService].qualifiedWith("Company Car").toInstance(JourneyCacheService(CompanyCar_JourneyKey)),
    bind[JourneyCacheService].qualifiedWith("Update Income").toInstance(JourneyCacheService(UpdateIncome_JourneyKey)),
    bind[IncomeService].toInstance(IncomeService)
  )
}
