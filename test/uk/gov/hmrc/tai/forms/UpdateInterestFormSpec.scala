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

package uk.gov.hmrc.tai.forms

import uk.gov.hmrc.tai.forms.incomes.bbsi.UpdateInterestForm
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json

class UpdateInterestFormSpec extends PlaySpec with OneAppPerSuite with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "Update Income Form" must {
    "return no error with valid data" in {
      val untaxedInterest = form.bind(Json.obj("untaxedInterest" -> "1,000"))

      untaxedInterest.errors mustBe empty
    }

    "return error" when {
      "passed empty value" in {
        val untaxedInterest = form.bind(Json.obj("untaxedInterest" -> ""))

        untaxedInterest.errors must contain(FormError("untaxedInterest", List(Messages("tai.bbsi.update.form.interest.blank"))))
      }

      "passed characters" in {
        val untaxedInterest = form.bind(Json.obj("untaxedInterest" -> "dasdas"))

        untaxedInterest.errors must contain(FormError("untaxedInterest", Messages("tai.bbsi.update.form.interest.isCurrency")))
      }

      "entered (,) at wrong place" in {
        val untaxedInterest = form.bind(Json.obj("untaxedInterest" -> "1,00"))

        untaxedInterest.errors must contain(FormError("untaxedInterest", Messages("tai.bbsi.update.form.interest.isCurrency")))
      }

      "passed decimal value" in {
        val untaxedInterest = form.bind(Json.obj("untaxedInterest" -> "1.00"))

        untaxedInterest.errors must contain(FormError("untaxedInterest", Messages("tai.bbsi.update.form.interest.wholeNumber")))
      }

    }

  }

  private lazy val form = UpdateInterestForm.form
}
