/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}

class CompanyCarMakeModelSpec extends PlaySpec {

  val companyCarBenefit10 =
    CompanyCarBenefit(10, 1000, List(CompanyCar(10, "Make Model1", true, None, None, None)), Some(1))
  val companyCarBenefit12 =
    CompanyCarBenefit(12, 1000, List(CompanyCar(10, "Make Model2", true, None, None, None)), Some(1))
  val companyCarBenefitTwoCars = CompanyCarBenefit(
    1,
    1000,
    List(CompanyCar(10, "Make Model1", true, None, None, None), CompanyCar(12, "Make Model1", true, None, None, None)),
    Some(1))
  val companyBenefitNoCars = CompanyCarBenefit(2, 1000, List(), Some(1))
  val companyCarBenefits = Seq(companyCarBenefit10, companyCarBenefit12, companyCarBenefitTwoCars, companyBenefitNoCars)

  "CompanyCarMakeModel" must {
    "return company car model from list of company car benefits" when {
      "provided with employment id with company car benefit" in {
        val result = CompanyCarMakeModel.description(10, companyCarBenefits)
        result mustBe Some("Make Model1")
      }
    }

    "return generic car benefit message" when {
      "provided the same employment has more than one company cars" in {
        val result = CompanyCarMakeModel.description(1, companyCarBenefits)
        result mustBe Some("Car Benefit")
      }
    }

    "return None" when {
      "employment id do not have any associated company car benefit" in {
        val result = CompanyCarMakeModel.description(16, companyCarBenefits)
        result mustBe None
      }

      "employment id do not have any company cars associated with their car benefit" in {
        val result = CompanyCarMakeModel.description(2, companyCarBenefits)
        result mustBe None
      }
    }

  }
}
