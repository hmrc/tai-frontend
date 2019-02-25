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

package uk.gov.hmrc.tai.viewModels

import play.api.i18n.Messages
import uk.gov.hmrc.tai.service.{SevenDays, ThreeWeeks, TimeToProcess}

case class IFormInProgressBannerViewModel(isAnyIFormInProgress: TimeToProcess) {
  def durationMessage()(implicit messages: Messages): Option[String] = {
    isAnyIFormInProgress match {
      case ThreeWeeks => Some(messages("tai.whatDoYouWantToDo.iformPanel.threeWeeks.p2"))
      case SevenDays => Some(messages("tai.whatDoYouWantToDo.iformPanel.sevenDays.p2"))
      case _ => None
    }
  }
}
