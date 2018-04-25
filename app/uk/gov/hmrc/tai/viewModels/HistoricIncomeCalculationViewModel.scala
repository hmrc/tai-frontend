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

import org.joda.time.LocalDate
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.TaiConstants.EYU_DATE_FORMAT
import play.api.i18n.Messages
import play.api.Play.current
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.urls.Link

case class HistoricIncomeCalculationViewModel(employerName: Option[String], employmentId: Int, payments: Seq[Payment],
                                              endOfTaxYearUpdateMessages: Seq[String], realTimeStatus: RealTimeStatus,
                                              iFormLink: Html, taxYear: TaxYear)

object HistoricIncomeCalculationViewModel {

  def apply(employments: Seq[Employment], employmentId: Int, taxYear: TaxYear)(implicit messages: Messages): HistoricIncomeCalculationViewModel = {
    val (employment, annualAccount) = fetchEmploymentAndAnnualAccount(employments, taxYear, employmentId)
    val realTimeStatus = fetchRealTimeStatus(annualAccount)
    val (payments, endOfTaxYearUpdateMessages) = annualAccount match {
      case Some(annualAccnt) => (annualAccnt.payments, createEndOfYearTaxUpdateMessages(annualAccnt))
      case _ => (Nil, Nil)
    }

    HistoricIncomeCalculationViewModel(employment.map(_.name), employmentId, payments, endOfTaxYearUpdateMessages, realTimeStatus, createIFormLink, taxYear)
  }

  def fetchRealTimeStatus(annualAccount: Option[AnnualAccount]): RealTimeStatus = {
    annualAccount match {
      case Some(annualAccnt) => annualAccnt.realTimeStatus
      case _ => TemporarilyUnavailable
    }
  }

  def fetchEmploymentAndAnnualAccount(employments: Seq[Employment], taxYear: TaxYear, employmentId: Int): (Option[Employment], Option[AnnualAccount]) = {
    val employment = employments.find(_.sequenceNumber == employmentId)
    val annualAccount: Option[AnnualAccount] = employment.flatMap(emp => emp.annualAccounts.find(_.taxYear.year == taxYear.year))
    (employment, annualAccount)
  }

  def createEndOfYearTaxUpdateMessages(annualAccount: AnnualAccount)(implicit messages: Messages): Seq[String] = {
    val lessOrMore = (amount: BigDecimal) => amount.abs.toString + (if (amount > 0) " more" else " less")
    val eyuObjectList = filterEndOfYearUpdateAdjustments(annualAccount)

    eyuObjectList.map {
      adjustmentsWithDate =>
        val (adjustment, date) = adjustmentsWithDate

        val messageKey = (adjustment.`type`, eyuObjectList.size) match {
          case (IncomeAdjustment, 1)                            => "tai.income.calculation.eyu.single.taxableincome"
          case (TaxAdjustment, 1)                               => "tai.income.calculation.eyu.single.taxPaid"
          case (NationalInsuranceAdjustment, 1)                 => "tai.income.calculation.eyu.single.nationalInsurance"
          case (IncomeAdjustment, size) if size > 1             => "tai.income.calculation.eyu.multi.taxableincome"
          case (TaxAdjustment, size) if size > 1                => "tai.income.calculation.eyu.multi.taxPaid"
          case (NationalInsuranceAdjustment, size) if size > 1  => "tai.income.calculation.eyu.multi.nationalInsurance"
        }

        Messages(messageKey, date.toString(EYU_DATE_FORMAT), lessOrMore(adjustment.amount))
    }
  }

  def createIFormLink(implicit messages: Messages) = {
    Html(Messages("tai.income.calculation.detailsWrongIform.description",
      Link.toInternalPage(url = ApplicationConfig.incomeFromEmploymentPensionLinkUrl,
      value = Some(Messages("tai.income.calculation.detailsWrongIformLink")),
      dataAttributes = Some(Map("journey-click" -> "check-income-tax:Outbound Link:wrong-other-income-iform"))).toHtml))
  }

  def filterEndOfYearUpdateAdjustments(annualAccount: AnnualAccount): Seq[(Adjustment, LocalDate)] = {
    annualAccount.endOfTaxYearUpdates.flatMap {
      eyu =>
        eyu.adjustments.filter(_.amount != 0).map((_, eyu.date))
    }
  }
}