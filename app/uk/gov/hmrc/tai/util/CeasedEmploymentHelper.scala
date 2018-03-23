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

package uk.gov.hmrc.tai.util

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Play.current
import play.api.i18n.Messages
//import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.{CeasedEmploymentDetails, TaiRoot}
import uk.gov.hmrc.urls.Link
import TaiConstants._
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.config.ApplicationConfig

object CeasedEmploymentHelper {

  def isMCI(taiRoot: Option[TaiRoot]) = {
    taiRoot.isDefined match {
      case true => taiRoot.get.manualCorrespondenceInd
      case _ => true
    }
  }

  def getEmploymentCeasedStatus(ceasedDetail: Option[CeasedEmploymentDetails]) = {
    ceasedDetail match {
      case Some(detail) => detail.ceasedStatus
      case _ => None
    }
  }

  def getCeasedMsg(ceasedDetail: CeasedEmploymentDetails)(implicit messages: Messages): (Option[String]) = {
    val ceasedKey: String = ceasedDetail.ceasedStatus match {
      case Some(CeasedMinusThree) => if(ApplicationConfig.isTaiCy3Enabled) "contact" else CEASED_MINUS_TWO
      case _ => ceasedDetail.ceasedStatus.getOrElse("")
    }

    Some(Messages("tai.paye.ceased." + (if (ceasedDetail.isPension.get) "pension." else "emp.") + ceasedKey,
      ceasedDetail.endDate.map(Dates.formatDate).getOrElse("")))
  }

  def getTellUsCeasedMsg(ceasedDetail: CeasedEmploymentDetails)(implicit messages: Messages): Html = {
    Html(Messages("tai.income.calculation.detailsWrongIform." + (if (ceasedDetail.isPension.getOrElse(false)) {
      "pension"
    } else {
      "emp"
    }), Link.toInternalPage(url = ApplicationConfig.incomeFromEmploymentPensionLinkUrl,
      value = Some(Messages("tai.income.calculation.detailsWrongIformLink")),
      dataAttributes = Some(Map("journey-click" -> "check-income-tax:Outbound Link:wrong-other-income-iform"))).toHtml))
  }

  def toDisplayFormat(date: Option[LocalDate])(implicit messages: Messages): String = {
    date match {
      case Some(dt) => Dates.formatDate(dt)
      case _ => ""
    }
  }

  def isDeceased(taiRoot: Option[TaiRoot]): Boolean = {
    taiRoot match {
      case Some(TaiRoot(_, _, _, _, _, _, _, _, Some(deceasedIndicator))) => deceasedIndicator
      case _ => false
    }
  }
}
