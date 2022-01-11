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

package views.html.incomes.nextYear

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class UpdateIncomeCYPlus1SuccessViewSpec extends TaiViewSpec {

  val employerName = "Employer Name"
  val isPension = false
  private val template = inject[UpdateIncomeCYPlus1SuccessView]

  override def view: Html = template(employerName, isPension)

  "CYPlus1 Success Page" should {
    "contain the success heading" in {
      doc(view).getElementsByTag("h1").text must include(
        s"${messages("tai.updateIncome.CYPlus1.success.heading.p1")} ${messages("tai.updateIncome.CYPlus1.success.heading.p2")}"
      )
    }

    "contain the success paragraph" in {
      doc(view).getElementsByTag("p").text must include(messages("tai.updateIncome.CYPlus1.success.p1"))
    }

    "contain the may change paragraph when income is from employment" in {
      doc(view).getElementsByTag("p").text must include(
        messages("tai.updateIncome.CYPlus1.success.employment.p2", employerName))
    }

    "contain the may change paragraph when income is from pension" in {
      val isPension = true
      val pensionView: Html = template(employerName, isPension)
      doc(pensionView).getElementsByTag("p").text must include(
        messages("tai.updateIncome.CYPlus1.success.pension.p2", employerName))
    }
  }
}
