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

import akka.actor.ActorSystem
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.ws.{DefaultWSProxyServer, WSClient, WSProxyServer}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.partials._


trait HttpClient extends HttpGet with HttpPut with HttpPost with HttpDelete with HttpPatch

class ProxyHttpClient @Inject()(config: Configuration,
                                override val auditConnector: Auditing,
                                override val wsClient: WSClient,
                                defaultWSProxyServer: DefaultWSProxyServer,
                                override protected val actorSystem: ActorSystem)
  extends HttpClient
    with WSHttp
    with HttpAuditing
    with WSProxy {

  override lazy val configuration = Option(config.underlying)

  override val appName: String = new AppName {
    override def configuration: Configuration = config
  }.appName

  override val hooks: Seq[HttpHook] = Seq(AuditingHook)

  override def wsProxyServer: Option[WSProxyServer] = Some(defaultWSProxyServer)
}

class TaiHtmlPartialRetriever @Inject()(sessionCookieCrypto: SessionCookieCrypto, http: DefaultHttpClient) extends FormPartialRetriever {
  override val httpGet = http

  override def crypto: String => String = cookie => sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value
}
