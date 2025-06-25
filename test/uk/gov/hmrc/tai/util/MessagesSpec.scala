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

package uk.gov.hmrc.tai.util

import play.api.Application
import play.api.i18n.{Lang, Messages}
import play.api.inject.guice.GuiceApplicationBuilder
import utils.BaseSpec

import scala.util.matching.Regex

class MessagesSpec extends BaseSpec {

  private def isInteger(s: String): Boolean = s forall Character.isDigit

  private def toArgArray(msg: String) = msg.split("[{}]").map(_.trim()).filter(isInteger)

  private def countArgs(msg: String) = toArgArray(msg).length

  private def listArgs(msg: String) = toArgArray(msg).mkString

  private def assertNonEmptyValuesForDefaultMessages(): Unit =
    assertNonEmptyNonTemporaryValues("Default", defaultMessages)

  private def assertNonEmptyValuesForWelshMessages(): Unit = assertNonEmptyNonTemporaryValues("Welsh", welshMessages)

  private def assertCorrectUseOfQuotesForDefaultMessages(): Unit = assertCorrectUseOfQuotes("Default", defaultMessages)

  private def assertCorrectUseOfQuotesForWelshMessages(): Unit = assertCorrectUseOfQuotes("Welsh", welshMessages)

  private def assertNonEmptyNonTemporaryValues(label: String, messages: Map[String, String]): Unit = messages.foreach {
    case (msgKey: String, msgValue: String) =>
      withClue(s"In $label, there is an empty value for the key:[$msgKey][$msgValue]") {
        msgValue.trim.isEmpty mustBe false
      }
  }

  private def assertCorrectUseOfQuotes(label: String, messages: Map[String, String]): Unit = messages.foreach {
    case (msgKey: String, msgValue: String) =>
      withClue(s"In $label, there is an unescaped or invalid quote:[$msgKey][$msgValue]") {
        MatchSingleQuoteOnly.findFirstIn(msgValue).isDefined mustBe false
        MatchBacktickQuoteOnly.findFirstIn(msgValue).isDefined mustBe false
      }
  }

  private def listMissingMessageKeys(header: String, missingKeys: Set[String]) =
    missingKeys.toList.sorted.mkString(header + displayLine, "\n", displayLine)

  private lazy val displayLine = "\n" + ("@" * 42) + "\n"

  private lazy val defaultMessages: Map[String, String] = getExpectedMessages("default")

  private lazy val welshMessages: Map[String, String] = getExpectedMessages("cy") -- commonProvidedKeys -- scaKeys

  private def getExpectedMessages(languageCode: String) =
    messagesApi.messages.getOrElse(languageCode, throw new Exception(s"Missing messages for $languageCode"))

  private def mismatchingKeys(defaultKeySet: Set[String], welshKeySet: Set[String]) = {
    val test1 =
      listMissingMessageKeys("The following message keys are missing from Welsh Set:", defaultKeySet.diff(welshKeySet))
    val test2 = listMissingMessageKeys(
      "The following message keys are missing from English Set:",
      welshKeySet.diff(defaultKeySet)
    )

    test1 ++ test2
  }

  private val commonProvidedKeys = Set(
    "error.address.invalid.character"
  )

  private val scaKeys = Set(
    "label.back",
    "sca-wrapper.banner.label.no_thanks",
    "sca-wrapper.child.benefit.banner.heading",
    "sca-wrapper.child.benefit.banner.link.text",
    "sca-wrapper.fallback.menu.back",
    "sca-wrapper.fallback.menu.bta",
    "sca-wrapper.fallback.menu.home",
    "sca-wrapper.fallback.menu.messages",
    "sca-wrapper.fallback.menu.name",
    "sca-wrapper.fallback.menu.profile",
    "sca-wrapper.fallback.menu.progress",
    "sca-wrapper.fallback.menu.signout",
    "sca-wrapper.help.improve.banner.heading",
    "sca-wrapper.help.improve.banner.link.text",
    "sca-wrapper.timeout.keepAlive",
    "sca-wrapper.timeout.message",
    "sca-wrapper.timeout.signOut",
    "sca-wrapper.timeout.title",
    "attorney.banner.link",
    "attorney.banner.using.service.for"
  )

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .configure(
      Map("application.langs" -> "en,cy", "govuk-tax.Test.enableLanguageSwitching" -> "true")
    )
    .build()

  override implicit lazy val messages: Messages = messagesApi.preferred(Seq(lang, Lang("cy")))

  val MatchSingleQuoteOnly: Regex   = """\w+'\w+""".r
  val MatchBacktickQuoteOnly: Regex = """`+""".r

  "Application" should {
    "have the correct message configs" in {
      messagesApi.messages.size mustBe 4
      messagesApi.messages.keys must contain theSameElementsAs Vector("en", "cy", "default", "default.play")
    }

    "have messages for default and cy only" in {
      (messagesApi.messages("en") -- scaKeys).size mustBe 0
      val englishMessageCount = messagesApi.messages("default").size

      (messagesApi.messages("cy") -- scaKeys).size mustBe englishMessageCount
    }
  }

  "All message files" should {
    "have the same set of keys" in {
      withClue(mismatchingKeys(defaultMessages.keySet, welshMessages.keySet)) {
        assert(welshMessages.keySet equals defaultMessages.keySet)
      }
    }
    "not have the same messages" in {
      val same = defaultMessages.keys.collect {
        case msgKey if defaultMessages.get(msgKey) == welshMessages.get(msgKey) =>
          (msgKey, defaultMessages.get(msgKey))
      }

      // 94% of app needs to be translated into Welsh. 94% allows for:
      //   - Messages which just can't be different from English
      //     E.g. addresses, acronyms, numbers, etc.
      //   - Content which is pending translation to Welsh
      same.size.toDouble / defaultMessages.size.toDouble < 0.06 mustBe true
    }
    "have a non-empty message for each key" in {
      assertNonEmptyValuesForDefaultMessages()
      assertNonEmptyValuesForWelshMessages()
    }
    "have no unescaped single quotes in value" in {
      assertCorrectUseOfQuotesForDefaultMessages()
      assertCorrectUseOfQuotesForWelshMessages()
    }
    "have a resolvable message for keys which take args" in {
      val englishWithArgsMsgKeys = defaultMessages collect {
        case (msgKey, msgValue) if countArgs(msgValue) > 0 => msgKey
      }
      val welshWithArgsMsgKeys   = welshMessages collect { case (msgKey, msgValue) if countArgs(msgValue) > 0 => msgKey }
      val missingFromEnglish     = englishWithArgsMsgKeys.toList diff welshWithArgsMsgKeys.toList
      val missingFromWelsh       = welshWithArgsMsgKeys.toList diff englishWithArgsMsgKeys.toList
      missingFromEnglish foreach { msgKey =>
        println(s"Key which has arguments in English but not in Welsh: $msgKey")
      }
      missingFromWelsh foreach { msgKey =>
        println(s"Key which has arguments in Welsh but not in English: $msgKey")
      }
      englishWithArgsMsgKeys.size mustBe welshWithArgsMsgKeys.size
    }
    "have the same args in the same order for all keys which take args" in {
      val englishWithArgsMsgKeysAndArgList = defaultMessages collect {
        case (msgKey, msgValue) if countArgs(msgValue) > 0 => (msgKey, listArgs(msgValue))
      }
      val welshWithArgsMsgKeysAndArgList   = welshMessages collect {
        case (msgKey, msgValue) if countArgs(msgValue) > 0 => (msgKey, listArgs(msgValue))
      }
      val mismatchedArgSequences           = englishWithArgsMsgKeysAndArgList collect {
        case (msgKey, engArgSeq) if engArgSeq != welshWithArgsMsgKeysAndArgList(msgKey) =>
          (msgKey, engArgSeq, welshWithArgsMsgKeysAndArgList(msgKey))
      }
      mismatchedArgSequences foreach { case (msgKey, engArgSeq, welshArgSeq) =>
        println(
          s"key which has different arguments or order of arguments between English and Welsh: $msgKey -- English arg seq=$engArgSeq and Welsh arg seq=$welshArgSeq"
        )
      }
      mismatchedArgSequences.size mustBe 0
    }
  }
}
