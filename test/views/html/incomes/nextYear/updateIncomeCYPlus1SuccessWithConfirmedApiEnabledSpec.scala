/*
 * Copyright 2019 HM Revenue & Customs
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

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class updateIncomeCYPlus1SuccessWithConfirmedApiEnabledSpec extends TaiViewSpec {

  val employerName = "Employer Name"
  val isPension = false
  override def view: Html = views.html.incomes.nextYear.updateIncomeCYPlus1SuccessWithConfirmedApiEnabled(employerName, isPension)

  "CYPlus1 Success Page with ConfirmedApi enabled" should {
    "contain the success heading" in {
      doc(view).getElementsByTag("h1").text must include(
        s"${messages("tai.updateIncome.CYPlus1.success.confirmedApi.heading.p1")} ${messages("tai.updateIncome.CYPlus1.success.confirmedApi.heading.p2")}"
      )
    }

    "contain the success paragraph" in {
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.success.confirmedApi.p1"))
    }

    "contain the may change paragraph when income is from employment" in {
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.success.confirmedApi.employment.p2", employerName))
    }

    "contain the may change paragraph when income is from pension" in {
      val isPension = true
      val pensionView: Html = views.html.incomes.nextYear.updateIncomeCYPlus1SuccessWithConfirmedApiEnabled(employerName, isPension)
      doc(pensionView).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.success.confirmedApi.pension.p2", employerName))
    }
  }
}
