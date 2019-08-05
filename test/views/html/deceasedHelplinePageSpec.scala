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

package views.html

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.urls.Link
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class deceasedHelplinePageSpec extends TaiViewSpec {

  override def view: Html = views.html.deceased_helpline()

  "Deceased helpline page" should {
    behave like pageWithTitle(messages("tai.deceased.title"))
    behave like pageWithHeader(messages("tai.deceased.heading", authedUser.getDisplayName))
  }

  "the paragraph" should {
    "show information about deceased user" in {
      assertParagraph(messages("tai.deceased.information.p1", authedUser.getDisplayName))
      assertParagraph(messages("tai.deceased.information.p2"))
    }
  }

  "the bereavement helpline section" should {
    "contain a h2 heading" in {
      assertSubHeading("tai.deceased.bereavement.helpline")
    }

    "contain phone numbers" in {
      assertTimings("tai.deceased.telephone", "tai.deceased.telephone.number")
      assertTimings("tai.deceased.textphone", "tai.deceased.textphone.number")
      assertTimings("tai.deceased.outsideUK", "tai.deceased.outsideUK.number")
    }

    "display call charges link" in {
      val callChargesLink = doc.getElementById("callCharges")
      callChargesLink must haveLinkURL("https://www.gov.uk/call-charges")
      doc.getElementsByTag("a").toString must include(messages("tai.deceased.call.charges"))
    }
  }

  "the opening time" should {
    "contain a h3 heading" in {
      assertSubHeadingH3("tai.deceased.opening.times")
    }

    "display the static timings content" in {
      assertBulletPoints(messages("tai.deceased.opening.times.p1"))
      assertBulletPoints(messages("tai.deceased.opening.times.p2"))
      assertBulletPoints(messages("tai.deceased.opening.times.p3"))
      assertParagraph(messages("tai.deceased.opening.times.p4"))
    }
  }

  "the best timings" should {
    "contain a h3 heading" in {
      assertSubHeadingH3("tai.deceased.best.times")
    }

    "display static best time to call content" in {
      assertParagraph(messages("tai.deceased.best.times.p1"))
    }
  }

  "the post section" should {
    "contain a h2 heading" in {
      assertSubHeading("tai.deceased.post")
    }

    "display different address link" in {
      assertElementById(
        "different-address",
        Html(
          Messages(
            "tai.deceased.post.p2",
            Link
              .toInternalPage(
                url = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/couriers",
                value = Some(Messages("tai.deceased.post.p2.link.text")))
              .toHtml
          )).body
      )
    }

    "display static post content" in {
      assertParagraph(messages("tai.deceased.post.p1"))
      assertElementById("post-address", messages("tai.deceased.post.address1"))
      assertElementById("post-address", messages("tai.deceased.post.address2"))
      assertElementById("post-address", messages("tai.deceased.post.address3"))
      assertElementById("post-address", messages("tai.deceased.post.address4"))
    }
  }

  "the tell us once section" should {
    "contain a h2 heading" in {
      assertSubHeading("tai.deceased.tell.us")
    }

    "display tell us once link" in {
      assertElementById(
        "tell-us-once",
        Html(
          Messages(
            "tai.deceased.tell.us.p1",
            Link
              .toInternalPage(
                url = "https://www.gov.uk/after-a-death/organisations-you-need-to-contact-and-tell-us-once",
                value = Some(Messages("tai.deceased.tell.us.p1.link.text")))
              .toHtml
          )).body
      )
    }

  }

  val assertSubHeading = (subHeadingKey: String) => doc must haveHeadingH2WithText(messages(subHeadingKey))
  val assertSubHeadingH3 = (subHeadingKey: String) => doc must haveHeadingH3WithText(messages(subHeadingKey))
  val assertParagraph = (message: String) => doc must haveParagraphWithText(message)
  val assertElementById = (id: String, message: String) =>
    doc.getElementById(id).toString must include(messages(message))
  val assertBulletPoints = (message: String) => doc must haveBulletPointWithText(message)
  val assertTimings = (message1: String, message2: String) =>
    assertBulletPoints(messages(message1) + " " + messages(message2))

}
