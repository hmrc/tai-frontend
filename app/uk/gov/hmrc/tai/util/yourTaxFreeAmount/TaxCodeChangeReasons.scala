/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import javax.inject.Inject
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodePair, TaxCodePairs}

class TaxCodeChangeReasons @Inject()() {

  def reasons(taxCodeChange: TaxCodeChange)(implicit messages: Messages): List[String] = {

    val taxCodePairs = TaxCodePairs(taxCodeChange)
    val employmentReasons = primaryEmploymentsChanged(taxCodePairs.primaryPairs) ++
      secondaryEmploymentsChanged(taxCodePairs.unMatchedPreviousCodes, taxCodePairs.unMatchedCurrentCodes)

    employmentReasons ++ incomeSourceCountReason(employmentReasons, taxCodeChange)
  }

  private def secondaryEmploymentsChanged(
    unMatchedPreviousCodes: List[TaxCodePair],
    unMatchedCurrentCodes: List[TaxCodePair])(implicit messages: Messages): List[String] = {

    val previous = unMatchedPreviousCodes.flatMap(_.previous).map(record => record.employerName)

    val currentTaxCodeRecords: List[TaxCodeRecord] = unMatchedCurrentCodes.flatMap(_.current)
    val current = currentTaxCodeRecords.map(record => record.employerName)

    val uniquePrevious = previous.distinct.sorted
    val uniqueCurrent = current.distinct.sorted

    val currentAndPreviousEmployerNamesAreSame
      : Boolean = (uniquePrevious == uniqueCurrent) && (uniquePrevious ++ uniqueCurrent).nonEmpty

    if (currentAndPreviousEmployerNamesAreSame) {
      genericMessage
    } else {
      removeEmployerMessage(previous) ++ addEmployerMessages(currentTaxCodeRecords)
    }
  }

  private def primaryEmploymentsChanged(primaryPairs: List[TaxCodePair])(implicit messages: Messages): List[String] =
    primaryPairs flatMap { primaryPair: TaxCodePair =>
      val current = primaryPair.current.map(_.employerName)
      val previous = primaryPair.previous.map(_.employerName)

      (current, previous) match {
        case (Some(current), Some(previous)) if (current != previous) => {
          removeEmployerMessage(List(previous)) ++ addSingleEmployerMessage(primaryPair.current)
        }
        case (Some(current), Some(previous)) if isDifferentPayRollWithSameEmployerName(primaryPair) => {
          genericMessage
        }
        case _ => List.empty[String]
      }
    }

  private def isDifferentPayRollWithSameEmployerName(primaryPair: TaxCodePair): Boolean = {

    val isEmployerNameSame = {
      val currentName = primaryPair.current.map(_.employerName)
      val previousName = primaryPair.previous.map(_.employerName)

      currentName == previousName
    }

    val isPayRollSame = {
      val previousPayRoll = primaryPair.previous.flatMap(_.payrollNumber)
      val currentPayRoll = primaryPair.current.flatMap(_.payrollNumber)

      previousPayRoll == currentPayRoll
    }

    isEmployerNameSame && !isPayRollSame
  }

  private def removeEmployerMessage(employerNames: List[String])(implicit messages: Messages): List[String] =
    employerNames map (name => messages("tai.taxCodeComparison.removeEmployer", name))

  private def addSingleEmployerMessage(taxCodeRecord: Option[TaxCodeRecord])(
    implicit messages: Messages): List[String] =
    taxCodeRecord.fold(List.empty[String]) { record =>
      if (record.pensionIndicator) {
        List(messages("tai.taxCodeComparison.add.pension", record.employerName))
      } else {
        List(messages("tai.taxCodeComparison.add.employer", record.employerName))
      }
    }

  private def addEmployerMessages(taxCodeRecords: List[TaxCodeRecord])(implicit messages: Messages): List[String] =
    taxCodeRecords.flatMap(x => addSingleEmployerMessage(Some(x)))

  private def genericMessage(implicit messages: Messages): List[String] =
    List(messages("taxCode.change.yourTaxCodeChanged.paragraph"))

  private def filterOutGenericMessage(reasons: List[String])(implicit messages: Messages): List[String] =
    reasons.filterNot(_ == genericMessage.head)

  private def incomeSourceCountReason(reasons: List[String], taxCodeChange: TaxCodeChange)(
    implicit messages: Messages): List[String] =
    if (filterOutGenericMessage(reasons).nonEmpty) {
      List(incomeSourceCountMessage(taxCodeChange))
    } else {
      List.empty[String]
    }

  private def incomeSourceCountMessage(taxCodeChange: TaxCodeChange)(implicit messages: Messages): String = {

    val employmentMessage = {
      if (taxCodeChange.currentEmploymentCount > 1) {
        messages("tai.taxCodeComparison.employments.count", taxCodeChange.currentEmploymentCount)
      } else {
        messages("tai.taxCodeComparison.employment.count", taxCodeChange.currentEmploymentCount)
      }
    }

    val pensionMessage = {
      if (taxCodeChange.currentPensionCount > 1) {
        messages("tai.taxCodeComparison.pensions.count", taxCodeChange.currentPensionCount)
      } else {
        messages("tai.taxCodeComparison.pension.count", taxCodeChange.currentPensionCount)
      }
    }

    (taxCodeChange.currentEmploymentCount > 0, taxCodeChange.currentPensionCount > 0) match {
      case (true, true) =>
        messages(
          "tai.taxCodeComparison.incomeSources.count",
          taxCodeChange.currentEmploymentCount + taxCodeChange.currentPensionCount)
      case (true, false) => employmentMessage
      case (false, true) => pensionMessage
    }
  }
}
