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
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

class BbsiUpdateInterestViewModelSpec extends PlaySpec with FakeTaiPlayApplication {

  "Update Bbsi Model" must {
    "show confirmation lines" in {
      val result = BbsiUpdateInterestViewModel(1, "100.12", "TEST")

      val updateLine = result.journeyConfirmationLines.head
      val interestLine = result.journeyConfirmationLines(applicationMessages)(1)

      updateLine.question mustBe Messages("tai.checkYourAnswers.whatYouToldUs")
      updateLine.answer mustBe Messages("tai.bbsi.update.checkYourAnswers.rowOne.answer")
      interestLine.question mustBe Messages("tai.bbsi.update.checkYourAnswers.rowTwo")
      interestLine.answer must include("\u00A3100")
    }
  }

}
