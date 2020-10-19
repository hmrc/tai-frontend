/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import javax.inject.{Inject, Named}
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSProxyServer}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.partials._

trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch

class ProxyHttpClient @Inject()(
  @Named("appName") applicationName: String,
  config: Configuration,
  override val auditConnector: Auditing,
  override val wsClient: WSClient,
  override protected val actorSystem: ActorSystem)
    extends HttpClient with WSHttp with HttpAuditing with WSProxy {

  override lazy val configuration = Option(config.underlying)

  override val appName: String = applicationName

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)

  override def wsProxyServer: Option[WSProxyServer] = WSProxyConfiguration("proxy", config)
}

class TaiHtmlPartialRetriever @Inject()(http: DefaultHttpClient) extends FormPartialRetriever {
  override val httpGet = http

  override def crypto: String => String = cookie => cookie
}
