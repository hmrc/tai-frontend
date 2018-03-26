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

import java.util.NoSuchElementException

import uk.gov.hmrc.tai.model.{SessionData, TaxSummaryDetails}
import org.joda.time.LocalDate
import play.api.http.Status
import play.api.Play.current
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.tai.model.nps2.{EmploymentStatus, IabdUpdateSource}
import uk.gov.hmrc.tai.model.tai.TaxYear
import uk.gov.hmrc.play.views.helpers.MoneyPounds
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.{IncomeExplanation, SessionData, TaxSummaryDetails}
import uk.gov.hmrc.tai.model.rti.PayFrequency._
import uk.gov.hmrc.tai.model.rti.{RtiEyu, RtiPayment}
import uk.gov.hmrc.time.TaxYearResolver
import uk.gov.hmrc.urls.Link
import uk.gov.hmrc.tai.util.TaiConstants._
import uk.gov.hmrc.play.language.LanguageUtils.Dates

import scala.collection.mutable.ListBuffer

object YourIncomeCalculationHelper {

  def displayPayrollNumber(hasDuplicateEmploymentNames: Boolean, worksNumber: Option[String], isPension: Boolean)(implicit messages: Messages): Option[String] = {

    (hasDuplicateEmploymentNames, worksNumber.isDefined, isPension) match {
      case (true, true, true) => worksNumber.map(payrollNumber => messages("tai.income.calculation.Income.PayrollNumberPension.text", payrollNumber))
      case (true, true, false) => worksNumber.map(payrollNumber => messages("tai.income.calculation.Income.PayrollNumberEmployment.text", payrollNumber))
      case _ => None
    }
  }

  def getCurrentYearPayments(taxSummaryDetails: TaxSummaryDetails, empId: Int)(implicit messages: Messages): (List[RtiPayment], Boolean, Option[String]) = {

    val accounts = taxSummaryDetails.currentYearAccounts

    val employment = accounts.toList.flatMap(_.employments).find(_.nps.employmentId.contains(empId))
    val employmentPayments = employment.toList.flatMap(_.payments)

    val empIncomes = accounts.flatMap(_.nps).map(_.incomes).flatMap(x => x.find(_.employmentId.contains(empId)))
    val employmentStartDate = empIncomes.flatMap(_.employmentRecord.map(_.start))
    val hasPrevious = hasPreviousEmployment(employmentStartDate)

    val income = taxSummaryDetails.incomeData.map(_.incomeExplanations).flatMap(_.find(_.incomeId == empId))

    (employmentPayments, hasPrevious, getNotMatchingTotalMessage(employmentPayments, income.map(_.isPension)))
  }

  def getIncomeExplanationMessage(incomeExplanation: IncomeExplanation)(implicit messages: Messages): (Option[String], Option[String]) = {

    (getCeasedMsg(incomeExplanation), getManualUpdateMsg(incomeExplanation), getSameMsg(incomeExplanation), getPayFreqMsg(incomeExplanation)) match {

      case ((Some(ceasedMsg), Some(ceasedMsgEstimate)), _, _, _) => (Some(ceasedMsg), Some(ceasedMsgEstimate))
      case ((None, Some(ceasedMsgEstimate)), _, _, _) => (None, Some(ceasedMsgEstimate))
      case ((Some(ceasedMsg), None), _, _, _) => (Some(ceasedMsg), None)
      case (_, (Some(manualUpdateMsg), Some(manualUpdateMsgEstimate)), _, _) => (Some(manualUpdateMsg), Some(manualUpdateMsgEstimate))
      case (_, (None, Some(manualUpdateMsgEstimate)), _, _) => (None, Some(manualUpdateMsgEstimate))
      case (_, (Some(manualUpdateMsg), None), _, _) => (Some(manualUpdateMsg), None)
      case (_, _, (Some(sameIncomeMsg), Some(sameIncomeEstimateMsg)), _) => (Some(sameIncomeMsg), Some(sameIncomeEstimateMsg))
      case (_, _, (Some(sameIncomeMsg), None), _) => (Some(sameIncomeMsg), None)
      case (_, _, _, (Some(payFreqMsg), Some(payFreqMsgEstimate))) => (Some(payFreqMsg), Some(payFreqMsgEstimate))
      case (_, _, _, (None, Some(payFreqMsgEstimate))) => (None, Some(payFreqMsgEstimate))
      case (_, _, _, (Some(payFreqMsg), None)) => (Some(payFreqMsg), None)

      case _ => (Some(messages("tai.income.calculation.default." + (if (incomeExplanation.isPension) "pension" else "emp"),
        Dates.formatDate(TaxYear().end))),
        Some(messages("tai.income.calculation.default.estimate." + (if (incomeExplanation.isPension) "pension" else "emp"),
          incomeExplanation.grossAmount.getOrElse(BigDecimal(0)))))
    }
  }

  def getSameMsg(incomeExplanation: IncomeExplanation)(implicit messages: Messages): (Option[String], Option[String]) = {

    val startDate = incomeExplanation.startDate.map(strtDt =>
      if (TaxYearResolver.fallsInThisTaxYear(strtDt)) strtDt else TaxYear().start
    )
    if (incomeExplanation.calcAmount.isDefined && (incomeExplanation.calcAmount.getOrElse(BigDecimal(0)) == incomeExplanation.payToDate)) {

      (Some(messages("tai.income.calculation.rti." + (if (incomeExplanation.isPension) "pension" else "emp") +
        ".same", startDate.map(Dates.formatDate).getOrElse(""),
        incomeExplanation.paymentDate.map(Dates.formatDate).getOrElse(""), MoneyPounds(incomeExplanation.payToDate, 0).quantity)), None)
    } else { (None, None) }
  }

  def getCeasedMsg(incomeExplanation: IncomeExplanation)(implicit messages: Messages): (Option[String], Option[String]) = {

    (incomeExplanation.employmentStatus.getOrElse(BigDecimal(1)), incomeExplanation.endDate.isDefined,
      incomeExplanation.cessationPay.isDefined) match {

      case (EmploymentStatus.Ceased.code, true, true) =>
        (Some(messages("tai.income.calculation.rti.ceased." + (if (incomeExplanation.isPension) "pension" else "emp"),
          incomeExplanation.endDate.map(Dates.formatDate).getOrElse(""))), None)
      case (EmploymentStatus.Ceased.code, true, false) =>
        (Some(messages("tai.income.calculation.rti.ceased." + (if (incomeExplanation.isPension) "pension" else "emp") + ".noFinalPay")),
          Some(messages("tai.income.calculation.rti.ceased.noFinalPay.estimate",
            MoneyPounds(incomeExplanation.grossAmount.getOrElse(BigDecimal(0)), 0).quantity)))
      case (EmploymentStatus.PotentiallyCeased.code, false, _) => (Some(messages("tai.income.calculation.rti.ceased." +
        (if (incomeExplanation.isPension) "pension" else "emp") + ".noFinalPay")),
        Some(messages("tai.income.calculation.rti.ceased.noFinalPay.estimate",
          MoneyPounds(incomeExplanation.grossAmount.getOrElse(BigDecimal(0)), 0).quantity)))

      case _ => (None, None)
    }
  }

  def getPayFreqMsg(incomeExplanation: IncomeExplanation)(implicit messages: Messages): (Option[String], Option[String]) = {

    val isMidYear = incomeExplanation.startDate.exists(_.isAfter(TaxYear().start))
    val pensionOrEmp = if (incomeExplanation.isPension) "pension" else "emp"
    def getDefaultPay = if (incomeExplanation.employmentType.contains(1)) defaultPrimaryPay else defaultSecondaryPay

    (incomeExplanation.payFrequency, isMidYear, incomeExplanation.calcAmount.isDefined) match {
      case ((Some(Weekly) | Some(Fortnightly) | Some(FourWeekly) | Some(Monthly) | Some(Quarterly) | Some(BiAnnually)), false, true) =>
        (Some(messages("tai.income.calculation.rti.continuous.weekly." + pensionOrEmp,
          MoneyPounds(incomeExplanation.payToDate, 2).quantity, incomeExplanation.paymentDate.map(Dates.formatDate).getOrElse(""))),
          Some(messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(incomeExplanation.calcAmount.getOrElse(BigDecimal(0)), 0).quantity)))
      case (Some(Annually), false, true) =>
        (Some(messages("tai.income.calculation.rti.continuous.annually." + pensionOrEmp,
          MoneyPounds(incomeExplanation.payToDate, 2).quantity)),
          Some(messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(incomeExplanation.calcAmount.getOrElse(BigDecimal(0)), 0).quantity)))
      case (Some(OneOff), false, true) =>
        (Some(messages("tai.income.calculation.rti.oneOff." + pensionOrEmp,
          MoneyPounds(incomeExplanation.payToDate, 2).quantity)),
          Some(messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(incomeExplanation.calcAmount.getOrElse(BigDecimal(0)), 0).quantity)))
      case (Some(Irregular), false, true) =>
        val estPay = incomeExplanation.grossAmount.getOrElse(throw new NoSuchElementException("incomeExplanation.grossAmount must not be none"))
        (None, Some(messages("tai.income.calculation.rti.irregular." + pensionOrEmp, MoneyPounds(estPay, 0).quantity)))
      case ((Some(Weekly) | Some(Fortnightly) | Some(FourWeekly) | Some(Monthly) | Some(Quarterly) | Some(BiAnnually) | Some(Annually)), true, true) =>
        (Some(messages("tai.income.calculation.rti.midYear.weekly", incomeExplanation.startDate.map(Dates.formatDate).getOrElse(""),
          incomeExplanation.paymentDate.map(Dates.formatDate).getOrElse(""), MoneyPounds(incomeExplanation.payToDate, 2).quantity)),
          Some(messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(incomeExplanation.calcAmount.getOrElse(BigDecimal(0)), 0).quantity)))
      case (Some(OneOff), true, true) =>
        (Some(messages("tai.income.calculation.rti.oneOff." + pensionOrEmp, MoneyPounds(incomeExplanation.payToDate, 2).quantity)),
          Some(messages("tai.income.calculation.rti." + pensionOrEmp + ".estimate",
            MoneyPounds(incomeExplanation.calcAmount.getOrElse(BigDecimal(0)), 0).quantity)))
      case (Some(Irregular), true, true) =>
        val estPay =  incomeExplanation.grossAmount.reduceLeft((x,y) => y)
        (None, Some(messages("tai.income.calculation.rti.irregular." + pensionOrEmp, MoneyPounds(estPay, 0).quantity)))
      case _ => (None, None)
    }
  }

  def getManualUpdateMsg(incomeExpl: IncomeExplanation)(implicit messages: Messages): (Option[String], Option[String]) = {

    (incomeExpl.iabdSource.getOrElse(0), incomeExpl.notificationDate.isDefined, incomeExpl.updateActionDate.isDefined) match {
      case (IabdUpdateSource.ManualTelephone.code, true, true) => (Some(messages("tai.income.calculation.manual.update.phone",
        Dates.formatDate(incomeExpl.updateActionDate.get), Dates.formatDate(incomeExpl.notificationDate.get))),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.ManualTelephone.code, false, false) | (IabdUpdateSource.ManualTelephone.code, false, true) |
           (IabdUpdateSource.ManualTelephone.code, true, false) => (Some(messages("tai.income.calculation.manual.update.phone.withoutDate")),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.Letter.code, true, true) => (Some(messages("tai.income.calculation.manual.update.letter",
        Dates.formatDate(incomeExpl.updateActionDate.get), Dates.formatDate(incomeExpl.notificationDate.get))),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.Letter.code, false, false) | (IabdUpdateSource.Letter.code, false, true) |
           (IabdUpdateSource.Letter.code, true, false) => (Some(messages("tai.income.calculation.manual.update.letter.withoutDate")),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.Email.code, true, true) => (Some(messages("tai.income.calculation.manual.update.email",
        Dates.formatDate(incomeExpl.updateActionDate.get), Dates.formatDate(incomeExpl.notificationDate.get))),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.Email.code, false, false) | (IabdUpdateSource.Email.code, false, true) |
           (IabdUpdateSource.Email.code, true, false) => (Some(messages("tai.income.calculation.manual.update.email.withoutDate")),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.AgentContact.code, _, _) => (Some(messages("tai.income.calculation.agent")),
        Some(messages("tai.income.calculation.agent.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.OtherForm.code | IabdUpdateSource.InformationLetter.code, true, true) =>
        (Some(messages("tai.income.calculation.manual.update.informationLetter", Dates.formatDate(incomeExpl.updateActionDate.get),
          Dates.formatDate(incomeExpl.notificationDate.get))),
          Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.OtherForm.code | IabdUpdateSource.InformationLetter.code, false, false)
           | (IabdUpdateSource.OtherForm.code | IabdUpdateSource.InformationLetter.code, false, true)
           | (IabdUpdateSource.OtherForm.code | IabdUpdateSource.InformationLetter.code, true, false) =>
        (Some(messages("tai.income.calculation.manual.update.informationLetter.withoutDate")),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.Internet.code, true, _) => (Some(messages("tai.income.calculation.manual.update.internet",
        Dates.formatDate(incomeExpl.notificationDate.get))),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case (IabdUpdateSource.Internet.code, false, _) => (Some(messages("tai.income.calculation.manual.update.internet.withoutDate")),
        Some(messages("tai.income.calculation.rti.manual.update.estimate", incomeExpl.grossAmount.getOrElse(BigDecimal(0)))))

      case _ => (None, None)
    }
  }

  def hasPreviousEmployment(employmentStartDate: Option[LocalDate]): Boolean = {
    employmentStartDate.exists(startDate => TaxYear(startDate).compare(TaxYear(TaxYear().start)) < 0)
  }

  /*def getPreviousYearPayments(sessionDetails: SessionData, empId: Int)(implicit messages: Messages):
  (List[RtiPayment], List[String], Option[String], Option[String], Boolean, Html, Boolean) = {

    val taxSummaryDetails = sessionDetails.taxSummaryDetailsCY
    val employment = taxSummaryDetails.currentYearAccounts.flatMap(_.nps).map(_.incomes).flatMap(_.find(_.employmentId.contains(empId))).toList

    val prevourYearRtiData = taxSummaryDetails.previousYearAccount.flatMap(_.rti)
    val previousYearRtiEmployments = prevourYearRtiData.map(_.employments).getOrElse(Nil)

    val previousYearEmploymentPayments = employment.flatMap { n =>
      val filteredRtiEmployments = previousYearRtiEmployments.filter { r =>
        n.payeRef == r.payeRef &&
          n.taxDistrict.contains(r.officeRefNo.toInt)
      } match {
        case x if x.size > 1 => x.find(_.currentPayId == n.employmentRecord.flatMap(_.worksNumber))
        case x => x.headOption
      }
      filteredRtiEmployments.toList.flatMap(_.payments)
    }

    val previousYearEmploymentEyu = employment.flatMap { n =>
      val filteredRtiEmployments = previousYearRtiEmployments.filter { r =>
        n.payeRef == r.payeRef &&
          n.taxDistrict.contains(r.officeRefNo.toInt)
      } match {
        case x if x.size > 1 => x.find(_.currentPayId == n.employmentRecord.flatMap(_.worksNumber))
        case x => x.headOption
      }
      filteredRtiEmployments.toList.flatMap(_.eyu)
    }

    val incomeExplanations = taxSummaryDetails.incomeData.map(_.incomeExplanations)

    val (employerName, isPension) = incomeExplanations match {
      case Some(incomeExplanations) =>
        val income = incomeExplanations.find(_.incomeId == empId)
        (income.map(_.employerName), income.map(_.isPension))
      case _ => (None, None)
    }

    val iFormLink = formHtml(isPension)
    val rtiStatus = taxSummaryDetails.previousYearAccount.flatMap(_.rtiStatus)

    val isRtiDown = rtiStatus.map(_.status) match {
      case Some(Status.INTERNAL_SERVER_ERROR) => true
      case _ => false
    }

    (previousYearEmploymentPayments, getEyuMessage(previousYearEmploymentEyu, employerName.getOrElse("")), employerName,
      getNotMatchingTotalMessage(previousYearEmploymentPayments, isPension), isPension.getOrElse(false), iFormLink, isRtiDown)
  }*/

  private def formHtml(isPension: Option[Boolean])(implicit messages: Messages): Html = {
    Html(messages("tai.income.calculation.detailsWrongIform." + (if (isPension.getOrElse(false)) {
      "pension"
    } else {
      "emp"
    }), Link.toInternalPage(url = ApplicationConfig.incomeFromEmploymentPensionLinkUrl,
      value = Some(messages("tai.income.calculation.detailsWrongIformLink")),
      dataAttributes = Some(Map("journey-click" -> "check-income-tax:Outbound Link:wrong-other-income-iform"))).toHtml))
  }

  def getNotMatchingTotalMessage(employmentPayments: List[RtiPayment], isPension: Option[Boolean])(implicit messages: Messages): Option[String] = {

    val isTotalEqual = (employmentPayments.map(x => x.taxed).sum == employmentPayments.lastOption.map(_.taxedYTD).getOrElse(0) &&
      employmentPayments.map(_.taxablePay).sum == employmentPayments.lastOption.map(_.taxablePayYTD).getOrElse(0)
      && employmentPayments.flatMap(x => x.nicPaid).sum == employmentPayments.lastOption.flatMap(_.nicPaidYTD).getOrElse(0))

    (isTotalEqual, isPension.getOrElse(false)) match {
      case (false, true) => Some(messages("tai.income.calculation.totalNotMatching.pension.message"))
      case (false, false) => Some(messages("tai.income.calculation.totalNotMatching.emp.message"))
      case _ => None
    }
  }

  /*def getEyuMessage(rtiEyu: List[RtiEyu], employerName: String)(implicit messages: Messages) = {

    val lessOrMore = (amount: BigDecimal) => amount.abs.toString + (if(amount > 0) " more" else " less")
    val eyuObjectList = flattenEyuObject(rtiEyu)
    var messages = new ListBuffer[String]()

    eyuObjectList.map {
      eyuObject =>
        val (eyuProperty, eyuAmount, date) = eyuObject
        (eyuProperty, eyuAmount, date, eyuObjectList.size) match {
          case ("TaxablePayDelta", amount, date, 1) =>
            messages += messages("tai.income.calculation.eyu.single.taxableincome", Dates.formatDate(date), lessOrMore(amount))
          case ("TotalTaxDelta", amount, date, 1) =>
            messages += messages("tai.income.calculation.eyu.single.taxPaid", Dates.formatDate(date), lessOrMore(amount))
          case ("EmpeeContribnsDelta", amount, date, 1) =>
            messages += messages("tai.income.calculation.eyu.single.nationalInsurance", Dates.formatDate(date), lessOrMore(amount))
          case ("TaxablePayDelta", amount, date, size) if size > 1 =>
            messages += messages("tai.income.calculation.eyu.multi.taxableincome", Dates.formatDate(date), lessOrMore(amount))
          case ("TotalTaxDelta", amount, date, size) if size > 1 =>
            messages += messages("tai.income.calculation.eyu.multi.taxPaid", Dates.formatDate(date), lessOrMore(amount))
          case ("EmpeeContribnsDelta", amount, date, size) if size > 1 =>
            messages += messages("tai.income.calculation.eyu.multi.nationalInsurance", Dates.formatDate(date), lessOrMore(amount))
          case _ => messages
        }
    }

    messages.toList
  }*/

  private def flattenEyuObject(rtiEyu: List[RtiEyu]): List[(String, BigDecimal, LocalDate)] = {

    var eyuObjectMap = new ListBuffer[(String, BigDecimal, LocalDate)]()
    val isEligible = (optionalAmount: Option[BigDecimal]) => optionalAmount.isDefined && (optionalAmount.get != 0)
    rtiEyu.foreach {
      eyu =>
        if (isEligible(eyu.totalTaxDelta)) {
          eyuObjectMap += ((TOTAL_TAX_DELTA, eyu.totalTaxDelta.get, eyu.rcvdDate))
        }
        if (isEligible(eyu.taxablePayDelta)) {
          eyuObjectMap += ((TAXABLE_PAY_DELTA, eyu.taxablePayDelta.get, eyu.rcvdDate))
        }
        if (isEligible(eyu.empeeContribnsDelta)) {
          eyuObjectMap += ((EMPEE_CONTRIBNS_DELTA, eyu.empeeContribnsDelta.get, eyu.rcvdDate))
        }
    }
    eyuObjectMap.toList
  }

}
