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
import play.api.libs.json.Json
import uk.gov.hmrc.tai.forms.benefits.RemoveCompanyBenefitStopDateForm
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, RemoveCompanyBenefitStopDateConstants}
import uk.gov.hmrc.tai.util.{TaxYearRangeUtil => Dates}
import utils.BaseSpec

class RemoveCompanyBenefitStopDateFormSpec extends BaseSpec {

  val choice = RemoveCompanyBenefitStopDateConstants.StopDateChoice
  private val form = RemoveCompanyBenefitStopDateForm.form

  "RemoveCompanyBenefitStopDateFormSpec" must {
    "return no errors with valid 'yes' choice" in {
      val validYesChoice = Json.obj(choice -> FormValuesConstants.YesValue)
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe Some(FormValuesConstants.YesValue)
    }

    "return no errors with valid 'no' choice" in {
      val validNoChoice = Json.obj(choice -> FormValuesConstants.NoValue)
      val validatedForm = form.bind(validNoChoice)

      validatedForm.errors mustBe empty
      validatedForm.value.get mustBe Some(FormValuesConstants.NoValue)
    }

    "return an error for invalid choice" in {
      val invalidChoice = Json.obj(choice -> "")
      val invalidatedForm = form.bind(invalidChoice)
      val taxYearStart = Dates.formatDate(TaxYear().start)

      invalidatedForm.errors.head.messages mustBe List(
        Messages("tai.benefits.ended.stopDate.radio.error", taxYearStart))
      invalidatedForm.value mustBe None
    }

  }
}
