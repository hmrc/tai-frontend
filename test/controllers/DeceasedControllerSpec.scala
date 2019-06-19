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

package controllers

import builders.RequestBuilder
import mocks.MockTemplateRenderer
import org.jsoup.Jsoup
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.play.partials.FormPartialRetriever

import scala.language.postfixOps

class DeceasedControllerSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  "Deceased Controller" must {
    "load the deceased page" when {
      "triggered from any page" which {
        "the user is authorised" in {
          val sut = createSut

          val result = sut.deceased()(RequestBuilder.buildFakeRequestWithAuth("GET"))
          status(result) mustBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.title() must include(Messages("tai.deceased.title"))
        }
      }
    }
  }

  def createSut = new SUT

  class SUT extends DeceasedController(
    FakeAuthAction,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  )

}
