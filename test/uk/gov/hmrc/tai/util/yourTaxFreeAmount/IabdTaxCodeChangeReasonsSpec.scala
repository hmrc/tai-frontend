/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.FakeTaiPlayApplication
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.tax.ConcessionalRelief

class IabdTaxCodeChangeReasonsSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi = app.injector.instanceOf[MessagesApi]

  val taxFreeInfo = TaxFreeInfo("12-12-2015", 2000, 1000)
  val jobExpensesIncrease = CodingComponentPair(JobExpenses, Some(2), Some(50), Some(100))
  val carBenefitIncrease = CodingComponentPair(CarBenefit, Some(1), Some(1000), Some(2000))
  val taxCodeChange = mock[TaxCodeChange]
  val iabdTaxCodeChangeReasons = new IabdTaxCodeChangeReasons

  "iabdReasons method" must {
    "have no reasons" when {
      "there are no allowances and deductions" in {
        val pairs = AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
        val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

        reasons mustBe Seq.empty
      }

      "there is no previous amount" in {
        val noPreviousAmount = CodingComponentPair(JobExpenses, None, None, Some(123))
        val pairs = AllowancesAndDeductionPairs(Seq(noPreviousAmount), Seq.empty)

        val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

        reasons mustBe Seq.empty
      }

      "there is no current amount" in {
        val noCurrentAmount = CodingComponentPair(JobExpenses, None, Some(123), None)
        val pairs = AllowancesAndDeductionPairs(Seq(noCurrentAmount), Seq.empty)

        val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

        reasons mustBe Seq.empty
      }
    }

    "give multiple reasons for a tax code change" in {
      val pairs = AllowancesAndDeductionPairs(Seq(jobExpensesIncrease), Seq(carBenefitIncrease))
      val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

      reasons mustBe Seq(
        "Your Job expenses have been updated",
        "Your Car benefit has been updated"
      )
    }

    iabdTaxCodeChangeReasons.hasBeenAllowances foreach {
      case (taxComponentType: TaxComponentType) =>
        s"have the text 'has been updated' for the benefit $taxComponentType" in {
          val benefit = CodingComponentPair(taxComponentType, Some(2), Some(50), Some(100))

          val pairs = AllowancesAndDeductionPairs(Seq.empty, Seq(benefit))
          val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

          reasons.head must include("has been updated")
        }
    }

    iabdTaxCodeChangeReasons.haveBeenAllowances foreach {
      case (taxComponentType: TaxComponentType) =>
        s"have the text 'have been updated' for the benefit $taxComponentType" in {
          val benefit = CodingComponentPair(taxComponentType, Some(2), Some(50), Some(100))

          val pairs = AllowancesAndDeductionPairs(Seq.empty, Seq(benefit))
          val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

          reasons.head must include("have been updated")
        }
    }

    Seq(
      CommunityInvestmentTaxCredit,
      DoubleTaxationRelief,
      ForeignPensionAllowance
    ) foreach {
      case (taxComponentType: TaxComponentType) =>
        s"have the generic text for any other benefit such as $taxComponentType" in {
          val benefit = CodingComponentPair(taxComponentType, Some(2), Some(50), Some(100))

          val pairs = AllowancesAndDeductionPairs(Seq.empty, Seq(benefit))
          val reasons = iabdTaxCodeChangeReasons.reasons(pairs)

          reasons mustBe Seq(messagesApi("taxCode.change.yourTaxCodeChanged.paragraph"))
        }
    }

  }
}
