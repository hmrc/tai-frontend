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

import play.api.i18n.Messages
import uk.gov.hmrc.tai.util.{FormValuesConstants, JourneyCacheConstants}

case class PensionNumberViewModel(pensionProviderName: String,
                                  firstPayChoice: Boolean)

object PensionNumberViewModel extends JourneyCacheConstants with FormValuesConstants {

  def apply(cache: Map[String, String])(implicit messages: Messages): PensionNumberViewModel = {
    val pensionProviderName = cache.getOrElse(AddPensionProvider_NameKey, "")
    val firstPayChoice = cache.get(AddPensionProvider_StartDateWithinSixWeeks) match {
      case Some(YesValue) => true
      case _ => false
    }
    PensionNumberViewModel(pensionProviderName, firstPayChoice)
  }
}
