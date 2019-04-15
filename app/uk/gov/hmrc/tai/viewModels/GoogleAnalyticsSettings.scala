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

import uk.gov.hmrc.tai.util.{FormHelper, MapForGoogleAnalytics, MonetaryUtil}

case class GoogleAnalyticsSettings(dimensions: Option[Map[String, String]] = None,
                                   customClientIdRequired: Boolean = false,
                                   customSessionIdRequired: Boolean = false,
                                   customHitStampRequired: Boolean = false)

object GoogleAnalyticsSettings {

  def createForAnnualIncome(gaKey: String, currentAmount: Int, newAmount: Option[String]): GoogleAnalyticsSettings = {
    val poundedCurrentAmount = MonetaryUtil.withPoundPrefix(currentAmount)
    val poundedNewAmount = MonetaryUtil.withPoundPrefix(FormHelper.stripNumber(newAmount).getOrElse("0").toInt)

    val amounts = Map("currentAmount" -> poundedCurrentAmount, "newAmount" -> poundedNewAmount)

    val dimensions: Option[Map[String, String]] = Some(Map(gaKey -> MapForGoogleAnalytics.format(amounts)))
    GoogleAnalyticsSettings(dimensions = dimensions)
  }

  def createForAnnualIncome(gaKey: String, currentAmount: Int, newAmount: Int): GoogleAnalyticsSettings = {
    val poundedNewAmount = MonetaryUtil.withPoundPrefix(newAmount)
    createForAnnualIncome(gaKey, currentAmount, Some(poundedNewAmount))
  }

}