/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util

import builders.RequestBuilder
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.twirl.api.Html
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.BaseSpec

class HelpFormHelperSpec extends BaseSpec {

  val mockPartialRetriever: FormPartialRetriever = mock[FormPartialRetriever]

  implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")

  "HelpFormHelper" should {

    "replace the deskpro link text" when {

      "the partial is retrieved with the expected message" in {

        when(mockPartialRetriever.getPartialContent(any(), any(), any())(any())) thenReturn Html(
          messages("tai.deskpro.link.text.original"))

        HelpFormHelper.replaceMessage(mockPartialRetriever, appConfig).toString() mustBe
          messages("tai.deskpro.link.text.replacement")
      }
    }

    "not replace the deskpro link text" when {

      "the partial is retrieved with different content" in {

        val expectedMessage = "A wild content appears"

        when(mockPartialRetriever.getPartialContent(any(), any(), any())(any())) thenReturn Html(expectedMessage)

        HelpFormHelper.replaceMessage(mockPartialRetriever, appConfig).toString() mustBe expectedMessage
      }

      "an empty partial is retrieved" in {
        when(mockPartialRetriever.getPartialContent(any(), any(), any())(any())) thenReturn Html("")

        HelpFormHelper.replaceMessage(mockPartialRetriever, appConfig).toString() mustBe empty
      }
    }
  }
}
