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

import controllers.routes
import uk.gov.hmrc.tai.viewModels.NoCYIncomeTaxErrorViewModel
import play.twirl.api.Html
import uk.gov.hmrc.tai.service.TaxPeriodLabelService
import uk.gov.hmrc.tai.util.TaiConstants.EmployeePensionIForm
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class NoCYIncomeTaxErrorPageSpec extends TaiViewSpec {

  "noCYIncomeTaxErrorPage" should {
    behave like pageWithTitle(messages("tai.noCYIncomeError.heading"))
    behave like pageWithHeader(messages("tai.noCYIncomeError.heading"))

    "have subheading" in {
      doc must haveHeadingH2WithText(messages("tai.noCYIncomeError.sub-heading") + " " + TaxPeriodLabelService.longFormCurrentTaxPeriodLabel)
    }

    "display no income message" when {
      "name and end date of employer is not present" in {
        doc must haveParagraphWithText(messages("tai.noCYIncomeError.body.missing.employment"))
      }

      "name and end date of employer is present" in {
        val endDate = "9 June 2016"

        def view: Html = views.html.noCYIncomeTaxErrorPage(NoCYIncomeTaxErrorViewModel(Some(endDate)))
        doc(view) must haveParagraphWithText(messages("tai.noCYIncomeError.body.with.employment", endDate))
      }
    }

    "have missing info link" in {
      doc must haveParagraphWithText(messages("tai.noCYIncomeError.missingInfo",
        messages("tai.missingInfo.link.message")))

      val linkElement = doc.getElementById("missing-info-Iform").getElementsByTag("a").get(0)

      linkElement must haveLinkURL(routes.AuditController.auditLinksToIForm(EmployeePensionIForm).url)
    }

    "have a back link" in {
      doc.select("#backLink").text() mustBe messages("tai.back-link.upper")
      doc.select("#backLink").attr("href") mustBe routes.WhatDoYouWantToDoController.whatDoYouWantToDoPage().toString
    }
  }

  override def view: Html = views.html.noCYIncomeTaxErrorPage(NoCYIncomeTaxErrorViewModel(None))
}
