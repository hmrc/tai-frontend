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

import uk.gov.hmrc.tai.model.domain.TaxCodeMismatch
import uk.gov.hmrc.tai.util.constants.GoogleAnalyticsConstants

import scala.collection.immutable.ListMap

case class WhatDoYouWantToDoViewModel(isAnyIFormInProgress: Boolean,
                                      isCyPlusOneEnabled: Boolean,
                                      hasTaxCodeChanged: Boolean = false,
                                      taxCodeMismatch: Option[TaxCodeMismatch] = None) {

  def showTaxCodeChangeTile(): Boolean = {
    (hasTaxCodeChanged, taxCodeMismatch) match {
      case (true, Some(TaxCodeMismatch(false, _, _))) => true
      case _ => false
    }
  }

  def gaDimensions(): Map[String, String] = {

    val enabledMap = taxCodeChangeDimensions ++ ListMap(
      GoogleAnalyticsConstants.taiLandingPageCYKey -> "true",
      GoogleAnalyticsConstants.taiLandingPagePYKey -> "true",
      GoogleAnalyticsConstants.taiLandingPageCY1Key -> isCyPlusOneEnabled.toString
    )

    Map(GoogleAnalyticsConstants.taiLandingPageInformation -> formatMapForGA(enabledMap))
  }

  private def taxCodeChangeDimensions: ListMap[String, String] = {
    (hasTaxCodeChanged, taxCodeMismatch) match {
      case (_, Some(mismatch)) if mismatch.mismatch => {
        ListMap(
          GoogleAnalyticsConstants.taiLandingPageTCCKey -> GoogleAnalyticsConstants.taiLandingPageMismatchValue,
          GoogleAnalyticsConstants.taiLandingPageConfirmedKey -> formatSeqToString(mismatch.confirmedTaxCodes),
          GoogleAnalyticsConstants.taiLandingPageUnconfirmedKey -> formatSeqToString(mismatch.unconfirmedTaxCodes)
        )
      }
      case _ => ListMap(GoogleAnalyticsConstants.taiLandingPageTCCKey -> hasTaxCodeChanged.toString)
    }
  }

  private def formatMapForGA(map: Map[String, String]): String = {
    map.mkString(";").replace(" -> ", "=")
  }

  private def formatSeqToString(seq: Seq[String]): String = {
    seq.mkString("[", ",", "]")
  }
}

