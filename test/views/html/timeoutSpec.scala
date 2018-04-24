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
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class timeoutSpec
  extends UnitSpec
  with FakeTaiPlayApplication
  with ScalaFutures {

  "Rendering timeout.scala.html" should {

    implicit val request = FakeRequest("GET", routes.ServiceController.timeoutPage().url)
    implicit val templateRenderer = MockTemplateRenderer
    implicit val partialRetriever = MockPartialRetriever

    "have the correct title for the page" in {
      val doc = Jsoup.parse(views.html.timeout().toString())

      doc.select(".page-header").text shouldBe "Log In"
    }
  }
}
