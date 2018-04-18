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

package views.html.print

import controllers.{FakeTaiPlayApplication, routes}
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.tai.model.EditableDetails
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModel

class YourIncomeCalculationSpec
  extends UnitSpec
    with FakeTaiPlayApplication
    with ScalaFutures {


  "The income calculation print page" should {
    "show the necessary back and print buttons" in {
      val incomeCalcViewModel = YourIncomeCalculationViewModel(employerName = "test", employmentPayments= Nil ,isPension = true, incomeCalculationMsg = "", empId = 2, hasPrevious = true,
        editableDetails = EditableDetails(payRollingBiks = false,isEditable = true), rtiDown = false, employmentStatus = Some(1), endDate = None)
      val doc = Jsoup.parse(views.html.print.yourIncomeCalculation(incomeCalcViewModel, empId = 2)(request = FakeRequest("GET", routes.YourIncomeCalculationController.printYourIncomeCalculationPage(None).url), messages = play.api.i18n.Messages.Implicits.applicationMessages, user = builders.UserBuilder.apply()).toString())
      doc.getElementsByClass("back-link").toString should include("/check-income-tax/your-income-calculation")
      doc.getElementsByClass("print-button").toString should include("javascript:window.print()")
      doc.getElementsByClass("title").toString should include("Taxable Income")
    }

  }

}
