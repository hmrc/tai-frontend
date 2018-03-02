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

package uk.gov.hmrc.tai.forms.income.bbsi

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import uk.gov.hmrc.tai.util.FormValuesConstants


class BankAccountClosingInterestFormSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with FormValuesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "BankAccountClosingInterestFormSpec" must {

    "return no errors" when {
      "given a valid 'yes' choice and interest amount" in {
        val validYesChoice = Json.obj(choice -> YesValue, interest -> "123")
        val validatedForm = form.bind(validYesChoice)

        validatedForm.errors mustBe empty
        validatedForm.value mustBe Some(BankAccountClosingInterestForm(Some(YesValue), Some("123")))
      }

      "given a valid 'no' choice and no interest amount" in {
        val validNoChoice = Json.obj(choice -> NoValue, interest -> "")
        val validatedForm = form.bind(validNoChoice)

        validatedForm.errors mustBe empty
        validatedForm.value mustBe Some(BankAccountClosingInterestForm(Some(NoValue), None))
      }

      "given a valid 'no' choice and interest amount as space" in {
        val validNoChoice = Json.obj(choice -> NoValue, interest -> " ")
        val validatedForm = form.bind(validNoChoice)

        validatedForm.errors mustBe empty
        validatedForm.value mustBe Some(BankAccountClosingInterestForm(Some(NoValue), None))
      }

      "given a valid 'no' choice and interest amount" in {
        val validNoChoice = Json.obj(choice -> NoValue, interest -> "123.45")
        val validatedForm = form.bind(validNoChoice)

        validatedForm.errors mustBe empty
        validatedForm.value mustBe Some(BankAccountClosingInterestForm(Some(NoValue), None))
      }
    }

    "return an error" when {

      "given an invalid choice" in {
        val invalidChoice = Json.obj(choice -> "", interest -> "")
        val invalidatedForm = form.bind(invalidChoice)

        invalidatedForm.errors.head.messages mustBe List(Messages("tai.closeBankAccount.closingInterest.error.selectOption"))
        invalidatedForm.value mustBe None
      }

      "given an invalid choice with empty values" in {
        val invalidChoice = Json.obj(interest -> "")
        val invalidatedForm = form.bind(invalidChoice)

        invalidatedForm.errors.head.messages mustBe List(Messages("tai.closeBankAccount.closingInterest.error.selectOption"))
        invalidatedForm.value mustBe None
      }

      "given a valid 'yes' choice but no interest amount" in {
        val invalidYesChoice = Json.obj(choice -> Some(YesValue), interest -> "")
        val invalidatedForm = form.bind(invalidYesChoice)

        invalidatedForm.errors.head.messages mustBe List(Messages("tai.closeBankAccount.closingInterest.error.blank"))
        invalidatedForm.value mustBe None
      }

      "given a valid 'yes' choice but a non numeric interest amount" in {
        val invalidYesChoice = Json.obj(choice -> Some(YesValue), interest -> "abc")
        val invalidatedForm = form.bind(invalidYesChoice)

        invalidatedForm.errors.head.messages mustBe List(Messages("tai.bbsi.update.form.interest.isCurrency"))
        invalidatedForm.value mustBe None
      }

      "given a valid 'yes' choice but decimal interest amount" in {
        val invalidYesChoice = Json.obj(choice -> Some(YesValue), interest -> "123.45")
        val invalidatedForm = form.bind(invalidYesChoice)

        invalidatedForm.errors.head.messages mustBe List(Messages("tai.bbsi.update.form.interest.wholeNumber"))
        invalidatedForm.value mustBe None
      }
    }
  }

  val choice = BankAccountClosingInterestForm.ClosingInterestChoice
  val interest = BankAccountClosingInterestForm.ClosingInterestEntry

  private lazy val form = BankAccountClosingInterestForm.form
}
