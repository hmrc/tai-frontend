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

package views.html.incomes.nextYear

import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class UpdateIncomeCYPlus1StartViewSpec extends TaiViewSpec {

  val employmentID                     = 1
  val isPension                        = false
  override def view: Html              = updateIncomeCYPlus1Start(employerName, employmentID, isPension)
  private val updateIncomeCYPlus1Start = inject[UpdateIncomeCYPlus1StartView]

  "CYPlus1 Start Page" should {
    behave like pageWithBackLink()
    behave like pageWithCombinedHeaderNewFormatNew(
      messages("tai.updateIncome.CYPlus1.preheading", employerName),
      messages("tai.updateIncome.CYPlus1.start.heading", employerName)
    )

    "contain the correct content when income is from employment" in {
      doc(view).getElementsByTag("p").text              must include(
        messages("tai.updateIncome.CYPlus1.start.paragraph1", employerName)
      )
      doc(view).getElementsByTag("p").text              must include(
        messages("tai.updateIncome.CYPlus1.start.paragraph2", employerName)
      )
      doc(view)                                         must haveLinkWithUrlWithID(
        "CYPlus1StartButton",
        controllers.income.routes.UpdateIncomeNextYearController.edit(employmentID).url
      )
      doc(view).getElementsByClass("govuk-button").text must include(messages("tai.updateIncome.CYPlus1.start.button"))
    }

    "contain the correct content when income is from pension" in {
      val isPension         = true
      val pensionView: Html = updateIncomeCYPlus1Start(employerName, employmentID, isPension)
      doc(pensionView).getElementsByTag("p").text              must include(
        messages("tai.updateIncome.CYPlus1.start.paragraph1", employerName)
      )
      doc(pensionView).getElementsByTag("p").text mustNot include(
        messages("tai.updateIncome.CYPlus1.start.paragraph2", employerName)
      )
      doc(pensionView).getElementsByTag("p").text              must include(
        messages("tai.updateIncome.CYPlus1.start.pension.paragraph2", employerName)
      )
      doc(pensionView)                                         must haveLinkWithUrlWithID(
        "CYPlus1StartButton",
        controllers.income.routes.UpdateIncomeNextYearController.edit(employmentID).url
      )
      doc(pensionView).getElementsByClass("govuk-button").text must include(
        messages("tai.income.details.updateTaxableIncome.update")
      )
    }

  }

}
