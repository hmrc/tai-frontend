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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages

class CombineCompanyCarBenefitsSpec extends PlaySpec with FakeTaiPlayApplication {
  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val carGrossAmount = 12345

  "combine" should {
    "contain nothing if there are no benefits" in {
      val actual = CombineCompanyCarBenefits(CompanyCarBenefitPairs(None, None))

      actual mustBe Seq.empty
    }

    "only contains current car benefits if previous is empty" in {
      val carMakeModel = "a specific make model"
      val expected = Seq(CombinedCarGrossAmountPairs(previous = 0, carGrossAmount, carMakeModel))

      val carGrossAmountPairs = Some(CarGrossAmountPairs(carGrossAmount, carMakeModel))
      val actual = CombineCompanyCarBenefits(CompanyCarBenefitPairs(None, carGrossAmountPairs))

      actual mustBe expected
    }

    "only contains previous car benefits if current is empty" in {
      val carMakeModel = "a specific make model"
      val expected = Seq(CombinedCarGrossAmountPairs(carGrossAmount, current = 0, carMakeModel))

      val carGrossAmountPairs = Some(CarGrossAmountPairs(carGrossAmount, carMakeModel))
      val actual = CombineCompanyCarBenefits(CompanyCarBenefitPairs(carGrossAmountPairs, None))

      actual mustBe expected
    }

    "merges the previous and current car benefits if the descriptions are the same" in {
      val carMakeModel = "a specific make model"
      val expected = Seq(CombinedCarGrossAmountPairs(carGrossAmount, carGrossAmount, carMakeModel))

      val carGrossAmountPairs = Some(CarGrossAmountPairs(carGrossAmount, carMakeModel))
      val actual = CombineCompanyCarBenefits(CompanyCarBenefitPairs(carGrossAmountPairs, carGrossAmountPairs))

      actual mustBe expected
    }

    "has two previous and current car benefits if the descriptions are not equal" in {
      val carMakeModel = "a specific make model"
      val aDifferentGrossAmount = 123456
      val expected = Seq(
        CombinedCarGrossAmountPairs(aDifferentGrossAmount, 0, carMakeModel),
        CombinedCarGrossAmountPairs(0, carGrossAmount, "a different make model")
      )

      val carGrossAmountPairs1 = Some(CarGrossAmountPairs(aDifferentGrossAmount, carMakeModel))
      val carGrossAmountPairs2 = Some(CarGrossAmountPairs(carGrossAmount, "a different make model"))

      val actual = CombineCompanyCarBenefits(CompanyCarBenefitPairs(carGrossAmountPairs1, carGrossAmountPairs2))

      actual mustBe expected
    }
  }
}
