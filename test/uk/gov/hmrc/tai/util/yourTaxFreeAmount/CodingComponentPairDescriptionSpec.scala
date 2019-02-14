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
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.{CarBenefit, GiftAidPayments}
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}

class CodingComponentPairDescriptionSpec extends PlaySpec with FakeTaiPlayApplication {
  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "#description" should {
    "return a human readable coding component and pass through the current and previous amounts" in {
      val codingComponentPair = CodingComponentPair(GiftAidPayments, None, Some(456), Some(789))
      val actual = CodingComponentPairDescription(codingComponentPair, employmentIds = Map.empty, companyCarBenefits = Seq.empty)

      actual mustBe CodingComponentPairDescription("Gift Aid Payments", 456, 789)
    }

    "return a human readable coding component" when {
      "employment id is found in the employment names map" in {
        val id = 123

        val employmentIds = Map(id -> "123")
        val codingComponentPair = CodingComponentPair(GiftAidPayments, Some(id), Some(456), Some(789))
        val actual = CodingComponentPairDescription(codingComponentPair, employmentIds, companyCarBenefits = Seq.empty)

        actual.description mustBe "Gift Aid Payments from 123"
      }

      "there is a car benefit" should {
        "display a generic car benefit message when there is no matching ID" in {
          val codingComponentPair = CodingComponentPair(CarBenefit, Some(123), Some(456), Some(789))
          val companyCarBenefit = Seq(CompanyCarBenefit(456, 123, Seq.empty))
          val actual = CodingComponentPairDescription(codingComponentPair, employmentIds = Map.empty, companyCarBenefit)

          actual.description mustBe "Car benefit"
        }

        "display the car make model when there is a matching ID" in {
          val id = 123

          val employmentIds = Map(id -> "123")
          val codingComponentPair = CodingComponentPair(CarBenefit, Some(id), Some(456), Some(789))
          val companyCar = CompanyCar(98, "Make Model", false, None, None, None)
          val companyCarBenefit = Seq(CompanyCarBenefit(id, 45678, Seq(companyCar)))
          val actual = CodingComponentPairDescription(codingComponentPair, employmentIds, companyCarBenefit)

          actual.description mustBe "Make Model from 123"
        }
      }
    }
  }
}
