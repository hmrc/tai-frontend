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

package views.html.incomes

import org.scalatest.mock.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.{EstimatedEmploymentIncomeSuccessViewModel, EstimatedPensionIncomeSuccessViewModel}

class EditSuccessSpec extends TaiViewSpec with MockitoSugar{

  private val employerId = 1
  private val employerName = "fakeFieldValue"

  "Edit Success Pension view" should {
    "contain the success heading" in {
      doc(pensionView).getElementsByTag("h1").text must include(messages("tai.incomes.updated.check.title", employerName))
    }

    "contain the success paragraph and link" in {
      doc(pensionView).getElementsByTag("p").text must include(
        s"${messages("tai.incomes.updated.pension.check.text")} ${messages("tai.incomes.updated.pension.check.link")}"
      )
    }

    "contain the may change paragraph" in {
      doc(pensionView).getElementsByTag("p").text must include(messages("tai.incomes.updated.pension.seeChanges.text", employerName))
    }
  }

  "Edit Success Employment view" should {
    "contain the success heading" in {
      doc(employmentView).getElementsByTag("h1").text must include(messages("tai.incomes.updated.check.title", employerName))
    }

    "contain the success paragraph and link" in {
      doc(employmentView).getElementsByTag("p").text must include(
        s"${messages("tai.incomes.updated.check.text")} ${messages("tai.incomes.updated.check.link")}"
      )
    }

    "contain the may change paragraph" in {
      doc(employmentView).getElementsByTag("p").text must include(messages("tai.incomes.seeChanges.text", employerName))
    }
  }

  val pensionViewModel = EstimatedPensionIncomeSuccessViewModel(employerName, employerId)
  val employmentViewModel = EstimatedEmploymentIncomeSuccessViewModel(employerName, employerId)

  def pensionView: Html = views.html.incomes.editSuccess(pensionViewModel)
  def employmentView: Html = views.html.incomes.editSuccess(employmentViewModel)

  override def view: Html = pensionView
}
