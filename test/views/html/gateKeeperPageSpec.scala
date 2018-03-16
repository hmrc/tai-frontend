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

import play.api.mvc.Call
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class gateKeeperPageSpec extends TaiViewSpec {

  override def view = views.html.gateKeeper()
  val taxYearStart: String = s"6 April " + TaxYear().year
  val taxYearEnd: String = s"5 April " + (TaxYear().year + 1)

  "Gate Keeper page" should {
    behave like pageWithHeader(messages("tai.gatekeeper.heading"))
    behave like pageWithTitle(messages("tai.gatekeeper.heading"))
    behave like pageWithBackLink
  }

  "have the tax year, tax year start date and tax year end date as h2" in {
    doc must haveHeadingH2WithText(messages("tai.taxYear", taxYearStart, taxYearEnd))
  }

  "have paragraph one and paragraph two with messages" in {
    doc must haveParagraphWithText(messages("tai.gateKeeper.description.p1", taxYearStart))
    doc must haveParagraphWithText(messages("tai.gateKeeper.description.p2"))
  }

  "have bullet point with description, tax year start and tax year end under paragraph two" in {
    doc must haveBulletPointWithText(messages("tai.gateKeeper.description.p2.1", taxYearStart, taxYearEnd))
  }

  "have paragraph with message of contact information" in {
    doc must haveParagraphWithText(messages("tai.gatekeeper.contact"))
  }

  "have static bullet points under contact info paragraph" in {
    doc must haveBulletPointWithText(messages("tai.gatekeeper.telephone"))
    doc must haveBulletPointWithText(messages("tai.gatekeeper.textphone"))
    doc must haveBulletPointWithText(messages("tai.gatekeeper.outsideUk"))
  }

  "have a message containing link" in {
    doc must haveLinkWithText(messages("tai.backToHome-link.upper"))
  }

  }
