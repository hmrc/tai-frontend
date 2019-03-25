/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}

class SameEstimatedPayViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val income = 123

  "SameEstimatedPayViewModel" must {
    "have a return link back to pension details" in {
      val model = SameEstimatedPayViewModel("pension", income, isPension =  true)
      model.returnLink mustBe messagesApi("tai.updateEmployment.incomeSame.pension.return.link")
    }

    "have a return link back to employment details" in {
      val model = SameEstimatedPayViewModel("employer", income, isPension =  false)
      model.returnLink mustBe messagesApi("tai.updateEmployment.incomeSame.employment.return.link")
    }
  }
}
