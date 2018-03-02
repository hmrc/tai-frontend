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

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class manualCorrespondenceSpec extends TaiViewSpec {

  override def view = views.html.manualCorrespondence()

  "manual correspondence page" should {
    behave like pageWithTitle(messages("tai.gatekeeper.refuse.title"))
    behave like pageWithHeader(messages("label.there_is_a_problem_accessing_your_account"))
  }

  "have a paragraph with message" in {
    doc must haveParagraphWithText(messages("label.we_need_to_speak_to_you_before_you_can_access"))
  }

  "have header two with text" in {
    doc must haveHeadingH2WithText(messages("label.how_to_fix_this"))
  }

  "have bullet points with static message" in {
    doc must haveOrderedBulletPointWithText(messages("label.phone_mci"))
    doc must haveOrderedBulletPointWithText(messages("label.say_I_cannot_log_in"))
    doc must haveOrderedBulletPointWithText(messages("label.say_yes_when_asked"))
    doc.select(".list> li").get(3).html() mustBe Html(messages("label.you_will_hear_a_recorded_message")).toString()
    doc must haveOrderedBulletPointWithText(messages("label.tell_the_adviser"))
  }

  "have header two with contact info" in {
    doc must haveHeadingH2WithText(messages("label.how_to_contact_us"))
  }

  "have bullet points with messages" in {
    doc must haveBulletPointWithText(messages("label.textphone_mci"))
    doc must haveBulletPointWithText(messages("label.phone_mci_outside_uk"))
  }

  "have paragraph with contact timings" in {
    doc must haveParagraphWithText(messages("label.phone_lines_are_open_8am_to_8pm_monday_to_friday_and_8am_to_4pm_on_saturday"))
    doc must haveParagraphWithText(messages("label.closed_sundays_and_bank_holidays"))
    doc must haveParagraphWithText(messages("label.phone_lines_are_less_busy_before_10am_monday-to_friday"))
  }

  "have link with call charges" in {
    val callChargesLink = doc.getElementById("callCharges")
    doc.select(".column-two-thirds> p > a").get(0) must haveLinkURL("https://www.gov.uk/call-charges")
    callChargesLink.text must include(messages("label.find_out_about_call_charges"))
    callChargesLink.text must include(messages("label.opens_in_a_new_window"))
  }
}
