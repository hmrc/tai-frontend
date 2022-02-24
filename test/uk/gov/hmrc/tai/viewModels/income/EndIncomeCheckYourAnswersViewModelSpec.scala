/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.i18n.Messages
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine
import utils.BaseSpec

import scala.concurrent.Future

class EndIncomeCheckYourAnswersViewModelSpec extends BaseSpec with JourneyCacheConstants {

  "journeyConfirmationLines method" must {
    "generate two confirmation lines when telephone contact not approved" in {
      val sut = EndIncomeCheckYourAnswersViewModel(
        "pre heading",
        "income source",
        empId,
        "2017-06-13",
        "No",
        None,
        "/fake/backlink/url")
      val res = sut.journeyConfirmationLines

      when(endEmploymentJourneyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
        .thenReturn(Future.successful(Right(Seq(employerName, empId.toString))))

      res.size mustBe 2
      res(0) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q2"),
        "13 June 2017",
        controllers.employments.routes.EndEmploymentController.endEmploymentPage.url)
      res(1) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q4"),
        "No",
        controllers.employments.routes.EndEmploymentController.submitTelephoneNumber().url)
    }

    "generate three confirmation lines when telephone contact is approved" in {
      val sut = EndIncomeCheckYourAnswersViewModel(
        "pre heading",
        "income source",
        empId,
        "2017-06-13",
        "Yes",
        Some("123456789"),
        "/fake/backlink/url")
      val res = sut.journeyConfirmationLines

      when(endEmploymentJourneyCacheService.mandatoryJourneyValues(Matchers.anyVararg[String])(any()))
        .thenReturn(Future.successful(Right(Seq(employerName, empId.toString))))

      res.size mustBe 3
      res(0) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q2"),
        "13 June 2017",
        controllers.employments.routes.EndEmploymentController.endEmploymentPage.url)
      res(1) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.addEmployment.cya.q4"),
        "Yes",
        controllers.employments.routes.EndEmploymentController.addTelephoneNumber().url)
      res(2) mustBe CheckYourAnswersConfirmationLine(
        Messages("tai.phoneNumber"),
        "123456789",
        controllers.employments.routes.EndEmploymentController.addTelephoneNumber.url)
    }
  }
  private val employerName = "Employer Name"
  private val empId = 1
  private val endEmploymentJourneyCacheService = mock[JourneyCacheService]
}
