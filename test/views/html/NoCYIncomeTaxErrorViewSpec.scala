/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.routes
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.constants.TaiConstants.EmployeePensionIForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.NoCYIncomeTaxErrorViewModel

class NoCYIncomeTaxErrorViewSpec extends TaiViewSpec {

  "noCYIncomeTaxErrorPage" should {
    behave like pageWithTitle(messages("tai.noCYIncomeError.heading"))
    behave like pageWithCombinedHeaderNewTemplate(
      preHeaderText = "Current tax year",
      mainHeaderText = "Your PAYE Income Tax")

    "display no income message" when {
      "name and end date of employer is not present" in {
        doc must haveParagraphWithText(messages("tai.noCYIncomeError.body.missing.employment"))
      }

      "name and end date of employer is present" in {
        val endDate = "9 June 2016"

        def view: Html = template(NoCYIncomeTaxErrorViewModel(Some(endDate)))
        doc(view) must haveParagraphWithText(messages("tai.noCYIncomeError.body.with.employment", endDate))
      }
    }

    "have missing info link" in {
      doc must haveParagraphWithText(
        messages("tai.noCYIncomeError.missingInfo", messages("tai.missingInfo.link.message")))

      val linkElement = doc.getElementById("missing-info-Iform").getElementsByTag("a").get(0)

      linkElement must haveLinkURL(routes.AuditController.auditLinksToIForm(EmployeePensionIForm).url)
    }

    "have a back link" in {
      doc.select("#backLinkId").text() mustBe messages("tai.returnToChooseTaxYear")
      doc.select("#backLinkId").attr("href") mustBe routes.WhatDoYouWantToDoController
        .whatDoYouWantToDoPage()
        .toString
    }
  }
  private val template = inject[NoCYIncomeTaxErrorView]

  override def view: Html = template(NoCYIncomeTaxErrorViewModel(None))
}
