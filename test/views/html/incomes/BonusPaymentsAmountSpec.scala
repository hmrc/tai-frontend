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
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.BonusOvertimeAmountForm
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class BonusPaymentsAmountSpec extends TaiViewSpec with MockitoSugar {

  val id = 1
  val employerName = "Employer"
  val bonusPaymentsAmountForm = BonusOvertimeAmountForm.createForm()

  "Bonus payments amount view" should {
    behave like pageWithBackLink
    behave like pageWithCancelLink(Call("GET", controllers.routes.IncomeSourceSummaryController.onPageLoad(id).url))
    behave like pageWithCombinedHeader(
      messages("tai.bonusPaymentsAmount.preHeading", employerName),
      messages("tai.bonusPaymentsAmount.title",TaxYearRangeUtil.currentTaxYearRangeHtmlNonBreakBetween))
    behave like pageWithTitle(messages("tai.bonusPaymentsAmount.title", TaxYearRangeUtil.currentTaxYearRangeHtmlNonBreakBetween))
  }

  override def view: Html = views.html.incomes.bonusPaymentAmount(bonusPaymentsAmountForm,"monthly",id, employerName)
}