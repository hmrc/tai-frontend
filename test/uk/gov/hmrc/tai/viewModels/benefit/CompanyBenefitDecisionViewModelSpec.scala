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

import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import utils.BaseSpec

class CompanyBenefitDecisionViewModelSpec extends BaseSpec {

  "CompanyBenefitDecisionViewModel" must {
    "show the word benefit once when the word benefit is part of the benefit name " in {
      val benefitType = "NonCashBenefit"
      val formattedBenefitName = "Non-cash"
      val viewModel =
        CompanyBenefitDecisionViewModel(benefitType, employerName, UpdateOrRemoveCompanyBenefitDecisionForm.form)

      viewModel.benefitName mustBe formattedBenefitName
    }

    "show the word benefit once when the word benefits is part of the benefit name " in {
      val benefitType = "ServiceBenefit"
      val formattedBenefitName = "Service"
      val viewModel =
        CompanyBenefitDecisionViewModel(benefitType, employerName, UpdateOrRemoveCompanyBenefitDecisionForm.form)

      viewModel.benefitName mustBe formattedBenefitName

    }

    lazy val employerName = "company name"

  }
}
