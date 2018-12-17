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
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{CarBenefit, GiftAidPayments}

class CompanyCarBenefitPairsSpec extends PlaySpec with FakeTaiPlayApplication {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val date = LocalDate.now()
  val emptyCodingComponentsWithCarBenefits = CodingComponentsWithCarBenefits(date, Seq.empty, Seq.empty)

  val employmentId = 787
  val currentCarGrossAmount = 456
  val previousCarBenefitAmount = 555
  val employmentIds = Map(employmentId -> "unused")
  val carBenefitCodingComponent = Seq(CodingComponent(CarBenefit, Some(employmentId), currentCarGrossAmount, ""))
  val companyCar = CompanyCar(100, "", false, None, None, None)
  val genericCarMakeModel = "Car benefit"

  def makeCompanyCar(makeModel: String): CompanyCar = {
    CompanyCar(100, makeModel, false, None, None, None)
  }

  "#apply" should {
    "return no car benefits for empties" in {
      val actual = CompanyCarBenefitPairs(Map.empty, emptyCodingComponentsWithCarBenefits, emptyCodingComponentsWithCarBenefits)
      actual mustBe CompanyCarBenefitPairs(None, None)
    }

    "return no car benefits when there are no CarBenefitCodingComponents" in {
      val codingComponents = Seq(CodingComponent(GiftAidPayments, Some(employmentId), 12345, "unused"))

      val previous = CodingComponentsWithCarBenefits(date, codingComponents, Seq.empty)
      val current = CodingComponentsWithCarBenefits(date, codingComponents, Seq.empty)

      val actual = CompanyCarBenefitPairs(employmentIds, previous, current)
      actual mustBe CompanyCarBenefitPairs(None, None)
    }

    "return a generic car make model if there are no matching id's to the CarBenefitCodingComponent" when {
      val badId = 987

      "there is only a current coding component" in {
        val currentCompanyCarBenefits = Seq(CompanyCarBenefit(badId, currentCarGrossAmount, Seq(companyCar)))
        val expectedCarGrossAmountPairs = Some(CarGrossAmountPairs(currentCarGrossAmount, genericCarMakeModel))
        val expected = CompanyCarBenefitPairs(None, expectedCarGrossAmountPairs)

        val current = CodingComponentsWithCarBenefits(date, carBenefitCodingComponent, currentCompanyCarBenefits)
        val actual = CompanyCarBenefitPairs(employmentIds, emptyCodingComponentsWithCarBenefits, current)

        actual mustBe expected
      }

      "there is only a previous coding component" in {
        val previousCompanyCarBenefits = Seq(CompanyCarBenefit(badId, currentCarGrossAmount, Seq(companyCar)))
        val expectedCarGrossAmountPairs = Some(CarGrossAmountPairs(currentCarGrossAmount, genericCarMakeModel))
        val expected = CompanyCarBenefitPairs(expectedCarGrossAmountPairs, None)

        val previous = CodingComponentsWithCarBenefits(date, carBenefitCodingComponent, previousCompanyCarBenefits)
        val actual = CompanyCarBenefitPairs(employmentIds, previous, emptyCodingComponentsWithCarBenefits)

        actual mustBe expected
      }
    }

    "returns a specific make model when the id matches" in {
      val carMakeModel = "a specific make model"
      val companyCar = makeCompanyCar(carMakeModel)

      val currentCompanyCarBenefits = Seq(CompanyCarBenefit(employmentId, currentCarGrossAmount, Seq(companyCar)))
      val expectedCarGrossAmountPairs = Some(CarGrossAmountPairs(currentCarGrossAmount, carMakeModel))
      val expected = CompanyCarBenefitPairs(None, expectedCarGrossAmountPairs)

      val current = CodingComponentsWithCarBenefits(date, carBenefitCodingComponent, currentCompanyCarBenefits)
      val actual = CompanyCarBenefitPairs(employmentIds, emptyCodingComponentsWithCarBenefits, current)

      actual mustBe expected
    }

    "returns a two car benefit pairs when the previous and current car benefits are not the same" in {
      val previousGrossAmount = 12893
      val expectedPreviousCarGrossAmountPairs = Some(CarGrossAmountPairs(previousGrossAmount, "previous company car"))
      val expectedCurrentCarGrossAmountPairs = Some(CarGrossAmountPairs(currentCarGrossAmount, "current company car"))
      val expected = CompanyCarBenefitPairs(expectedPreviousCarGrossAmountPairs, expectedCurrentCarGrossAmountPairs)

      val previousCompanyCar = makeCompanyCar("previous company car")
      val previousCarBenefitCodingComponent = Seq(CodingComponent(CarBenefit, Some(employmentId), previousGrossAmount, ""))
      val previousCompanyCarBenefits = Seq(CompanyCarBenefit(employmentId, previousGrossAmount, Seq(previousCompanyCar)))

      val currentCompanyCar = makeCompanyCar("current company car")
      val currentCompanyCarBenefits = Seq(CompanyCarBenefit(employmentId, currentCarGrossAmount, Seq(currentCompanyCar)))

      val previous = CodingComponentsWithCarBenefits(date, previousCarBenefitCodingComponent, previousCompanyCarBenefits)
      val current = CodingComponentsWithCarBenefits(date, carBenefitCodingComponent, currentCompanyCarBenefits)

      val actual = CompanyCarBenefitPairs(employmentIds, previous, current)

      actual mustBe expected
    }
  }
}
