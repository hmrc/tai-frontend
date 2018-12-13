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
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.CarBenefit
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent

class CompanyCarBenefitPairsSpec extends PlaySpec  with FakeTaiPlayApplication {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  val employmentId = 787

  val currentCarBenefit = CodingComponent(CarBenefit, Some(employmentId), 456, "car")

  def includeAllOf(expectedSubstrings: String*): Matcher[String] =
    new Matcher[String] {
      def apply(lhs: String): MatchResult =
        MatchResult(expectedSubstrings forall lhs.contains,
          s"""String "$lhs" did not include all of those substrings: ${expectedSubstrings.map(s => s""""$s"""").mkString(", ")}""",
          s"""String "$lhs" contained all of those substrings: ${expectedSubstrings.map(s => s""""$s"""").mkString(", ")}""")
    }

  "#apply" should {
    "return a default car description if no id's match" in {
      val actual = CompanyCarBenefitPairs(Map.empty, currentCarBenefit, currentCarBenefit, Seq.empty, Seq.empty)
      val defaultCarDescription = "Car benefit"
      actual.companyCarDescription must includeAllOf(defaultCarDescription)
    }

    "return a current car benefit that contains the make model and gross amount" in {
      val employmentIds = Map(employmentId -> "unused")
      val companyCar = CompanyCar(carSeqNo = 100, "Make Model", false, None, None, None)

      val expectedGrossAmount = 456

      val currentCompanyCarBenefits = Seq(CompanyCarBenefit(employmentId, expectedGrossAmount, Seq(companyCar)))
      val actual = CompanyCarBenefitPairs(employmentIds, currentCarBenefit, currentCarBenefit, Seq.empty, currentCompanyCarBenefits)

      actual.grossAmount mustBe expectedGrossAmount
      actual.companyCarDescription must includeAllOf("Make Model", "from 787")
    }
  }
}
