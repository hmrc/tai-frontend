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

package views.html.employments

import controllers.routes
import org.joda.time.LocalDate
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.employments.WithinSixWeeksViewModel

class endEmploymentWithinSixWeeksErrorSpec extends TaiViewSpec {

  private val template = inject[EndEmploymentWithinSixWeeksErrorView]

  override def view: Html = template(model)

  "endEmploymentWithinSixWeeksError" must {
    behave like pageWithTitle(messages("tai.endEmploymentWithinSixWeeksError.heading", model.earliestUpdateDateInHtml))
    behave like pageWithHeader(messages("tai.endEmploymentWithinSixWeeksError.heading", model.earliestUpdateDateInHtml))

    "have a paragraph that contains the employer name and last pay date" in {
      doc must haveParagraphWithText(
        messages(
          "tai.endEmploymentWithinSixWeeksError.employerAndPayDate",
          model.employerName,
          model.latestPayDateInHtml))
    }
    "have a paragraph that tells the user what the employer should do" in {
      doc must haveParagraphWithText(
        messages("tai.endEmploymentWithinSixWeeksError.whatTheEmployerShouldDo", model.earliestUpdateDateInHtml))
    }
    "have a paragraph that tells the user what to ask their employer to check" in {
      doc must haveParagraphWithText(
        messages("tai.endEmploymentWithinSixWeeksError.checkWithEmployer", model.employerName))

      doc must haveBulletPointWithText(messages("tai.endEmploymentWithinSixWeeksError.sendDetailsToHMRC"))
      doc must haveBulletPointWithText(messages("tai.endEmploymentWithinSixWeeksError.askForP45"))
    }
    "have a paragraph that tells the user to wait 6 weeks" in {
      doc must haveParagraphWithText(
        messages("tai.endEmploymentWithinSixWeeksError.wait6Weeks", model.earliestUpdateDateInHtml))
    }

    "have link" in {
      doc must haveLinkWithUrlWithID("returnToYourSummary", routes.TaxAccountSummaryController.onPageLoad().url)
    }
  }

  private val employerName = "Employer"
  private lazy val earliestUpdateDate = new LocalDate(2017, 6, 20)
  private lazy val latestPayDate = new LocalDate(2016, 5, 10)
  private lazy val model = WithinSixWeeksViewModel(earliestUpdateDate, employerName, latestPayDate, 2)
}
