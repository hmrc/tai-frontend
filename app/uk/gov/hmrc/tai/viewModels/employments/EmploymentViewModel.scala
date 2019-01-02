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

import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.play.language.LanguageUtils.Dates

case class EmploymentViewModel(employerName: String, empId: Int)

case class WithinSixWeeksViewModel(earliestUpdateDate: LocalDate, employerName: String, latestPayDate: LocalDate, empId: Int) {
  def earliestUpdateDateInHtml(implicit messages : Messages): String = Dates.formatDate(earliestUpdateDate)
  def latestPayDateInHtml(implicit messages : Messages): String = Dates.formatDate(latestPayDate)
}
