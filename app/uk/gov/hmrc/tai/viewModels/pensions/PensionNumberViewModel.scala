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

package uk.gov.hmrc.tai.viewModels.pensions

import pages.addPensionProvider.{AddPensionProviderNamePage, AddPensionProviderStartWithinSixWeeksPage}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.util.constants.FormValuesConstants
import uk.gov.hmrc.tai.util.constants.journeyCache.AddPensionProviderConstants

case class PensionNumberViewModel(pensionProviderName: String, firstPayChoice: Boolean)

object PensionNumberViewModel {

  def apply(cache: Map[String, String]): PensionNumberViewModel = {
    val pensionProviderName = cache.getOrElse(AddPensionProviderConstants.NameKey, "")
    val firstPayChoice      = cache.get(AddPensionProviderConstants.StartDateWithinSixWeeks) match {
      case Some(FormValuesConstants.YesValue) => true
      case _                                  => false
    }
    PensionNumberViewModel(pensionProviderName, firstPayChoice)
  }

  def apply(userAnswers: UserAnswers): PensionNumberViewModel = {
    val pensionProviderName = userAnswers.get(AddPensionProviderNamePage).getOrElse("")

    val firstPayChoice = userAnswers.get(AddPensionProviderStartWithinSixWeeksPage) match {
      case Some(FormValuesConstants.YesValue) => true
      case _                                  => false
    }
    PensionNumberViewModel(pensionProviderName, firstPayChoice)
  }
}
