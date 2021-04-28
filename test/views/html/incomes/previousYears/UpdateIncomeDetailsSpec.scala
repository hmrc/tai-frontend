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

package views.html.incomes.previousYears

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.income.previousYears.UpdateIncomeDetailsForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.TaxPeriodLabelService
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.income.previousYears.UpdateHistoricIncomeDetailsViewModel

class UpdateIncomeDetailsSpec extends TaiViewSpec {

  private val taxYear: Int = 2016
  private val formattedTaxYear = TaxPeriodLabelService.taxPeriodLabelSingleLine(taxYear)
  private val givenTaxYear: TaxYear = TaxYear(taxYear)
  private val UpdateIncomeDetails = inject[UpdateIncomeDetailsView]
  override def view: Html =
    UpdateIncomeDetails(UpdateHistoricIncomeDetailsViewModel(taxYear), UpdateIncomeDetailsForm.form)

  "UpdateIncomeDetails" must {
    behave like pageWithTitle(Messages("tai.income.previousYears.details.title"))
    behave like pageWithCombinedHeader(
      Messages("tai.income.previousYears.details.preHeading"),
      Messages("tai.income.previousYears.details.heading", formattedTaxYear))
    behave like pageWithContinueButtonForm("/check-income-tax/update-income-details/what-do-you-want-to-tell-us")
    behave like pageWithCancelLink(controllers.routes.PayeControllerHistoric.payePage(givenTaxYear))
    behave like pageWithBackLink

  }

}
