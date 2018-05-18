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

package uk.gov.hmrc.tai.util.viewHelpers

import org.jsoup.nodes.{Attributes, Document, Element}
import org.jsoup.select.Elements
import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.i18n.Messages

trait JsoupMatchers {

  import scala.collection.JavaConversions._

  class TagWithTextMatcher(expectedContent: String, tag: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: List[String] =
        left.getElementsByTag(tag)
          .toList
          .map(_.text)

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in '$tag' elements:[\n$elementContents]",
        s"'$tag' element found with text [$expectedContent]"
      )
    }
  }

  class CssSelectorWithTextMatcher(expectedContent: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: List[String] =
        left.select(selector)
          .toList
          .map(_.text)

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in elements with '$selector' selector:[\n$elementContents]",
        s"[$expectedContent] element found with '$selector' selector and text [$expectedContent]"
      )
    }
  }

  class TagWithIdAndTextMatcher(expectedContent: String, tag: String, id: String) extends CssSelectorWithTextMatcher(expectedContent, s"$tag[id=${id}")

  class CssSelectorWithAttributeValueMatcher(attributeName: String, attributeValue: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val attributes: List[Attributes] =
        left.select(selector)
          .toList
          .map(_.attributes())

      lazy val attributeContents = attributes.mkString("\t", "\n\t", "")

      MatchResult(
        attributes.map(_.get(attributeName)).contains(attributeValue),
        s"[$attributeName=$attributeValue] not found in elements with '$selector' selector:[\n$attributeContents]",
        s"[$attributeName=$attributeValue] element found with '$selector' selector"
      )
    }
  }

  class CssSelectorWithClassMatcher(className: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val classes: List[String] =
        left.select(selector)
          .toList
          .map(_.className())

      lazy val classContents = classes.mkString("\t", "\n\t", "")

      MatchResult(
        classes.exists(_.contains(className)),
        s"[class=$className] not found in elements with '$selector' selector:[\n$classContents]",
        s"[class=$className] element found with '$selector' selector"
      )
    }
  }

  class CssSelector(selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: Elements =
        left.select(selector)

      MatchResult(
        elements.size >= 1,
        s"No element found with '$selector' selector",
        s"${elements.size} elements found with '$selector' selector"
      )
    }
  }


  class IdSelectorWithTextMatcher(expectedContent: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: String =
        left.getElementById(selector).text

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in elements with '$selector' selector:[\n$elementContents]",
        s"[$expectedContent] element found with '$selector' selector and text [$expectedContent]"
      )
    }
  }

  class IdSelectorWithUrlMatcher(expectedContent: String, selector: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val elements: String =
        left.getElementById(selector).attr("href")

      lazy val elementContents = elements.mkString("\t", "\n\t", "")

      MatchResult(
        elements.contains(expectedContent),
        s"[$expectedContent] not found in elements with id '$selector':[\n$elementContents]",
        s"[$expectedContent] element found with id '$selector' and url [$expectedContent]"
      )
    }
  }

  class IdSelectorWithUrlAndTextMatcher(id: String, url: String, text: String) extends Matcher[Document] {
    def apply(left: Document): MatchResult = {
      val element = left.getElementById(id)
      val hrefFound: String = element.attr("href")
      val textFound: String = element.text

      MatchResult(
        hrefFound.contains(url) && textFound.contains(text),
        s"[url:$url][text:$text] not found in element with id:'$id' \nInstead found:[url:$hrefFound][text:$textFound]",
        s"Element found with id '$id' and url [$url] and text [$text]"
      )
    }
  }

  class ElementWithTextMatcher(expectedContent: String) extends Matcher[Element] {
    def apply(left: Element): MatchResult = {
      MatchResult(
        left.text == expectedContent,
        s"[${expectedContent}] was not equal to [${left.text}]",
        s"[${expectedContent}] was equal to [${left.text}]"
      )

    }
  }

  class ElementWithAttributeValueMatcher(expectedContent: String, attribute: String) extends Matcher[Element] {
    def apply(left: Element): MatchResult = {
      val attribVal = left.attr(attribute)
      val attributes = left.attributes().asList().mkString("\t", "\n\t", "")

      MatchResult(
        attribVal == expectedContent,
        s"""[${attribute}="${expectedContent}"] is not a member of the element's attributes:[\n${attributes}]""",
        s"""[${attribute}="${expectedContent}"] is a member of the element's attributes:[\n${attributes}]""")
    }

  }

  class ElementWithClassMatcher(expectedClass: String) extends Matcher[Element] {
    def apply(left: Element): MatchResult = {
      val classes = left.classNames.toList
      val classNames = classes.mkString("\t", "\n\t", "")

      MatchResult(
        classes.contains(expectedClass),
        s"[${expectedClass}] is not a member of the element's classes:[\n${classNames}]",
        s"[${expectedClass}] is a member of the element's classes:[\n${classNames}]")
    }

  }

  //document matchers
  def haveHeadingH2WithText(expectedText: String) = new TagWithTextMatcher(expectedText, "h2")
  def haveHeadingH3WithText(expectedText: String) = new TagWithTextMatcher(expectedText, "h3")
  def haveHeadingH4WithText(expectedText: String) = new TagWithTextMatcher(expectedText, "h4")
  def haveHeadingWithText (expectedText: String) = new TagWithTextMatcher(expectedText, "h1")
  def haveH2HeadingWithIdAndText(id: String, expectedText: String) = new CssSelectorWithTextMatcher(expectedText, s"h2[id=${id}]")
  def havePreHeadingWithText (expectedText: String, expectedPreHeadingAnnouncement: String = "This section is") =
    new CssSelectorWithTextMatcher(s"${expectedPreHeadingAnnouncement} ${expectedText}", "header>p")
  def haveH2HeadingWithText (expectedText: String) = new TagWithTextMatcher(expectedText, "h2")

  def haveDescriptionTermWithIdAndText(id: String, expectedText: String) = new CssSelectorWithTextMatcher(expectedText, s"dt[id=${id}]")
  def haveTermDescriptionWithIdAndText(id: String, expectedText: String) = new CssSelectorWithTextMatcher(expectedText, s"dd[id=${id}]")
  def haveParagraphWithText (expectedText: String) = new TagWithTextMatcher(expectedText, "p")
  def haveSpanWithText (expectedText: String) = new TagWithTextMatcher(expectedText, "span")
  def haveListItemWithText (expectedText: String) = new TagWithTextMatcher(expectedText, "li")
  def haveBulletPointWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText, "ul>li")
  def haveOrderedBulletPointWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText, "ol>li")
  def haveThWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText,"th")
  def haveTdWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText,"td")
  def haveCaptionWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText,"caption")
  def haveContinueSubmitInput = new CssSelectorWithAttributeValueMatcher("value", "Continue", "input[type=submit]")
  def haveSubmitButton(expectedText: String) = new CssSelectorWithTextMatcher(expectedText,"button[type=submit]")
  def haveSummaryWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText,"summary")
  def haveFormWithSubmitUrl(url: String) = new CssSelectorWithAttributeValueMatcher("action", url, "form[method=POST]")
  def haveDescriptionListWithId(id: String) = new CssSelectorWithAttributeValueMatcher("id", id, "dl")
  def haveUnorderedListWithId(id: String) = new CssSelectorWithAttributeValueMatcher("id", id, "ul")
  def haveAsideWithId(id: String) = new CssSelectorWithAttributeValueMatcher("id", id, "aside")
  def haveSectionWithId(id: String) = new CssSelectorWithAttributeValueMatcher("id", id, "section")
  def haveTableWithId(id: String) = new CssSelectorWithAttributeValueMatcher("id", id, "table")
  def haveTableTheadWithId(id: String) = new CssSelectorWithAttributeValueMatcher("id", id, "thead")
  def haveTableTdWithId(id: String) = new CssSelectorWithAttributeValueMatcher("id", id, "td")
  def haveTableThWithIdAndText(id: String, expectedText: String) = new CssSelectorWithTextMatcher(expectedText, s"th[id=${id}]")
  def haveTableCaptionWithIdAndText(id: String, expectedText: String) = new CssSelectorWithTextMatcher(expectedText, s"caption[id=${id}]")
  def haveElementAtPathWithId(elementSelector: String, id: String) = new CssSelectorWithAttributeValueMatcher("id", id, elementSelector)
  def haveElementAtPathWithText(elementSelector: String, expectedText: String) = new CssSelectorWithTextMatcher(expectedText, elementSelector)
  def haveElementAtPathWithAttribute(elementSelector: String, attributeName: String, attributeValue: String) = new CssSelectorWithAttributeValueMatcher(attributeName, attributeValue, elementSelector)
  def haveElementAtPathWithClass(elementSelector: String, className: String) = new CssSelectorWithClassMatcher(className, elementSelector)
  def haveElementWithId(id: String) = new CssSelector(s"#${id}")

  def haveTableRowWithText (expectedText: String) = new TagWithTextMatcher(expectedText, "dt")
  def haveTableRowWithTextDescription (expectedText: String) = new TagWithTextMatcher(expectedText, "dd")

  def haveCheckYourAnswersSummary = new CssSelectorWithAttributeValueMatcher("id", "check-answers-summary", "ul")
  def haveCheckYourAnswersSummaryLine(lineNo: Int, expectedQuestionText: String) = new TagWithIdAndTextMatcher(expectedQuestionText, "div", s"confirmation-line-${lineNo}-question")
  def haveCheckYourAnswersSummaryLineAnswer(lineNo: Int, expectedAnswerText: String) = new TagWithIdAndTextMatcher(expectedAnswerText, "div", s"confirmation-line-${lineNo}-answer")

  def haveCheckYourAnswersSummaryLineChangeLink(lineNo: Int, expectedLinkUrl: String) = new IdSelectorWithUrlMatcher(expectedLinkUrl, s"confirmation-line-${lineNo}-change-link")

  def haveLinkWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText, "a")
  def haveErrorLinkWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText, "div.error-summary>ul>li>a")
  def haveClassWithText(expectedText: String, className: String) = new CssSelectorWithTextMatcher(expectedText, s".$className")

  def haveBackLink = new CssSelector("a[id=backLink]")
  def haveBackButtonWithUrl(expectedURL: String) = new IdSelectorWithUrlMatcher(expectedURL, "backLink")
  def haveCancelLinkWithUrl(expectedURL: String) = new IdSelectorWithUrlMatcher(expectedURL, "cancelLink")
  def haveLinkWithUrlWithID(id: String, expectedURL: String) = new IdSelectorWithUrlMatcher(expectedURL, id)
  def haveReturnToSummaryButtonWithUrl(expectedURL: String) = new IdSelectorWithUrlMatcher(expectedURL, "returnToSummary")

  //element matchers
  def haveText(expectedText: String) = new ElementWithTextMatcher(expectedText)
  def haveLinkURL(expectedUrl: String) = new ElementWithAttributeValueMatcher(expectedUrl, "href")
  def haveClass(expectedClass: String) = new ElementWithClassMatcher(expectedClass)

  def haveLinkElement(id:String, href: String, text: String) = new IdSelectorWithUrlAndTextMatcher(id, href, text)

  def haveInputLabelWithText (id:String, expectedText: String) = new CssSelectorWithTextMatcher(expectedText, s"label[for=$id]")

  def haveStrongWithText (expectedText: String) = new CssSelectorWithTextMatcher(expectedText,"strong")
}