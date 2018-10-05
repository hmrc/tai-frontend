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
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.tai.util.constants.{FormValuesConstants, JourneyCacheConstants}

class PensionNumberViewModelSpec extends PlaySpec  with JourneyCacheConstants with FormValuesConstants with FakeTaiPlayApplication  {

  "Pension number view model" must {
    "create an instance of view model" when {

      "employment name and firstPayChoice is yes" in {
        val cacheMap = Map(AddPensionProvider_NameKey -> "Aaa", AddPensionProvider_StartDateWithinSixWeeks -> YesValue)
        val result = PensionNumberViewModel(cacheMap)
        result mustBe PensionNumberViewModel("Aaa", true)

      }

      "employment name and firstPayChoice is no" in {
        val cacheMap = Map(AddPensionProvider_NameKey -> "Aaa")
        val result = PensionNumberViewModel(cacheMap)
        result mustBe PensionNumberViewModel("Aaa", false)
      }
    }
  }

}
