/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import play.api.i18n.Messages
import uk.gov.hmrc.tai.model.domain.{TaxCodeChange, TaxCodeRecord}

class NoMatchPossibleException extends Exception

case class TaxCodePair(previous: Option[TaxCodeRecord], current: Option[TaxCodeRecord])

case class TaxCodePairs(
  primaryPairs: Seq[TaxCodePair],
  secondaryPairs: Seq[TaxCodePair],
  unMatchedPreviousCodes: Seq[TaxCodePair],
  unMatchedCurrentCodes: Seq[TaxCodePair]) {

  def combinedTaxCodePairs: Seq[TaxCodePair] =
    primaryPairs ++
      secondaryPairs ++
      unMatchedPreviousCodes ++
      unMatchedCurrentCodes
}

object TaxCodePairs {
  def apply(taxCodeChange: TaxCodeChange): TaxCodePairs = {

    val pairs = createAllPairs(taxCodeChange.previous, taxCodeChange.current)

    TaxCodePairs(
      primaryPairs(pairs),
      secondaryPairs(pairs),
      unMatchedPreviousCodes(pairs),
      unMatchedCurrentCodes(pairs)
    )
  }

  private val createAllPairs: (Seq[TaxCodeRecord], Seq[TaxCodeRecord]) => Seq[TaxCodePair] =
    (previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]) => {

      def innerCreateAllPairs(
        previous: Seq[TaxCodeRecord],
        current: Seq[TaxCodeRecord],
        acc: Seq[TaxCodePair] = Seq.empty): Seq[TaxCodePair] =
        (previous, current) match {
          case (Nil, Nil) => acc
          case (Nil, head :: tail) =>
            innerCreateAllPairs(Seq.empty, tail, acc ++ Seq(TaxCodePair(None, Some(head))))
          case (head :: tail, Nil) =>
            innerCreateAllPairs(tail, Seq.empty, acc ++ Seq(TaxCodePair(Some(head), None)))
          case (pHead :: pTail, curr) => {
            curr
              .find(isMatchingPair(_, pHead))
              .fold(innerCreateAllPairs(pTail, curr, acc ++ Seq(TaxCodePair(Some(pHead), None)))) { matching =>
                {
                  val rest = curr.filter(record => record != matching)
                  innerCreateAllPairs(pTail, rest, acc ++ Seq(TaxCodePair(Some(pHead), Some(matching))))
                }
              }
          }
        }

      innerCreateAllPairs(previous, current, Seq.empty)
    }

  private def primaryPairs(pairs: Seq[TaxCodePair]): Seq[TaxCodePair] =
    pairs.filter(taxCodeRecordPair => {
      taxCodeRecordPair.previous.isDefined && taxCodeRecordPair.current.isDefined && taxCodeRecordPair.current
        .exists(_.primary)
    })

  private def secondaryPairs(pairs: Seq[TaxCodePair]): Seq[TaxCodePair] =
    pairs.filter(taxCodeRecordPair => {
      taxCodeRecordPair.previous.isDefined && taxCodeRecordPair.current.isDefined && !taxCodeRecordPair.current
        .exists(_.primary)
    })

  private def unMatchedCurrentCodes(pairs: Seq[TaxCodePair]): Seq[TaxCodePair] =
    pairs.filter(taxCodeRecordPair => taxCodeRecordPair.previous.isEmpty)

  private def unMatchedPreviousCodes(pairs: Seq[TaxCodePair]): Seq[TaxCodePair] =
    pairs.filter(taxCodeRecordPair => taxCodeRecordPair.current.isEmpty)

  private def isMatchingPair(record1: TaxCodeRecord, record2: TaxCodeRecord): Boolean = {
    def equivalentPair(r1: TaxCodeRecord, r2: TaxCodeRecord): Boolean =
      r1.primary == r2.primary && r1.payrollNumber == r2.payrollNumber && r1.employerName == r2.employerName

    (record1.primary && record2.primary) || equivalentPair(record1, record2)
  }
}
