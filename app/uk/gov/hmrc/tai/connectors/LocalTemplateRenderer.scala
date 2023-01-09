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

package uk.gov.hmrc.tai.connectors

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class LocalTemplateRenderer @Inject()(http: DefaultHttpClient, servicesConfig: ServicesConfig)(
  implicit ec: ExecutionContext)
    extends TemplateRenderer {

  override lazy val templateServiceBaseUrl: String = servicesConfig.baseUrl("frontend-template-provider")
  override val refreshAfter: Duration = 10 minutes

  private implicit val hc = HeaderCarrier()

  override def fetchTemplate(path: String): Future[String] =
    http.GET(path).map(_.body)
}
