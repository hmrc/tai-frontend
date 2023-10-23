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

package views.html

import controllers.auth.{AuthedUser, AuthenticatedRequest}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.auth.core.ConfidenceLevel.L250
import uk.gov.hmrc.auth.core.retrieve.v2.TrustedHelper
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.util.viewHelpers.JsoupMatchers
import utils.BaseSpec

import scala.util.Random

class MainTemplateSpec extends BaseSpec with JsoupMatchers {

  private val mainTemplate = app.injector.instanceOf[MainTemplate]
  private implicit val authRequest = AuthenticatedRequest(FakeRequest(), authedUser, "Firstname Surname")

  "MainTemplate View" must {
    "show the correct nav menu items" when {
      "the user is a non SA user" in {
        def view =
          mainTemplate("Title", Some(authedUser.copy(utr = None)), backLinkContent = None)(Html(""))

        def doc: Document = Jsoup.parse(view.toString())

        doc must haveNavMenuItem("Account home")
        doc must haveNavMenuItem("Messages")
        doc must haveNavMenuItem("Check progress")
        doc must haveNavMenuItem("Profile and settings")
        doc mustNot haveNavMenuItem("Business Tax Account")
      }
      "the user is a SA user" in {
        def view =
          mainTemplate(
            "Title",
            Some(authedUser),
            backLinkContent = None
          )(
            Html("")
          )

        def doc: Document = Jsoup.parse(view.toString())

        doc must haveNavMenuItem("Account home")
        doc must haveNavMenuItem("Messages")
        doc must haveNavMenuItem("Check progress")
        doc must haveNavMenuItem("Profile and settings")
        doc must haveNavMenuItem("Business tax account")
      }
      "the user is acting as a a trusted helper" in {
        def view =
          mainTemplate(
            "Title",
            Some(
              AuthedUser(
                TrustedHelper("Principal", "Attorney", "/", nino.nino),
                Some("1130492359"),
                Some("GovernmentGateway"),
                L250,
                None
              )
            ),
            backLinkContent = None
          )(
            Html("")
          )

        def doc: Document = Jsoup.parse(view.toString())

        doc must haveNavMenuItem("Account home")
        doc must haveNavMenuItem("Messages")
        doc must haveNavMenuItem("Check progress")
        doc must haveNavMenuItem("Profile and settings")
        doc mustNot haveNavMenuItem("Business tax account")
      }
    }
    "display the attorney banner when acting as a trusted helped" in {
      def view =
        mainTemplate(
          "Title",
          Some(
            AuthedUser(
              TrustedHelper("Principal", "Attorney", "/", nino.nino),
              Some("1130492359"),
              Some("GovernmentGateway"),
              L250,
              None
            )
          ),
          backLinkContent = None
        )(
          Html("")
        )

      def doc: Document = Jsoup.parse(view.toString())

      doc must haveClassWithText("You are using this service for Principal.", "pta-attorney-banner__text")
    }
    "not display the attorney banner when not acting as a trusted helped" in {
      def view: HtmlFormat.Appendable =
        mainTemplate(
          "Title",
          Some(
            AuthedUser(
              nino,
              Some("1130492359"),
              Some("GovernmentGateway"),
              L250,
              None,
              None
            )
          ),
          backLinkContent = None
        )(
          Html("")
        )

      def doc: Document = Jsoup.parse(view.toString())

      doc mustNot haveClass("pta-attorney-banner__text")
    }
  }
}
