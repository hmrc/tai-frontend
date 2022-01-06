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

package uk.gov.hmrc.tai.viewModels.benefit

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

case class CompanyCarChoiceViewModel(carModel: String, carProvider: String)

object CompanyCarChoiceViewModel extends JourneyCacheConstants {

  val missingCarModelMessage = "No company car model found in supplied cache map"
  val missingCarProviderMessage = "No company car provider found in supplied cache map"

  def apply(carDetails: Map[String, String])(implicit messages: Messages): CompanyCarChoiceViewModel = {
    val carModel = carDetails.getOrElse(CompanyCar_CarModelKey, throw new RuntimeException(missingCarModelMessage))
    val carProvider =
      carDetails.getOrElse(CompanyCar_CarProviderKey, throw new RuntimeException(missingCarProviderMessage))
    CompanyCarChoiceViewModel(carModel, carProvider)
  }
}
