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

package uk.gov.hmrc.tai.util.viewHelpers

import builders.{UserBuilder}
import controllers.FakeTaiPlayApplication
import controllers.auth.AuthedUser
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.domain.Generator

import scala.util.Random

trait TaiViewSpec extends PlaySpec
  with JsoupMatchers
  with FakeTaiPlayApplication {
  implicit val request = FakeRequest()
  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages
  implicit val templateRenderer = MockTemplateRenderer
  implicit val partialRetriever = MockPartialRetriever
  implicit val authedUser: AuthedUser = UserBuilder()

  def view: Html

  def doc: Document = Jsoup.parse(view.toString())

  def doc(view: Html): Document = Jsoup.parse(view.toString())

  def pageWithTitle(titleText: String): Unit = {
    "have a static title" in {
      doc.title must include(titleText)
    }
  }

  def pageWithHeader(headerText: String): Unit = {
    "have a static h1 header" in {
      doc must haveHeadingWithText(headerText)
    }
  }

  def pageWithCombinedHeader(preHeaderText: String, mainHeaderText: String, preHeaderAnnouncementText: Option[String] = None): Unit = {
    "have an accessible pre heading" in {
      if(preHeaderAnnouncementText.isDefined){
        doc must havePreHeadingWithText(preHeaderText, expectedPreHeadingAnnouncement = preHeaderAnnouncementText.get)
      } else {
        doc must havePreHeadingWithText(preHeaderText)
      }
    }
    "have an h1 header consisting of the main heading text" in {
      doc must haveHeadingWithText(mainHeaderText)
    }
  }

  def pageWithH2Header(headerText: String): Unit = {
    "have a static h2 header" in {
      doc must haveH2HeadingWithText(headerText)
    }
  }

  def pageWithBackLink: Unit = {
    "have a back link" in {
      doc must haveBackLink
    }
  }

  def haveReturnToSummaryButtonWithUrl(previousPage: => Call): Unit = {
    "have a return to summary button with url" in {
      doc must haveReturnToSummaryButtonWithUrl(previousPage.url.toString)
    }
  }

  def pageWithContinueButtonForm(submitUrl: String): Unit = {
    pageWithButtonForm(submitUrl, "Continue")
  }

  def pageWithButtonForm(submitUrl: String, buttonText: String): Unit = {
    "have a form with a submit button or input labelled as buttonText" in {
      doc must haveSubmitButton(buttonText)
    }
    "have a form with the correct submit url" in {
      doc must haveFormWithSubmitUrl(submitUrl)
    }
  }

  def pageWithContinueInputForm(submitUrl: String): Unit = {
    "have a form with a submit button or input labelled 'Continue'" in {
      doc must haveContinueSubmitInput
    }
    "have a form with the correct submit url" in {
      doc must haveFormWithSubmitUrl(submitUrl)
    }
  }

  def pageWithCancelLink(call: Call): Unit = {
    "have a cancel link with url" in {
      doc must haveCancelLinkWithUrl(call.url.toString)
    }
  }

  def pageWithYesNoRadioButton(
                                idYes:String,
                                idNo:String,
                                yesLabelText: String = Messages("tai.label.yes"),
                                noLabelText: String = Messages("tai.label.no")): Unit = {
    "have a yes/no radio button" in {
      doc must haveInputLabelWithText(idYes, yesLabelText)
      doc must haveInputLabelWithText(idNo, noLabelText)
      doc.getElementById(idYes) must not be null
      doc.getElementById(idNo) must not be null

    }
  }

  def pageWithCheckYourAnswersSummary(): Unit = {
    "have a 'check your answers' summary section" in {
      doc must haveCheckYourAnswersSummary
    }
  }

  def nonBreakable(string: String): String = string.replace(" ", "\u00A0")
}
