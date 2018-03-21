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

package views.html

import controllers.{FakeTaiPlayApplication, routes}
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.EditableDetails
import uk.gov.hmrc.tai.util.CeasedEmploymentHelper
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModel

class YourIncomeCalculationSpec
  extends UnitSpec
  with FakeTaiPlayApplication
  with ScalaFutures {


  "The income calculation page" should {

    "do not show the warning message or any update link if an employment is not editable and not payrolling" in {
      val taxSummary = TaiData.getBasicRateTaxSummary
      val incomeCalcViewModel = YourIncomeCalculationViewModel(employerName = "test", employmentPayments= Nil ,isPension = false, incomeCalculationMsg = "", empId = 2, hasPrevious = true,
        editableDetails = EditableDetails(payRollingBiks = false,isEditable = false), rtiDown = false, employmentStatus = Some(1), endDate = None)
      val doc = Jsoup.parse(views.html.incomes.yourIncomeCalculation(incomeCalcViewModel, empId = 2)(request = FakeRequest("GET", routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url), messages = play.api.i18n.Messages.Implicits.applicationMessages, user = builders.UserBuilder.apply(), templateRenderer = MockTemplateRenderer, partialRetriever = MockPartialRetriever).toString())
      doc.select("#payrolling1").size shouldBe 0
      doc.select("#payrolling2").size shouldBe 0
      doc.select("#pensionUpdateLink").size shouldBe 0
      doc.select("#regularUpdateLink").size shouldBe 0

    }

    "show the payrolling warning message an employment is editable and is payrolling" in {
      val incomeCalcViewModel = YourIncomeCalculationViewModel(employerName = "test", employmentPayments= Nil ,isPension = false, incomeCalculationMsg = "", empId = 2, hasPrevious = true,
        editableDetails = EditableDetails(payRollingBiks = true,isEditable = true), rtiDown = false, employmentStatus = Some(1), endDate = None)
      val doc = Jsoup.parse(
        views.html.incomes.yourIncomeCalculation(incomeCalcViewModel, empId = 2)(
          FakeRequest(
            "GET",
            routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url
          ),
          messages = play.api.i18n.Messages.Implicits.applicationMessages,
          user = builders.UserBuilder.apply(),
          templateRenderer = MockTemplateRenderer,
          partialRetriever = MockPartialRetriever
        ).toString()
      )
      doc.select("#payrolling1").size shouldBe 1
      doc.select("#payrolling2").size shouldBe 1

    }

    "show the pensionUpdate Link for an editable pension" in {
      val incomeCalcViewModel = YourIncomeCalculationViewModel(employerName = "test", employmentPayments= Nil ,isPension = true, incomeCalculationMsg = "", empId = 2, hasPrevious = true,
        editableDetails = EditableDetails(payRollingBiks = false,isEditable = true), rtiDown = false, employmentStatus = Some(1), endDate = None)
      val doc = Jsoup.parse(views.html.incomes.yourIncomeCalculation(incomeCalcViewModel, empId = 2)(request = FakeRequest("GET", routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url), messages = play.api.i18n.Messages.Implicits.applicationMessages, user = builders.UserBuilder.apply(), templateRenderer = MockTemplateRenderer, partialRetriever = MockPartialRetriever).toString())
      doc.select("#pensionUpdateLink").size shouldBe 1
      doc.select("#regularUpdateLink").size shouldBe 0

    }

    "show the regularUpdate Link for an editable employment" in {
      val incomeCalcViewModel = YourIncomeCalculationViewModel(employerName = "test", employmentPayments= Nil ,isPension = false, incomeCalculationMsg = "", empId = 2, hasPrevious = true,
        editableDetails = EditableDetails(payRollingBiks = false,isEditable = true), rtiDown = false, employmentStatus = Some(1), endDate = None)
      val doc = Jsoup.parse(views.html.incomes.yourIncomeCalculation(incomeCalcViewModel, empId = 2)(request = FakeRequest("GET", routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url), messages = play.api.i18n.Messages.Implicits.applicationMessages, user = builders.UserBuilder.apply(), templateRenderer = MockTemplateRenderer, partialRetriever = MockPartialRetriever).toString())
      doc.select("#pensionUpdateLink").size shouldBe 0
      doc.select("#regularUpdateLink").size shouldBe 1

    }

    "not show a side navigation component, where no previous tax year details are present" in {
      val incomeCalcViewModel = YourIncomeCalculationViewModel(employerName = "test", employmentPayments= Nil ,isPension = false, incomeCalculationMsg = "", empId = 2, hasPrevious = false,
        editableDetails = EditableDetails(payRollingBiks = false,isEditable = true), rtiDown = false, employmentStatus = Some(1), endDate = None)
      val doc = Jsoup.parse(views.html.incomes.yourIncomeCalculation(incomeCalcViewModel, empId = 2)(request = FakeRequest("GET", routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url), messages = play.api.i18n.Messages.Implicits.applicationMessages, user = builders.UserBuilder.apply(), templateRenderer = MockTemplateRenderer, partialRetriever = MockPartialRetriever).toString())
      doc.select("#taxableIncomeSideNav").size shouldBe 0
    }
  }

  "The end date for ceased employment" should {
    "be displayed in the expected format" in {
      CeasedEmploymentHelper.toDisplayFormat(Some(new LocalDate("2017-06-09"))) shouldBe ("9 June 2017")
    }

    "not error out on empty date" in {
      CeasedEmploymentHelper.toDisplayFormat(None) shouldBe ""
    }
  }

}
