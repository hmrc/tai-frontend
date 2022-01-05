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

package uk.gov.hmrc.tai.viewModels

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.domain.TaxCodeMismatch
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants
import utils.factories.TaxCodeMismatchFactory

class WhatDoYouWantToDoViewModelSpec extends PlaySpec {

  def gaMap(result: String): Map[String, String] =
    Map(GoogleAnalyticsConstants.taiLandingPageInformation -> result)

  def createViewModel(
    isCyPlusOneEnabled: Boolean,
    hasTaxCodeChanged: Boolean = false,
    taxCodeMismatch: Option[TaxCodeMismatch] = None): WhatDoYouWantToDoViewModel =
    WhatDoYouWantToDoViewModel(isCyPlusOneEnabled, hasTaxCodeChanged, false, taxCodeMismatch)

  val mismatchedTaxCode = TaxCodeMismatchFactory.mismatchedTaxCodeComplex
  val matchedTaxCode = TaxCodeMismatchFactory.matchedTaxCode

  "showTaxCodeChangeTile" must {
    "return true" when {
      "there has been a tax code change and no mismatch" in {
        val viewModel = createViewModel(true, true, Some(matchedTaxCode))

        viewModel.showTaxCodeChangeTile() mustEqual true
      }
    }

    "return false" when {

      "there has been a tax code change and mismatch is None" in {
        val viewModel = createViewModel(true, true)

        viewModel.showTaxCodeChangeTile() mustEqual false
      }

      "there are no confirmed taxCodeRecords in the TaxCodeMismatch" in {

        val taxCodeMismatchWithNoConfirmedRecords = TaxCodeMismatch(true, Seq("taxCode"), Seq.empty)

        val viewModel = createViewModel(true, true, Some(taxCodeMismatchWithNoConfirmedRecords))

        viewModel.showTaxCodeChangeTile() mustEqual false

      }

      "there are no taxCodeRecords in the TaxCodeMismatch at all" in {

        val taxCodeMismatchWithNoRecords = TaxCodeMismatch(false, Seq.empty, Seq.empty)

        val viewModel = createViewModel(true, true, Some(taxCodeMismatchWithNoRecords))

        viewModel.showTaxCodeChangeTile() mustEqual false

      }

      "there has been a tax code change and there is a mismatch" in {
        val viewModel = createViewModel(true, true, Some(mismatchedTaxCode))

        viewModel.showTaxCodeChangeTile() mustEqual false
      }
      "there has not been a tax code change" in {
        val viewModel = createViewModel(true, false)

        viewModel.showTaxCodeChangeTile() mustEqual false
      }
    }
  }

  "gaDimensions" must {
    "Create a string collection of what the tile view shows" when {

      "CY+1=false TCC=false" in {
        val viewModel = createViewModel(false, false)

        val expected = "TCC=false;CY=true;PY=true;CY+1=false"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }

      "CY+1=true TCC=false" in {
        val viewModel = createViewModel(true, false)

        val expected = "TCC=false;CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }

      "CY+1=true TCC=true" in {
        val viewModel = createViewModel(true, true)

        val expected = "TCC=true;CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }

      "CY+1=false TCC=true" in {
        val viewModel = createViewModel(false, true)

        val expected = "TCC=true;CY=true;PY=true;CY+1=false"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }
    }

    "return mismatched tax code code comparision results" when {
      "there has been a tax code change" in {
        val viewModel = createViewModel(true, true, Some(mismatchedTaxCode))

        val expected = "TCC=true;TCM=true;CONFIRMED=[1180L,0T];UNCONFIRMED=[1185L,0T];CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }
      "there has not been a tax code change" in {
        val viewModel = createViewModel(true, false, Some(mismatchedTaxCode))

        val expected = "TCC=false;TCM=true;CONFIRMED=[1180L,0T];UNCONFIRMED=[1185L,0T];CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }
    }

    "return matched tax code code comparision results" when {
      "there has been a tax code change" in {
        val viewModel = createViewModel(true, true, Some(matchedTaxCode))

        val expected = "TCC=true;TCM=false;CONFIRMED=[1185L];UNCONFIRMED=[1185L];CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }
      "there has not been a tax code change" in {
        val viewModel = createViewModel(true, false, Some(matchedTaxCode))

        val expected = "TCC=false;TCM=false;CONFIRMED=[1185L];UNCONFIRMED=[1185L];CY=true;PY=true;CY+1=true"

        viewModel.gaDimensions() mustEqual gaMap(expected)
      }
    }
  }
}
