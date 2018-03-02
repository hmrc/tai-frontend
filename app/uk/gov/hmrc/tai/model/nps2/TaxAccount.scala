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

import org.joda.time.LocalDate
import uk.gov.hmrc.tai.model.nps2.TaxDetail

/**
 * The root object for the NPS tax account, typically this is read in
 * from the HoD. It is considered less authoritative than the RTI Tax
 * Account data but is always available.
 */
case class TaxAccount (

                        /**
                         * A unique ID to identify a tax account. You cannot really use this for
                         * anything practical as we don't know if NPS will populate it or not.
                         */
                        id: Option[Long],

                        /**
                         * Date the tax pos
                          * ition was confirmed (will be the tax code
                         * date). May be absent, though we don't know what this means.
                         */
                        date: Option[LocalDate],

                        /**
                         * Total tax across all income sources
                         */
                        tax: BigDecimal,

                        taxObjects: Map[TaxObject.Type.Value, TaxDetail] = Map.empty,

                        incomes: Seq[Income] = Nil

                        ) {
  def withEmployments(emps: Seq[NpsEmployment]): TaxAccount = {
    val newIncomes = incomes.map{ i =>
      emps.find(x => Some(x.sequenceNumber) == i.employmentId) match {
        case Some(e) => i.copy(worksNumber=e.worksNumber,employmentRecord = Some(e))
        case None => i
      }
    }
    this.copy( incomes = newIncomes )
  }
}

