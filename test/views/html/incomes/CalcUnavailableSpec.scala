/*
 * Copyright 2020 HM Revenue & Customs
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

import play.twirl.api.Html
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class CalcUnavailableSpec extends TaiViewSpec {

  val id = 1
  val employerName = "Employer"

  "Calculation unavailable page" should {
    behave like pageWithBackLink
    behave like pageWithCombinedHeader(
      messages("tai.unableToCalculate.preHeading", employerName),
      messages("tai.unableToCalculate.title"))
  }

  override def view: Html = views.html.incomes.calcUnavailable(id, employerName)
}
