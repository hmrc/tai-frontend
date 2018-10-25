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

package uk.gov.hmrc.tai.viewModels

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.util.GoogleAnalyticsConstants

class WhatDoYouWantToDoViewModelSpec extends PlaySpec {

  def gaMap(result: String): Map[String, String] = {
    Map(GoogleAnalyticsConstants.taiLandingPageInformation -> result)
  }

  "gaDimensions" must {
    "Create a string collection of what the tile view shows" when {

      "CY+1=false TCC=false" in {
        val viewModel = WhatDoYouWantToDoViewModel(false, false, false)

        val expected = "TCC=false;CY=true;PY=true;CY+1=false"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }

      "CY+1=true TCC=false" in {
        val viewModel = WhatDoYouWantToDoViewModel(false, true, false)

        val expected = "TCC=false;CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }

      "CY+1=true TCC=true" in {
        val viewModel = WhatDoYouWantToDoViewModel(false, true, true)

        val expected = "TCC=true;CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }

      "CY+1=false TCC=true" in {
        val viewModel = WhatDoYouWantToDoViewModel(false, false, true)

        val expected = "TCC=true;CY=true;PY=true;CY+1=false"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }
    }
  }
}
