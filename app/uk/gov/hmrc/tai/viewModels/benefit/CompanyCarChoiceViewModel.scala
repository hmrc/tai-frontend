/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.benefit

import uk.gov.hmrc.tai.util.constants.journeyCache._

case class CompanyCarChoiceViewModel(carModel: String, carProvider: String)

object CompanyCarChoiceViewModel {

  val missingCarModelMessage    = "No company car model found in supplied cache map"
  val missingCarProviderMessage = "No company car provider found in supplied cache map"

  def apply(carDetails: Map[String, String]): CompanyCarChoiceViewModel = {
    val carModel    =
      carDetails.getOrElse(CompanyCarConstants.CarModelKey, throw new RuntimeException(missingCarModelMessage))
    val carProvider =
      carDetails.getOrElse(CompanyCarConstants.CarProviderKey, throw new RuntimeException(missingCarProviderMessage))
    CompanyCarChoiceViewModel(carModel, carProvider)
  }
}
