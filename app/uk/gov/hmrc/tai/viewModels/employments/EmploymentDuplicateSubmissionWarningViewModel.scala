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

package uk.gov.hmrc.tai.viewModels.employments

import play.api.i18n.Messages
import uk.gov.hmrc.tai.viewModels.duplicateSubmissionWarning.DuplicateSubmissionWarning

case class EmploymentDuplicateSubmissionWarningViewModel(incomeSource: String) extends DuplicateSubmissionWarning(incomeSource) {

  override def warningHeading(implicit messages: Messages): String = {
    messages("tai.employment.warning.heading", incomeSource)
  }

  override def yesChoiceText(implicit messages: Messages): String = {
    messages("tai.employment.warning.radio1", incomeSource)
  }

  override def noChoiceText(implicit messages: Messages): String = {
    messages("tai.employment.warning.radio2")
  }

  override def actionRoute = {controllers.employments.routes.EndEmploymentController.submitDuplicateSubmissionWarning}
}

