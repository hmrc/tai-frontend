/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.config.ApplicationConfig

import scala.concurrent.ExecutionContext

object HelpFormHelper {

  def replaceMessage(formPartialRetriever: FormPartialRetriever, applicationConfig: ApplicationConfig)(
    implicit request: Request[_],
    messages: Messages,
    ec: ExecutionContext): Html = {
    def partial = formPartialRetriever.getPartialContent(applicationConfig.reportAProblemPartialUrl)

    Html(
      partial.toString
        .replace(
          messages("tai.deskpro.link.text.original"),
          messages("tai.deskpro.link.text.replacement")
        ))
  }
}
