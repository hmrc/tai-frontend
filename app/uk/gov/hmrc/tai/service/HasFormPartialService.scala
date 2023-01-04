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

package uk.gov.hmrc.tai.service

import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.HttpGet
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.EnhancedPartialRetriever

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasFormPartialService @Inject()(httpClient: DefaultHttpClient, applicationConfig: ApplicationConfig)(
  implicit ec: ExecutionContext)
    extends EnhancedPartialRetriever {

  override val http: HttpGet = httpClient

  def getIncomeTaxPartial(implicit request: RequestHeader): Future[HtmlPartial] =
    loadPartial(applicationConfig.incomeTaxFormPartialLinkUrl)
}
