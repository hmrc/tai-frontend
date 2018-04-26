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

package uk.gov.hmrc.tai.viewModels.benefit

import controllers.{FakeTaiPlayApplication, routes}
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.JourneyCacheConstants

class CompanyCarCheckAnswersViewModelSpec extends PlaySpec
  with JourneyCacheConstants with FakeTaiPlayApplication {

  "Company car view model" should {
    "create a valid model" when {
      "given a cache map with all expected fields" in {
        val result: CompanyCarCheckAnswersViewModel = CompanyCarCheckAnswersViewModel(cacheMap, taxYear)
        result mustBe CompanyCarCheckAnswersViewModel("Some Car Model", "Some company name", "15 June 2017", "15 May 2017", "2017", "2018")
      }
      "given a cache map with all expected fields except for fuelBenefitStoppedDate" in {
        val result: CompanyCarCheckAnswersViewModel = CompanyCarCheckAnswersViewModel(noFuelCacheMap, taxYear)
        result mustBe CompanyCarCheckAnswersViewModel("Some Car Model", "Some company name", "15 June 2017", "", "2017", "2018")
      }
    }
    "throw an exception" when {
      "any required data is not retrieved from the cache" in {
        val cacheMap = Map.empty[String, String]
        val taxYear = TaxYear(2017)

        val ex = the[RuntimeException] thrownBy CompanyCarCheckAnswersViewModel(cacheMap, taxYear)
        ex.getMessage mustBe "Could not create CompanyCarCheckAnswersViewModel from cache with missing values"
      }

    }
    "supply a single confirmation line view model" when {
      "no fuel benefit end date is present" in {
        val sut = CompanyCarCheckAnswersViewModel(noFuelCacheMap, taxYear)
        val res = sut.journeyConfirmationLines
        res.size mustBe 1
        res(0) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.companyCar.checkAnswers.table.rowOne.description"),
          "15 June 2017",
          routes.CompanyCarController.getCompanyCarEndDate.url
        )
      }
    }
    "supply two confirmation line view model instances" when {
      "fuel benefit end date is present" in {
        val sut = CompanyCarCheckAnswersViewModel(cacheMap, taxYear)
        val res = sut.journeyConfirmationLines
        res.size mustBe 2
        res(0) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.companyCar.checkAnswers.table.rowOne.description"),
          "15 June 2017",
          routes.CompanyCarController.getCompanyCarEndDate.url
        )
        res(1) mustBe CheckYourAnswersConfirmationLine(
          Messages("tai.companyCar.checkAnswers.table.rowTwo.description"),
          "15 May 2017",
          routes.CompanyCarController.getFuelBenefitEndDate.url
        )
      }
    }
  }

  val cacheMap = Map(
    CompanyCar_CarModelKey -> "Some Car Model",
    CompanyCar_CarProviderKey -> "Some company name",
    CompanyCar_DateGivenBackKey -> "2017-06-15",
    CompanyCar_DateFuelBenefitStoppedKey -> "2017-05-15"
  )
  val noFuelCacheMap = Map(
    CompanyCar_CarModelKey -> "Some Car Model",
    CompanyCar_CarProviderKey -> "Some company name",
    CompanyCar_DateGivenBackKey -> "2017-06-15"
  )
  val taxYear = TaxYear(2017)

}
