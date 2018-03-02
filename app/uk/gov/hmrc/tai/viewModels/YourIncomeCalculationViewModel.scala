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

import uk.gov.hmrc.tai.model.rti.RtiPayment
import org.joda.time.LocalDate
import uk.gov.hmrc.tai.model.EditableDetails

/**
  * Created by dev-01 on 06/07/16.
  */
case class YourIncomeCalculationViewModel(
 employerName: String,
 isPension: Boolean,
 incomeCalculationMsg: String,
 incomeCalculationEstimateMsg: Option[String] = None,
 payrollMsg: Option[String] = None,
 employmentPayments: Seq[RtiPayment],
 empId: Int,
 hasPrevious: Boolean,
 totalNotEqualMessage: Option[String] = None,
 editableDetails: EditableDetails,
 rtiDown: Boolean,
 employmentStatus: Option[Int],
 endDate: Option[LocalDate])
