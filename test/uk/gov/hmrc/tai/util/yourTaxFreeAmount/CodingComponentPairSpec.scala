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
import uk.gov.hmrc.tai.model.domain.{CarBenefit, GiftAidPayments}
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}

class CodingComponentPairSpec extends PlaySpec with FakeTaiPlayApplication {
  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "description" should {
    "return a human readable coding component" in {
      val codingComponentPair = CodingComponentPair(GiftAidPayments, None, 456, 789)
      val description = codingComponentPair.description(Map.empty, Seq.empty)

      description mustBe "Gift Aid Payments"
    }

    "return a human readable coding component" when {
      "employment id is found in the employment names map" in {
        val id = 123

        val employmentIds = Map(id -> "123")
        val codingComponentPair = CodingComponentPair(GiftAidPayments, Some(id), 456, 789)
        val description = codingComponentPair.description(employmentIds, Seq.empty)

        description mustBe "Gift Aid Payments from 123"
      }

      "there is a car benefit" should {
        "display a generic car benefit message when there is no matching ID" in {
          val codingComponentPair = CodingComponentPair(CarBenefit, Some(123), 456, 789)
          val companyCarBenefit = Seq(CompanyCarBenefit(456, 123, Seq.empty))
          val description = codingComponentPair.description(Map.empty, companyCarBenefit)

          description mustBe "Car benefit"
        }

        "display the car make model when there is a matching ID" in {
          val id = 123

          val employmentIds = Map(id -> "123")
          val codingComponentPair = CodingComponentPair(CarBenefit, Some(id), 456, 789)
          val companyCar = CompanyCar(98, "Make Model", false, None, None, None)
          val companyCarBenefit = Seq(CompanyCarBenefit(id, 45678, Seq(companyCar)))
          val description = codingComponentPair.description(employmentIds, companyCarBenefit)

          description mustBe "Make Model from 123"
        }
      }
    }
  }
}
