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

package views.html

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class deceasedHelplinePageSpec extends TaiViewSpec {

  override def view: Html = views.html.deceased_helpline()

  "Deceased helpline page" should {
    behave like pageWithTitle(messages("tai.deceased.title"))
    behave like pageWithHeader(
      s"${messages("tai.deceased.heading.part1")} ${HtmlFormatter.htmlNonBroken(messages("tai.deceased.heading.part2"))}")
  }

  "contain an h2 heading concerning a bereavement question" in {
    doc must haveHeadingH2WithText(messages("tai.deceased.question"))
  }

  "contain a paragraph stating to call hmrc" in {
    doc must haveParagraphWithText(messages("tai.deceased.callHmrc.paragraph"))
  }

  "contain a telephone section which " should {

    "include a telephone title" in {
      doc must haveHeadingH3WithText(messages("tai.deceased.telephone.title"))
    }

    "include a telephone number" in {
      doc must haveParagraphWithText(messages("tai.deceased.telephone.number"))
    }

    "include a telephone description section " in {
      doc must haveParagraphWithText(messages("tai.deceased.telephone.advice"))
    }

  }

  "contain a textphone section which " should {

    "include a textphone title" in {
      doc must haveHeadingH3WithText(messages("tai.deceased.textphone.title"))

    }

    "include a textphone number" in {
      doc must haveParagraphWithText(messages("tai.deceased.textphone.number"))
    }
  }

  "contain an Outside UK section which " should {

    "include an outside uk title" in {
      val element = doc.getElementById("outsideUk")

      element.text must include(messages("tai.deceased.outsideUK.title"))
      element.tag().toString mustBe "h3"

      val spanElement = element.children().first()
      spanElement.text mustBe messages("tai.deceased.telephone")
      spanElement.tag().toString mustBe "span"
      spanElement.attr("class") mustBe "visually-hidden"
    }

    "include an outside of uk telephone number" in {
      doc must haveParagraphWithText(messages("tai.deceased.outsideUK.number"))
    }

  }

  "contain an opening times section which" should {

    "include the hidden heading opening times" in {
      doc must haveHeadingH3WithText(messages("tai.deceased.opening.times"))
      doc must haveElementAtPathWithClass("h3[id=openingTimes]", "visually-hidden")
    }

    "include a time for Monday to Friday" in {
      doc must haveParagraphWithText(messages("tai.deceased.openingTimes.mondayToFriday"))
    }

    "include a time for Saturday" in {
      doc must haveParagraphWithText(messages("tai.deceased.openingTimes.saturday"))
    }

    "state daysClosed" in {
      doc must haveParagraphWithText(messages("tai.deceased.daysClosed"))
    }

  }

}
