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

package uk.gov.hmrc.tai.util

import controllers.FakeTaiPlayApplication
import mocks.TaxAccountCalculatorMock
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

class TaxFreeInfoSpec extends PlaySpec with FakeTaiPlayApplication {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages
  val date = "Date"

  def createCodingComponent(allowance: AllowanceComponentType, allowanceAmount: BigDecimal) = {
    CodingComponent(allowance, Some(123), allowanceAmount, allowance.toString())
  }

  "#apply" should {
    "return a TaxFreeInfo" in {
      val actual = TaxFreeInfo(date, Seq.empty)
      val expected = TaxFreeInfo(date, 0, 0)
      actual mustBe expected
    }

    "calculate the annual tax free amount" in {
      val codingComponents = Seq(CodingComponent(MarriageAllowanceReceived, Some(123), 5885, "MarriageAllowanceReceived"))

      val actual = TaxFreeInfo(date, codingComponents)
      val expected = TaxFreeInfo(date, 5885, 0)

      actual mustBe expected
    }

    "calculate the personal allowance" in {
      val codingComponents = Seq(CodingComponent(PersonalAllowancePA, Some(123), 11850, "MarriageAllowanceReceived"))

      val actual = TaxFreeInfo(date, codingComponents)
      val expected = TaxFreeInfo(date, 11850, 11850)

      actual mustBe expected
    }

    "ignores non personal allowances when accumulating taxable amount" in {
      val marriageAllowanceRecieved = createCodingComponent(MarriageAllowanceReceived, 555)

      val allowancePa = createCodingComponent(PersonalAllowancePA, 100)
      val allowanceAgedPAA = createCodingComponent(PersonalAllowanceAgedPAA, 200)
      val allowanceElderlyPAE = createCodingComponent(PersonalAllowanceElderlyPAE, 300)

      val codingComponents = Seq(marriageAllowanceRecieved, allowancePa, allowanceAgedPAA, allowanceElderlyPAE)

      val actual = TaxFreeInfo(date, codingComponents)
      val expected = TaxFreeInfo(date, 1155, 600)

      actual mustBe expected
    }
  }
}