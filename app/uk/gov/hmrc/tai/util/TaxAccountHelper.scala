/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

import uk.gov.hmrc.tai.model.domain.IabdDetails

import java.time.LocalDate

object TaxAccountHelper {

  def getIabdLatestEstimatedIncome(
    iabds: Seq[IabdDetails],
    maybeTaxAccountDate: Option[LocalDate],
    maybeEmpId: Option[Int]
  ): Option[BigDecimal] = {
    // If no date is present in tax account, used a date in past to allow the new estimate from iabd to be picked up if any
    val taxAccountDate = maybeTaxAccountDate.getOrElse(LocalDate.now.minusYears(1))
    iabds
      .sortBy(_.captureDate)
      .findLast { iabd => // find most recent iabd update
        iabd.`type`.contains(IabdDetails.newEstimatedPayCode) && // only keep new estimated income
        maybeEmpId.fold(true)(empId =>
          iabd.employmentSequenceNumber.contains(empId)
        ) && // if employment not provided, get iabd for any employment
        iabd.grossAmount.isDefined &&
        iabd.captureDate
          .getOrElse(LocalDate.now)
          .isAfter(taxAccountDate) // the iabd update needs to be more recent than the value from tax account
      }
      .map(_.grossAmount.get)
  }
}
