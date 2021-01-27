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

package uk.gov.hmrc.tai.viewModels

import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}

case class JrsClaimsViewModel(firstClaimDate: String, employers: List[Employers])

object JrsClaimsViewModel {

  def sortClaimData(yearAndMonthList: List[YearAndMonth], jrsClaimFromDate: LocalDate) = {

    val dateTypeList =
      for (data <- yearAndMonthList if !LocalDate.parse(data.yearAndMonth).isBefore(jrsClaimFromDate))
        yield (LocalDate.parse(data.yearAndMonth))

    val sortedDateTypeList = dateTypeList.sortBy(_.toDate)

    for (a <- sortedDateTypeList)
      yield (YearAndMonth(s"${a.monthOfYear().getAsText} ${a.year().getAsString}"))
  }

  def apply(jrsClaims: JrsClaims, jrsClaimFromDate: LocalDate): JrsClaimsViewModel = {

    val employersList = for (employer <- jrsClaims.employers)
      yield (Employers(employer.name, employer.employerReference, sortClaimData(employer.claims, jrsClaimFromDate)))

    JrsClaimsViewModel(
      s"${jrsClaimFromDate.monthOfYear().getAsText} ${jrsClaimFromDate.year().getAsString}",
      employersList.sortBy(_.name))
  }

}
