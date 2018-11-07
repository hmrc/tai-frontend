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

package uk.gov.hmrc.tai.forms.constraints

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import uk.gov.hmrc.tai.forms.YesNoTextEntryForm
import uk.gov.hmrc.tai.forms.constaints.TelephoneNumberConstraint.telephoneNumberSizeConstraint
import uk.gov.hmrc.tai.util.constants.FormValuesConstants

class TelephoneNumberConstraintSpec extends PlaySpec with OneAppPerSuite with I18nSupport with FormValuesConstants {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "YesNoTextEntryFormSpec" must {
    "return no errors with valid 'yes' choice and text field content" in {
      val validYesChoice = Json.obj(YesNoChoice -> YesValue, YesNoTextEntry -> "123456789")
      val validatedForm = form.bind(validYesChoice)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(YesNoTextEntryForm(Some(YesValue), Some("123456789")))
    }

    "return errors" when {
      "phone number is less than 8 digits" in {
        val invalidPhoneNumberChoice = Json.obj(YesNoChoice -> YesValue, YesNoTextEntry -> "123456")
        val invalidatedForm = form.bind(invalidPhoneNumberChoice)

        invalidatedForm.errors.head.messages mustBe List(Messages("tai.canWeContactByPhone.telephone.invalid"))
        invalidatedForm.value mustBe None
      }

      "phone number is less than more than 30 digits" in {
        val invalidPhoneNumberChoice = Json.obj(YesNoChoice -> YesValue, YesNoTextEntry -> "123456123456123456123456123456123456")
        val invalidatedForm = form.bind(invalidPhoneNumberChoice)

        invalidatedForm.errors.head.messages mustBe List(Messages("tai.canWeContactByPhone.telephone.invalid"))
        invalidatedForm.value mustBe None
      }

    }
  }

  private val form = YesNoTextEntryForm.form(
    Messages("tai.canWeContactByPhone.YesNoChoice.empty"),
    Messages("tai.canWeContactByPhone.telephone.empty"),
    Some(telephoneNumberSizeConstraint))
}