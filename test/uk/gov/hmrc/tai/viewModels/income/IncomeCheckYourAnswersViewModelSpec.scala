/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.income

import play.api.i18n.Messages
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import utils.BaseSpec

class IncomeCheckYourAnswersViewModelSpec extends BaseSpec {

  "companion apply method" must {
    "generate five confirmation lines when telephone contact not approved (includes PAYE ref)" in {
      val sut = IncomeCheckYourAnswersViewModel(
        preHeading = "pre heading",
        incomeSourceName = "income source",
        incomeSourceStart = "2017-06-13",
        incomeSourceRefNo = "ref-123",
        payeRef = "123/AB456",
        contactableByPhone = "No",
        phoneNumber = None,
        backLinkUrl = "/fake/backlink/url",
        submissionUrl = "/fake/continue/url",
        cancelUrl = "/fake/cancel/url"
      )
      val res = sut.journeyConfirmationLines
      res.size mustBe 5
      res.head mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q1"),
        "income source",
        controllers.employments.routes.AddEmploymentController.addEmploymentName().url
      )
      res(1) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q2"),
        "13 June 2017",
        controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url
      )
      res(2) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q3"),
        "ref-123",
        controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url
      )
      res(3) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.income.details.ERN"),
        "123/AB456",
        controllers.employments.routes.AddEmploymentController.addPayeReference().url
      )
      res(4) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q4"),
        "No",
        controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url
      )
    }

    "generate six confirmation lines when telephone contact is approved (includes PAYE ref + phone number)" in {
      val sut = IncomeCheckYourAnswersViewModel(
        preHeading = "pre heading",
        incomeSourceName = "income source",
        incomeSourceStart = "2017-06-13",
        incomeSourceRefNo = "ref-123",
        payeRef = "123/AB456",
        contactableByPhone = "Yes",
        phoneNumber = Some("123456789"),
        backLinkUrl = "/fake/backlink/url",
        submissionUrl = "/fake/continue/url",
        cancelUrl = "/fake/cancel/url"
      )
      val res = sut.journeyConfirmationLines
      res.size mustBe 6
      res.head mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q1"),
        "income source",
        controllers.employments.routes.AddEmploymentController.addEmploymentName().url
      )
      res(1) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q2"),
        "13 June 2017",
        controllers.employments.routes.AddEmploymentController.addEmploymentStartDate().url
      )
      res(2) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q3"),
        "ref-123",
        controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber().url
      )
      res(3) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.income.details.ERN"),
        "123/AB456",
        controllers.employments.routes.AddEmploymentController.addPayeReference().url
      )
      res(4) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q4"),
        "Yes",
        controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url
      )
      res(5) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.phoneNumber"),
        "123456789",
        controllers.employments.routes.AddEmploymentController.addTelephoneNumber().url
      )
    }
  }

  "companion apply method for end employment" must {
    "generate two confirmation lines when telephone contact not approved" in {
      val sut = IncomeCheckYourAnswersViewModel(
        preHeading = "pre heading",
        incomeSourceEnd = "2017-06-13",
        contactableByPhone = "No",
        phoneNumber = None,
        backLinkUrl = "/fake/backlink/url",
        submissionUrl = "/fake/continue/url",
        cancelUrl = "/fake/cancel/url"
      )
      val res = sut.journeyConfirmationLines

      res.size mustBe 2
      res.head mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.checkYourAnswers.dateEmploymentEnded"),
        "13 June 2017",
        controllers.employments.routes.EndEmploymentController.endEmploymentPage().url
      )
      res(1) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.checkYourAnswers.contactByPhone"),
        "No",
        controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url
      )
    }

    "generate three confirmation lines when telephone contact is approved" in {
      val sut = IncomeCheckYourAnswersViewModel(
        preHeading = "pre heading",
        incomeSourceEnd = "2017-06-13",
        contactableByPhone = "Yes",
        phoneNumber = Some("123456789"),
        backLinkUrl = "/fake/backlink/url",
        submissionUrl = "/fake/continue/url",
        cancelUrl = "/fake/cancel/url"
      )
      val res = sut.journeyConfirmationLines

      res.size mustBe 3
      res.head mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.checkYourAnswers.dateEmploymentEnded"),
        "13 June 2017",
        controllers.employments.routes.EndEmploymentController.endEmploymentPage().url
      )
      res(1) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.checkYourAnswers.contactByPhone"),
        "Yes",
        controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url
      )
      res(2) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.phoneNumber"),
        "123456789",
        controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url
      )
    }
  }
}
