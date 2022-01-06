/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit

object CompanyCarMakeModel {
  def description(employmentId: Int, companyCarBenefits: Seq[CompanyCarBenefit]): Option[String] = {
    def modelFromBenefits(employersCarBenefits: CompanyCarBenefit): Option[String] =
      if (employersCarBenefits.companyCars.size > 1) {
        Some("Car Benefit")
      } else {
        employersCarBenefits.companyCars.headOption.map(_.makeModel)
      }

    for {
      carBenefits: CompanyCarBenefit <- companyCarBenefits.find(_.employmentSeqNo == employmentId)
      model                          <- modelFromBenefits(carBenefits)
    } yield model

  }
}
