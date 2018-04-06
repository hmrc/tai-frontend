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

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.partials._
import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter

object AuditConnector extends Auditing with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}

trait Hooks extends HttpHooks with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  override lazy val auditConnector: Auditing = AuditConnector
}

trait WSHttp extends HttpGet with WSGet
  with HttpPut with WSPut
  with HttpPost with WSPost
  with HttpDelete with WSDelete
  with Hooks with AppName

object WSHttp extends WSHttp

trait WSHttpProxy extends WSHttp with WSProxy with RunMode with HttpAuditing with ServicesConfig

object WSHttpProxy extends WSHttpProxy {
  override lazy val appName = getString("appName")
  override lazy val wsProxyServer = WSProxyConfiguration(s"proxy")
  override lazy val auditConnector = AuditConnector
}

object TaiHtmlPartialRetriever extends FormPartialRetriever {
  override val httpGet = WSHttp
  override def crypto: String => String = SessionCookieCryptoFilter.encrypt
}

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  val serviceUrl = baseUrl("auth")
  lazy val http = WSHttp
}

object FrontEndDelegationConnector extends DelegationConnector with ServicesConfig {
  override protected def serviceUrl: String = baseUrl("delegation")
  override protected def http: WSHttp = WSHttp
}
