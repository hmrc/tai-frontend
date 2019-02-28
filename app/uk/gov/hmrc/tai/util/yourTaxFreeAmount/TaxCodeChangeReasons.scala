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

package uk.gov.hmrc.tai.util.yourTaxFreeAmount

import com.google.inject.Inject
import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}
import uk.gov.hmrc.tai.viewModels.taxCodeChange.{TaxCodePair, TaxCodePairs}

class TaxCodeChangeReasons @Inject()() {

  def reasons(taxCodeChange: TaxCodeChange)(implicit messages: Messages): Seq[String] = {

    val taxCodePairs = TaxCodePairs(taxCodeChange)
    primaryEmploymentsChanged(taxCodePairs.primaryPairs) ++
      secondaryEmploymentsChanged(taxCodePairs.unMatchedPreviousCodes, taxCodePairs.unMatchedCurrentCodes)
  }

  private def secondaryEmploymentsChanged(unMatchedPreviousCodes: Seq[TaxCodePair],
                                          unMatchedCurrentCodes: Seq[TaxCodePair])
                                         (implicit messages: Messages): Seq[String] = {

    val previous = unMatchedPreviousCodes.flatMap(_.previous).map(record => record.employerName)

    val currentTaxCodeRecords = unMatchedCurrentCodes.flatMap(_.current)
    val current = currentTaxCodeRecords.map(record => record.employerName)

    val uniquePrevious = previous.distinct.sorted
    val uniqueCurrent = current.distinct.sorted

    val currentAndPreviousEmployerNamesAreSame: Boolean = (uniquePrevious == uniqueCurrent) && (uniquePrevious ++ uniqueCurrent).nonEmpty

    if (currentAndPreviousEmployerNamesAreSame) {
      genericMessage
    } else {
      removeEmployerMessage(previous) ++ addEmployerMessages(currentTaxCodeRecords)
    }
  }

  private def primaryEmploymentsChanged(primaryPairs: Seq[TaxCodePair])(implicit messages: Messages): Seq[String] = {
    primaryPairs flatMap { primaryPair: TaxCodePair =>
      val current = primaryPair.current.map(_.employerName)
      val previous = primaryPair.previous.map(_.employerName)

      (current, previous) match {
        case (Some(current), Some(previous)) if (current != previous) => {
          removeEmployerMessage(Seq(previous)) ++ addSingleEmployerMessage(primaryPair.current)
        }
        case (Some(current), Some(previous)) if isDifferentPayRollWithSameEmployerName(primaryPair) => {
          genericMessage
        }
        case _ => Seq.empty[String]
      }
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

  private def removeEmployerMessage(employerNames: Seq[String])(implicit messages: Messages): Seq[String] = {
    employerNames map (name => messages("tai.taxCodeComparison.removeEmployer", name))
  }

  private def addSingleEmployerMessage(taxCodeRecord: Option[TaxCodeRecord])(implicit messages: Messages): Seq[String] = {
    taxCodeRecord.fold(Seq.empty[String]) {
      record =>
        if (record.pensionIndicator) {
          Seq(messages("tai.taxCodeComparison.add.pension", record.employerName))
        } else {
          Seq(messages("tai.taxCodeComparison.add.employer", record.employerName))
        }
    }
  }

  private def addEmployerMessages(taxCodeRecords: Seq[TaxCodeRecord])(implicit messages: Messages): Seq[String] = {
    taxCodeRecords.flatMap(x => addSingleEmployerMessage(Some(x)))
  }

  private def genericMessage(implicit messages: Messages): Seq[String] = {
    Seq(messages("taxCode.change.yourTaxCodeChanged.paragraph"))
  }
}
