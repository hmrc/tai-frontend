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

package views.html.taxCodeChange

import controllers.routes
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.urls.Link

class WhatHappensNextViewSpec extends TaiViewSpec {

  "what happens next" should {
    behave like pageWithTitle(messages("taxCode.change.whatHappensNext.title"))
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(messages("taxCode.change.journey.preHeading"), messages("taxCode.change.whatHappensNext.title"))

    "display static messages" in {
      doc must haveParagraphWithText(messages("taxCode.change.whatHappensNext.paragragh1"))

      doc(view).select("#check-income-tax-estimate").html() mustBe Html(messages("taxCode.change.whatHappensNext.paragragh2",
        Link.toInternalPage(
          id = Some("income-tax-estimate-link"),
          url = routes.EstimatedIncomeTaxController.estimatedIncomeTax().url.toString,
          value = Some(messages("taxCode.change.whatHappensNext.yourIncomeTaxEstimate.link"))).toHtml)).body


      doc must haveH2HeadingWithText(messages("taxCode.change.whatHappensNext.wrongInformation.text"))

      doc(view).select("#update-current-income-or-benefits").html() mustBe Html(messages("taxCode.change.whatHappensNext.paragragh3",
        Link.toInternalPage(
          id = Some("update-current-income-or-benefits-link"),
          url = routes.TaxAccountSummaryController.onPageLoad().url.toString,
          value = Some(messages("taxCode.change.whatHappensNext.updateCurrentIncomeOrBenefits.link"))).toHtml)).body
    }

  }

  override def view = views.html.taxCodeChange.whatHappensNext()

}
