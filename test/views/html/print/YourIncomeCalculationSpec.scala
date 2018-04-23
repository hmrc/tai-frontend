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

import controllers.routes
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.EditableDetails
import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModel

class YourIncomeCalculationSpec
  extends TaiViewSpec {


  "The income calculation print page" should {

    behave like pageWithTitle(s"${messages("tai.yourIncome.heading")} - ${messages("tai.service.navTitle")} - GOV.UK")

    "have a back link" in {
      doc must haveBackLink
    }

    "show a print button" in {
      doc.getElementsByClass("print-button").toString must include("javascript:window.print()")
    }

    "show appropriate headings" in {
      doc must haveHeadingWithText("Taxable Income")
      doc must haveHeadingH2WithText("Taxable income from test")
    }
  }

  val incomeCalcViewModel = YourIncomeCalculationViewModel(
    employerName = "test",
    employmentPayments= Nil,
    isPension = true,
    incomeCalculationMsg = "",
    empId = 2,
    hasPrevious = true,
    editableDetails = EditableDetails(
      payRollingBiks = false,
      isEditable = true
    ),
    rtiDown = false,
    employmentStatus = Some(1),
    endDate = None)

  override def view: Html =
    views.html.print.yourIncomeCalculation(incomeCalcViewModel, empId = 2)(
      request = FakeRequest("GET",
      routes.YourIncomeCalculationController.printYourIncomeCalculationPage(None).url),
      messages = play.api.i18n.Messages.Implicits.applicationMessages,
      user = builders.UserBuilder.apply()
    )
}
