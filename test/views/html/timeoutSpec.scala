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

package views.html

import controllers.{FakeTaiPlayApplication, routes}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest

class timeoutSpec
  extends PlaySpec
  with FakeTaiPlayApplication
  with ScalaFutures
  with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Rendering timeout.scala.html" must {

    "have the correct title for the page" in {
      val doc = Jsoup.parse(views.html.timeout()(
        FakeRequest("GET", routes.ServiceController.timeoutPage().url), messagesApi,
        templateRenderer = MockTemplateRenderer, partialRetriever = MockPartialRetriever).toString())

      doc.select(".page-header").text mustBe "Log In"
    }
  }
}
