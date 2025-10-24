/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tai.forms

import org.scalatest.OptionValues
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.BaseSpec

class PayeRefFormSpec extends BaseSpec with OptionValues {

  private implicit val messages: Messages = messagesApi.preferred(FakeRequest())

  "calling the PayeRefForm" should {

    "return the form with errors" when {

      "the payeReference field is empty" in {
        val form = PayeRefForm.form("company", "employment").bind(Map("payeReference" -> ""))
        form.hasErrors mustBe true
        form.errors.head.message mustBe messages("tai.payeRefForm.employment.required", "company")
      }

      "the payeReference field is only whitespace" in {
        val form = PayeRefForm.form("company", "employment").bind(Map("payeReference" -> "   "))
        form.hasErrors mustBe true
        form.errors.head.message mustBe messages("tai.payeRefForm.employment.required", "company")
      }

      "the payeReference field does not match NNN/XXXXXXXXXX format" in {
        val invalids = Seq(
          "ABC/123",
          "12/ABC",
          "123ABC",
          "123/",
          "123/ABCDEFGHIJK",
          "123/ABC_DEF",
          "123/ABC-DEF",
          "123/abc def"
        )

        invalids.foreach { v =>
          val form = PayeRefForm.form("company", "employment").bind(Map("payeReference" -> v))
          withClue(s"Value '$v' should be invalid") {
            form.hasErrors mustBe true
            form.errors.head.message mustBe messages("tai.payeRefForm.employment.format", "company")
          }
        }
      }
    }

    "return the form without any errors" when {

      "the payeReference matches NNN/ALPHANUM(1..10)" in {
        val valids = Seq(
          "123/A",
          "123/ABC123",
          "123/ABCDEFGHIJ",
          "123/abc123"
        )

        valids.foreach { v =>
          val form = PayeRefForm.form("company", "employment").bind(Map("payeReference" -> v))
          withClue(s"Value '$v' should be valid") {
            form.hasErrors mustBe false
            form.value.value mustBe v
          }
        }
      }
    }
  }

  "use pension-specific messages when journey is pension" in {

    val emptyForm = PayeRefForm.form("company", "pension").bind(Map("payeReference" -> ""))
    emptyForm.hasErrors mustBe true
    emptyForm.errors.head.message mustBe messages("tai.payeRefForm.pension.required", "company")

    val invalidForm = PayeRefForm.form("company", "pension").bind(Map("payeReference" -> "ABC/123"))
    invalidForm.hasErrors mustBe true
    invalidForm.errors.head.message mustBe messages("tai.payeRefForm.pension.format", "company")

    val validForm = PayeRefForm.form("company", "pension").bind(Map("payeReference" -> "123/ABC123"))
    validForm.hasErrors mustBe false
    validForm.value.value mustBe "123/ABC123"

  }
}
