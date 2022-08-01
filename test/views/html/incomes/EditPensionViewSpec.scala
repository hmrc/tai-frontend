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

package views.html.incomes

import play.twirl.api.Html
import uk.gov.hmrc.tai.forms.EditIncomeForm
import uk.gov.hmrc.tai.model.EmploymentAmount
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class EditPensionViewSpec extends TaiViewSpec {

  private val empId = 1
  private val employerName = "fakeFieldValue"
  private val newAmount = 321
  private val oldAmount = 123

  "Edit Pension view" should {
    behave like pageWithBackLink
    behave like pageWithCombinedHeaderNewFormat(
      messages("tai.howToUpdate.preHeading", employerName),
      messages("tai.incomes.edit.heading", TaxYearRangeUtil.currentTaxYearRange)
    )
  }

  val editIncomeForm = EditIncomeForm.create(
    EmploymentAmount(
      name = employerName,
      description = employerName,
      employmentId = empId,
      newAmount = newAmount,
      oldAmount = oldAmount
    )
  )

  private def editPension = inject[EditPensionView]
  override def view: Html = editPension(editIncomeForm, hasMultipleIncomes = false, empId, "0")
}
