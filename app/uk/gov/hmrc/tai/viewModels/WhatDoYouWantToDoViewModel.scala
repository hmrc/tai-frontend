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

import uk.gov.hmrc.tai.model.domain.TaxCodeMismatch
import uk.gov.hmrc.tai.util.MapForGoogleAnalytics
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants

import scala.collection.immutable.ListMap

case class WhatDoYouWantToDoViewModel(
  isCyPlusOneEnabled: Boolean,
  hasTaxCodeChanged: Boolean = false,
  showJrsLink: Boolean,
  taxCodeMismatch: Option[TaxCodeMismatch] = None) {

  def showTaxCodeChangeTile(): Boolean =
    (hasTaxCodeChanged, taxCodeMismatch) match {
      case (_, Some(mismatch)) if mismatch.confirmedTaxCodes.isEmpty => false
      case (true, Some(TaxCodeMismatch(false, _, _)))                => true
      case _                                                         => false
    }

  def gaDimensions(): Map[String, String] = {

    val enabledMap = taxCodeChangeDimensions ++ ListMap(
      GoogleAnalyticsConstants.TaiLandingPageCYKey  -> "true",
      GoogleAnalyticsConstants.TaiLandingPagePYKey  -> "true",
      GoogleAnalyticsConstants.TaiLandingPageCY1Key -> isCyPlusOneEnabled.toString
    )

    Map(GoogleAnalyticsConstants.TaiLandingPageInformation -> MapForGoogleAnalytics.format(enabledMap))
  }

  private def taxCodeChangeDimensions: ListMap[String, String] =
    taxCodeMismatch match {
      case Some(mismatch) =>
        ListMap(
          GoogleAnalyticsConstants.TaiLandingPageTCCKey         -> hasTaxCodeChanged.toString,
          GoogleAnalyticsConstants.TaiLandingPageTCMKey         -> mismatch.mismatch.toString,
          GoogleAnalyticsConstants.TaiLandingPageConfirmedKey   -> formatSeqToString(mismatch.confirmedTaxCodes),
          GoogleAnalyticsConstants.TaiLandingPageUnconfirmedKey -> formatSeqToString(mismatch.unconfirmedTaxCodes)
        )
      case _ => ListMap(GoogleAnalyticsConstants.TaiLandingPageTCCKey -> hasTaxCodeChanged.toString)
    }

  private def formatSeqToString(seq: Seq[String]): String =
    seq.mkString("[", ",", "]")

}
