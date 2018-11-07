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

package uk.gov.hmrc.tai.viewModels.income

import controllers.FakeTaiPlayApplication
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants


class IncomeCheckYourAnswersViewModelSpec extends PlaySpec
  with JourneyCacheConstants with FakeTaiPlayApplication {

  "companion apply method" must {
    "generate four confirmation lines when telephone contact not approved" in {
      val sut = IncomeCheckYourAnswersViewModel("pre heading", "income source", "2017-06-13", "ref-123", "No", None, "/fake/backlink/url", "/fake/continue/url", "/fake/cancel/url")
      val res = sut.journeyConfirmationLines
      res.size mustBe 4
      res(0) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q1"), "income source", controllers.employments.routes.AddEmploymentController.addEmploymentName.url)
      res(1) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q2"), "13 June 2017", controllers.employments.routes.AddEmploymentController.addEmploymentStartDate.url)
      res(2) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q3"), "ref-123", controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber.url)
      res(3) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q4"), "No", controllers.employments.routes.AddEmploymentController.addTelephoneNumber.url)
    }

    "generate five confirmation lines when telephone contact is approved" in {
      val sut = IncomeCheckYourAnswersViewModel("pre heading", "income source", "2017-06-13", "ref-123", "Yes", Some("123456789"), "/fake/backlink/url", "/fake/continue/url", "/fake/cancel/url")
      val res = sut.journeyConfirmationLines
      res.size mustBe 5
      res(0) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q1"), "income source", controllers.employments.routes.AddEmploymentController.addEmploymentName.url)
      res(1) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q2"), "13 June 2017", controllers.employments.routes.AddEmploymentController.addEmploymentStartDate.url)
      res(2) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q3"), "ref-123", controllers.employments.routes.AddEmploymentController.addEmploymentPayrollNumber.url)
      res(3) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addEmployment.cya.q4"), "Yes", controllers.employments.routes.AddEmploymentController.addTelephoneNumber.url)
      res(4) mustBe CheckYourAnswersConfirmationLine(Messages("tai.phoneNumber"), "123456789", controllers.employments.routes.AddEmploymentController.addTelephoneNumber.url)
    }
  }

  "companion apply method for end employment" must {
    "generate two confirmation lines when telephone contact not approved" in {
      val sut = IncomeCheckYourAnswersViewModel(0, "pre heading", "2017-06-13", "No", None, "/fake/backlink/url", "/fake/continue/url", "/fake/cancel/url")
      val res = sut.journeyConfirmationLines
      res.size mustBe 2
      res(0) mustBe CheckYourAnswersConfirmationLine(Messages("tai.checkYourAnswers.dateEmploymentEnded"), "13 June 2017", controllers.employments.routes.EndEmploymentController.endEmploymentPage(0).url)
      res(1) mustBe CheckYourAnswersConfirmationLine(Messages("tai.checkYourAnswers.contactByPhone"), "No", controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url)
    }

    "generate five confirmation lines when telephone contact is approved" in {
      val sut = IncomeCheckYourAnswersViewModel(0, "pre heading", "2017-06-13", "Yes", Some("123456789"), "/fake/backlink/url", "/fake/continue/url", "/fake/cancel/url")
      val res = sut.journeyConfirmationLines
      res.size mustBe 3
      res(0) mustBe CheckYourAnswersConfirmationLine(Messages("tai.checkYourAnswers.dateEmploymentEnded"), "13 June 2017", controllers.employments.routes.EndEmploymentController.endEmploymentPage(0).url)
      res(1) mustBe CheckYourAnswersConfirmationLine(Messages("tai.checkYourAnswers.contactByPhone"), "Yes", controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url)
      res(2) mustBe CheckYourAnswersConfirmationLine(Messages("tai.phoneNumber"), "123456789", controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url)
    }
  }
}