/*
 * Copyright 2023 HM Revenue & Customs
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

import utils.BaseSpec

class SameEstimatedPayViewModelSpec extends BaseSpec {

  val income = 123
  val id     = 1

  "SameEstimatedPayViewModel" must {
    "have a return link back to pension details" in {
      val model = SameEstimatedPayViewModel("pension", id, Some(income), isPension = true, "some url")
      model.returnLinkLabel mustBe messagesApi("tai.updateEmployment.incomeSame.pension.return.link")
    }

    "have a return link back to employment details" in {
      val model = SameEstimatedPayViewModel("employer", id, Some(income), isPension = false, "some url")
      model.returnLinkLabel mustBe messagesApi("tai.updateEmployment.incomeSame.employment.return.link")
    }

    "format amount as £0 when amount is None" in {
      val model = SameEstimatedPayViewModel("employer", id, None, isPension = false, "some url")
      model.amountWithPounds mustBe "£0"
    }

    "format amount with pound sign when amount is defined" in {
      val model = SameEstimatedPayViewModel("employer", id, Some(123), isPension = false, "some url")
      model.amountWithPounds mustBe "£123"
    }
  }
}
