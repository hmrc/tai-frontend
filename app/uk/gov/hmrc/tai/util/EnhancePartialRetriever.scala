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

package uk.gov.hmrc.tai.util

import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.partials.{HtmlPartial, HeaderCarrierForPartialsConverter}
import HtmlPartial._
import uk.gov.hmrc.http.HttpGet


/*
 * This is a PartialRetriever with a HeaderCarrierForPartialsConverter to forward request headers on
 */
trait EnhancedPartialRetriever extends HeaderCarrierForPartialsConverter {

  val http: HttpGet

  def loadPartial(url: String)(implicit request: RequestHeader) = {
    http.GET[HtmlPartial](url) recover connectionExceptionsAsHtmlPartialFailure
  }
}
