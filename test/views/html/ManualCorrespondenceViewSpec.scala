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

package views.html

import uk.gov.hmrc.tai.util.viewHelpers.TaiViewSpec

class ManualCorrespondenceViewSpec extends TaiViewSpec {

  private val manualCorrespondence = inject[ManualCorrespondenceView]
  override def view = manualCorrespondence("https://contacturl")

  "manual correspondence page" should {
    behave like pageWithTitle(messages("mci.title"))
    behave like pageWithHeader(messages("mci.title"))

    "have the contactUrl" in {
      doc.body().toString must include("https://contacturl")
    }
  }
}
