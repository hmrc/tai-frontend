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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.TaxAccountCalculator
import utils.BaseSpec

class TaxFreeInfoSpec extends BaseSpec {

  val date = "Date"
  val taxAccountCalculatorMock = mock[TaxAccountCalculator]
  val taxFreeAmount = 123

  def createCodingComponent(allowance: AllowanceComponentType, allowanceAmount: BigDecimal) =
    CodingComponent(allowance, Some(123), allowanceAmount, allowance.toString())

  "#apply" should {
    when(taxAccountCalculatorMock.taxFreeAmount(any())).thenReturn(taxFreeAmount)

    "return a TaxFreeInfo" in {
      when(taxAccountCalculatorMock.taxFreeAmount(Matchers.eq(Seq.empty))).thenReturn(0)

      val expected = TaxFreeInfo(date, 0, 0)
      TaxFreeInfo(date, Seq.empty, taxAccountCalculatorMock) mustBe expected
    }

    "calculate the annual tax free amount" in {
      val codingComponents =
        Seq(CodingComponent(MarriageAllowanceReceived, Some(taxFreeAmount), 5885, "MarriageAllowanceReceived"))
      val expected = TaxFreeInfo(date, taxFreeAmount, 0)

      TaxFreeInfo(date, codingComponents, taxAccountCalculatorMock) mustBe expected
    }

    "calculate the personal allowance" in {
      val codingComponents =
        Seq(CodingComponent(PersonalAllowancePA, Some(taxFreeAmount), 11850, "MarriageAllowanceReceived"))
      val expected = TaxFreeInfo(date, taxFreeAmount, 11850)

      TaxFreeInfo(date, codingComponents, taxAccountCalculatorMock) mustBe expected
    }

    "ignores non personal allowances when accumulating taxable amount" in {
      val marriageAllowanceRecieved = createCodingComponent(MarriageAllowanceReceived, 555)

      val allowancePa = createCodingComponent(PersonalAllowancePA, 100)
      val allowanceAgedPAA = createCodingComponent(PersonalAllowanceAgedPAA, 200)
      val allowanceElderlyPAE = createCodingComponent(PersonalAllowanceElderlyPAE, 300)

      val codingComponents = Seq(marriageAllowanceRecieved, allowancePa, allowanceAgedPAA, allowanceElderlyPAE)

      val actual = TaxFreeInfo(date, codingComponents, taxAccountCalculatorMock)
      val expected = TaxFreeInfo(date, taxFreeAmount, 600)

      actual mustBe expected
    }
  }
}
