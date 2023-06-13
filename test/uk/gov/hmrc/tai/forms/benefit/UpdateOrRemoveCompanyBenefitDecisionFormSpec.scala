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

package uk.gov.hmrc.tai.forms.benefit

import play.api.i18n.Messages
import uk.gov.hmrc.tai.forms.benefits.UpdateOrRemoveCompanyBenefitDecisionForm
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants
import uk.gov.hmrc.tai.util.constants.UpdateOrRemoveCompanyBenefitDecisionConstants.{NoIDontGetThisBenefit, YesIGetThisBenefit}
import utils.BaseSpec

class UpdateOrRemoveCompanyBenefitDecisionFormSpec extends BaseSpec {

  val choice = UpdateOrRemoveCompanyBenefitDecisionConstants.DecisionChoice
  private val form = UpdateOrRemoveCompanyBenefitDecisionForm.form

  "UpdateOrRemoveCompanyBenefitDecisionFormSpec" must {
    "return no errors with valid 'yes' choice" in {
      val validYesChoice = Map(choice -> YesIGetThisBenefit)
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe Some(YesIGetThisBenefit)
    }

    "return no errors with valid 'no' choice" in {
      val validNoChoice = Map(choice -> NoIDontGetThisBenefit)
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe Some(NoIDontGetThisBenefit)
    }

    "return an error for invalid choice" in {
      val invalidChoice = Map(choice -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(Messages("tai.benefits.updateOrRemove.decision.radio.error"))
      invalidatedForm.value mustBe None
    }

  }
}
