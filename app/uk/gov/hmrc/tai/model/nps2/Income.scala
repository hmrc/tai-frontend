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

package hmrc.nps2

import hmrc.nps2.Income.{EmploymentStatus, IncomeType}
import org.slf4j._

case class Income (
                    employmentId: Option[Int],
                    isPrimary: Boolean,
                    incomeType: IncomeType.Value,
                    status: EmploymentStatus,
                    taxDistrict: Option[Int],
                    payeRef: String,
                    name: String,
                    worksNumber: Option[String],
                    taxCode: String,
                    potentialUnderpayment: BigDecimal,
                    employmentRecord: Option[NpsEmployment]
                    )

object Income {
  implicit val log: Logger = LoggerFactory.getLogger(this.getClass)

  sealed trait EmploymentStatus {
    def code: Int
  }

  object EmploymentStatus {
    object Live extends EmploymentStatus { val code = 1 }
    object PotentiallyCeased extends EmploymentStatus { val code = 3 }
    object Ceased extends EmploymentStatus { val code = 2 }
  }

  object IncomeType extends Enumeration {
    val Employment, JobSeekersAllowance, Pension, OtherIncome = Value
  }
}
