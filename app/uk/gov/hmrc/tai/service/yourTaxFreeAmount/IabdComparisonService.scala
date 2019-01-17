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

package uk.gov.hmrc.tai.service.yourTaxFreeAmount

import com.google.inject.Inject
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{AllowancesAndDeductionPairs, CodingComponentsWithCarBenefits}

class IabdComparisonService @Inject()() {

  def allowancesAndDeductions(previous: Option[CodingComponentsWithCarBenefits],
                              current: CodingComponentsWithCarBenefits,
                              employmentIds: Map[Int, String])(implicit message: Messages): AllowancesAndDeductionPairs = {

    val previousCodingComponents: Seq[CodingComponent] = previous.fold(Seq.empty[CodingComponent])(_.codingComponents)

    AllowancesAndDeductionPairs.fromCodingComponents(previousCodingComponents, current.codingComponents)
  }
}
