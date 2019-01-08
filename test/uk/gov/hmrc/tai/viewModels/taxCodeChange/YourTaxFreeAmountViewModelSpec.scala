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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.CodingComponentPairDescription

class YourTaxFreeAmountViewModelSpec extends PlaySpec with FakeTaiPlayApplication with ViewModelHelper with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private val pairs = Seq(
    CodingComponentPairDescription("Thing", 1000, 2000),
    CodingComponentPairDescription("Thing", 3000, 3000)
  )

  "YourTaxFreeAmountViewModel" should {
    "prettyPrint BigDecimals as currency" in {
      YourTaxFreeAmountViewModel.prettyPrint(1000) mustBe "£1,000"
    }

    "sum previous when passed a seq of pairs and return as currency" in {
      YourTaxFreeAmountViewModel.totalPrevious(pairs) mustBe "£4,000"
    }

    "sum current when passed a seq of pairs and return as currency" in {
      YourTaxFreeAmountViewModel.totalCurrent(pairs) mustBe "£5,000"
    }
  }
}