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

package uk.gov.hmrc.tai.viewModels


import org.joda.time.LocalDate
import uk.gov.hmrc.tai.model.domain.Employment

case class NoCYIncomeTaxErrorViewModel(endDate: Option[String])

object NoCYIncomeTaxErrorViewModel {

  val dateFormat = "d MMMM yyyy"

  def apply(employments: Seq[Employment]): NoCYIncomeTaxErrorViewModel = {
    val endDate = mostRecentEmploymentEndDate(employments)
    NoCYIncomeTaxErrorViewModel(endDate)
  }

  def mostRecentEmploymentEndDate(employments: Seq[Employment]): Option[String] = {
    employments match {
      case Nil => None
      case _ =>
        val endDateSeq: Seq[LocalDate] = employments.flatMap(_.endDate)
        val findLatest = (x: LocalDate, y: LocalDate) => if(x.isAfter(y)) x else y
        endDateSeq.reduceLeftOption(findLatest).map(_.toString(dateFormat))
    }
  }
}
