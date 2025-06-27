/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.partials.HtmlPartial.*
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, HtmlPartial}

import scala.concurrent.{ExecutionContext, Future}

/*
 * This is a PartialRetriever with a HeaderCarrierForPartialsConverter to forward request headers on
 */
trait EnhancedPartialRetriever extends HeaderCarrierForPartialsConverter {

  val httpClientV2: HttpClientV2

  def loadPartial(partialUrl: String)(implicit request: RequestHeader, ec: ExecutionContext): Future[HtmlPartial] =
    httpClientV2.get(url"$partialUrl").execute[HtmlPartial] recover connectionExceptionsAsHtmlPartialFailure
}
