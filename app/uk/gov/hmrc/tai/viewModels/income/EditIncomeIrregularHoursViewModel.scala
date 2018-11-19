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

package uk.gov.hmrc.tai.viewModels.income

import play.api.i18n.Messages
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.util.ViewModelHelper


case class EditIncomeIrregularHoursViewModel(heading: String, preHeading: String, employmentId: Int, employerName: String, currentAmount: String)

object EditIncomeIrregularHoursViewModel  extends ViewModelHelper {

  def apply(employmentId: Int,
            employerName: String,
            currentAmount: String)(implicit messages: Messages): EditIncomeIrregularHoursViewModel = {

    val taxYearMessage = currentTaxYearRangeHtmlNonBreak

    val heading = messages("tai.irregular.heading", taxYearMessage)
    val preHeading = messages("tai.estimatedPay.preHeading", employerName)

    new EditIncomeIrregularHoursViewModel(heading, preHeading, employmentId, employerName, currentAmount)
  }

  def apply(employmentId: Int,
            employerName: String,
            currentAmount: BigDecimal)(implicit messages: Messages): EditIncomeIrregularHoursViewModel = {

    val taxYearMessage = currentTaxYearRangeHtmlNonBreak

    val heading = messages("tai.irregular.heading", taxYearMessage)
    val preHeading = messages("tai.estimatedPay.preHeading", employerName)



    new EditIncomeIrregularHoursViewModel(heading, preHeading, employmentId, employerName, currentAmount.toString)
  }
}
