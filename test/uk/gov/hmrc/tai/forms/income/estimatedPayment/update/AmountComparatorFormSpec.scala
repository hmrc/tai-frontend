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

package uk.gov.hmrc.tai.forms.income.estimatedPayment.update

import java.time.LocalDate
import uk.gov.hmrc.tai.forms.AmountComparatorForm
import utils.BaseSpec

class AmountComparatorFormSpec extends BaseSpec {

  "AmountComparatorForm" must {

    "return no errors when a valid monetary amount is entered" in {
      val validMonetaryAmount = Map("income" -> "11000")
      val validatedForm = form.bind(validMonetaryAmount)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AmountComparatorForm(Some("11000")))
    }

    "return no errors when a valid monetary amount is entered with non numeric characters" in {
      val validMonetaryAmount = Map("income" -> "£11,000")
      val validatedForm = form.bind(validMonetaryAmount)

      validatedForm.errors mustBe empty
      validatedForm.value mustBe Some(AmountComparatorForm(Some("11000")))
    }
    "return an error for an invalid monetary amount" in {
      val invalidChoice = Map("income" -> "11,00")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(
        messagesApi("error.tai.update.estimatedTaxableIncome.input.invalid")
      )
      invalidatedForm.value mustBe None
    }

    "return an error for no input value" in {
      val invalidChoice = Map("income" -> "")
      val invalidatedForm = form.bind(invalidChoice)

      invalidatedForm.errors.head.messages mustBe List(messagesApi("tai.irregular.error.blankValue"))
      invalidatedForm.value mustBe None
    }

    "return an error when new amount is less than current amount" in {
      val newAmount = Map("income" -> "9000")
      val invalidatedForm = form.bind(newAmount)

      invalidatedForm.errors.head.messages mustBe List(
        messagesApi("tai.irregular.error.error.incorrectTaxableIncome", taxablePayYTD, currentDate)
      )
      invalidatedForm.value mustBe None

    }

    "return an error when new amount is greater than max length" in {
      val newAmount = Map("income" -> "1000000000")
      val invalidatedForm = form.bind(newAmount)

      invalidatedForm.errors.head.messages mustBe List(messagesApi("error.tai.updateDataEmployment.maxLength"))
      invalidatedForm.value mustBe None

    }
  }

  val currentDate = LocalDate.now().toString()
  val taxablePayYTD = BigDecimal(10000)
  val form = AmountComparatorForm.createForm(Some(currentDate), Some(taxablePayYTD))

}
