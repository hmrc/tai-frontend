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

package uk.gov.hmrc.tai.viewModels.pensions

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.util.JourneyCacheConstants
import uk.gov.hmrc.tai.viewModels.CheckYourAnswersConfirmationLine


class CheckYourAnswersViewModelSpec extends PlaySpec
  with JourneyCacheConstants with FakeTaiPlayApplication {

  "companion apply method" must {
    "generate four confirmation lines when telephone contact not approved" in {
      val sut = CheckYourAnswersViewModel("pension provider", "2017-06-13", "ref-123", "No", None)
      val res = sut.journeyConfirmationLines
      res.size mustBe 4
      res(0) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q1"), "pension provider", controllers.pensions.routes.AddPensionProviderController.addPensionProviderName().url)
      res(1) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q2"), "13 June 2017", controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate().url)
      res(2) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q3"), "ref-123", controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url)
      res(3) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q4"), "No", controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url)
    }

    "generate five confirmation lines when telephone contact is approved" in {
      val sut = CheckYourAnswersViewModel("pension provider", "2017-06-13", "ref-123", "Yes", Some("123456789"))
      val res = sut.journeyConfirmationLines
      res.size mustBe 5
      res(0) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q1"), "pension provider", controllers.pensions.routes.AddPensionProviderController.addPensionProviderName().url)
      res(1) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q2"), "13 June 2017", controllers.pensions.routes.AddPensionProviderController.addPensionProviderStartDate().url)
      res(2) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q3"), "ref-123", controllers.pensions.routes.AddPensionProviderController.addPensionNumber().url)
      res(3) mustBe CheckYourAnswersConfirmationLine(Messages("tai.addPensionProvider.cya.q4"), "Yes", controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url)
      res(4) mustBe CheckYourAnswersConfirmationLine(Messages("tai.phoneNumber"), "123456789", controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url)
    }

    "generate a view model with the correct navigational links" in {
      val sut = CheckYourAnswersViewModel("pension provider", "2017-06-13", "ref-123", "Yes", Some("123456789"))
      sut.backLinkUrl mustBe controllers.pensions.routes.AddPensionProviderController.addTelephoneNumber().url
      sut.submissionUrl mustBe controllers.pensions.routes.AddPensionProviderController.submitYourAnswers().url
      sut.cancelUrl mustBe controllers.routes.TaxAccountSummaryController.onPageLoad().url
    }

    "generate a view model with cya title and explanatory text" in {
      val sut = CheckYourAnswersViewModel("pension provider", "2017-06-13", "ref-123", "Yes", Some("123456789"))
      sut.title mustBe Messages("tai.addPensionProvider.cya.title")
      sut.postConfirmationText mustBe Messages("tai.checkYourAnswers.confirmText")
    }
  }
}