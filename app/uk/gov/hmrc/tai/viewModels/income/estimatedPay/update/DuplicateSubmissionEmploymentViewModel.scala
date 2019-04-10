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

package uk.gov.hmrc.tai.viewModels.income.estimatedPay.update

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.MonetaryUtil

trait DuplicateSubmissionEstimatedPay {
  val heading: String
  val paragraphOne: String
  val optionOne: String
  val optionTwo: String
}

case class DuplicateSubmissionEmploymentViewModel(incomeName: String, newAmount: Int)(implicit messages: Messages) extends DuplicateSubmissionEstimatedPay {

  private val formattedNewAmount: String =  MonetaryUtil.withPoundPrefix(newAmount.toInt)

  override val heading: String = messages("tai.incomes.warning.employment.heading", incomeName)
  override val paragraphOne: String = messages("tai.incomes.warning.employment.text1", formattedNewAmount, incomeName)
  override val optionOne: String = messages("tai.incomes.warning.employment.radio1", incomeName)
  override val optionTwo: String = messages("tai.incomes.warning.employment.radio2", incomeName)
}

case class DuplicateSubmissionPensionViewModel(incomeName: String, newAmount: Int)(implicit messages: Messages) extends DuplicateSubmissionEstimatedPay {

  private val formattedNewAmount: String =  MonetaryUtil.withPoundPrefix(newAmount.toInt)

  override val heading: String = messages("tai.incomes.warning.pension.heading", incomeName)
  override val paragraphOne: String = messages("tai.incomes.warning.pension.text1", formattedNewAmount, incomeName)
  override val optionOne: String = messages("tai.incomes.warning.pension.radio1", incomeName)
  override val optionTwo: String = messages("tai.incomes.warning.pension.radio2", incomeName)
}