/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import builders.RequestBuilder
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import uk.gov.hmrc.tai.service._
import utils.BaseSpec
import views.html.PreviousYearUnderpaymentView

import scala.concurrent.Future

class UnderpaymentFromPreviousYearControllerSpec extends BaseSpec {

  val codingComponentService: CodingComponentService = mock[CodingComponentService]

  private class SUT()
      extends UnderpaymentFromPreviousYearController(
        codingComponentService,
        mockAuthJourney,
        mcc,
        inject[PreviousYearUnderpaymentView]
      ) {
    when(codingComponentService.taxFreeAmountComponents(any(), any())(any()))
      .thenReturn(Future.successful(Seq.empty))
  }

  private val referralMap = "Referer" -> "http://somelocation/somePageResource"

  "UnderPaymentFromPreviousYearController" should {
    "respond with OK" when {
      "underpaymentExplanation is called" in {
        val controller = new SUT
        val result     = controller.underpaymentExplanation()(RequestBuilder.buildFakeRequestWithAuth("GET", referralMap))
        status(result) mustBe OK
        contentAsString(result) must include(messagesApi("tai.previous.year.underpayment.title"))
      }
    }
  }

}
