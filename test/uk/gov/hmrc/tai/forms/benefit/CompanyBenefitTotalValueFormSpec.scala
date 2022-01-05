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

package uk.gov.hmrc.tai.forms.benefit

import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import uk.gov.hmrc.tai.forms.benefits.CompanyBenefitTotalValueForm
import utils.BaseSpec

class CompanyBenefitTotalValueFormSpec extends BaseSpec {

  "Company Benefit Total Value Form" must {
    "return no error with valid data" in {
      val totalValue = form.bind(Json.obj("totalValue" -> "1,000"))

      totalValue.errors mustBe empty
    }

    "return error" when {
      "passed empty value" in {
        val totalValue = form.bind(Json.obj("totalValue" -> ""))

        totalValue.errors must contain(FormError("totalValue", List(Messages("tai.interest.blank"))))
      }

      "passed characters" in {
        val totalValue = form.bind(Json.obj("totalValue" -> "dasdas"))

        totalValue.errors must contain(FormError("totalValue", Messages("tai.interest.isCurrency")))
      }

      "entered (,) at wrong place" in {
        val totalValue = form.bind(Json.obj("totalValue" -> "1,00"))

        totalValue.errors must contain(FormError("totalValue", Messages("tai.interest.isCurrency")))
      }

    }

  }

  private lazy val form = CompanyBenefitTotalValueForm.form
}
