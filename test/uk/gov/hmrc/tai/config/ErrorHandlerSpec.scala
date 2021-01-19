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

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.tai.config.{ErrorHandler, TaiHtmlPartialRetriever}
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import views.html.internalServerError
import utils.BaseSpec
import org.jsoup.nodes.Document
import play.api.Configuration
import uk.gov.hmrc.tai.connectors.LocalTemplateRenderer

class ErrorHandlerSpec extends BaseSpec {
  implicit val request = FakeRequest()

  lazy val errorHandler: ErrorHandler = new ErrorHandler(
    app.injector.instanceOf[LocalTemplateRenderer],
    app.injector.instanceOf[TaiHtmlPartialRetriever],
    appConfig,
    messagesApi,
    app.injector.instanceOf[Configuration]
  )

  "notFoundTemplate" in {
    def notFound: Document = Jsoup.parse(errorHandler.internalServerErrorTemplate(request).toString())

    notFound.getElementsByTag("h1").toString must include(messages("global.page.not.found.error.heading"))

  }
}
