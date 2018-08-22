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

package uk.gov.hmrc.tai.model.domain

case class TaxCodePair(previous: Option[TaxCodeRecord], current: Option[TaxCodeRecord])
case class TaxCodePairs(pairs: Seq[TaxCodePair])

object TaxCodePairs {
  def apply(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]): TaxCodePairs = {
    new TaxCodePairs(
      primaryPairs(previous, current) ++
        secondaryPairs(previous, current) ++
        unMatchedPreviousCodes(previous, current) ++
        unMatchedCurrentCodes(previous, current)
    )
  }

  def primaryPairs(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]): Seq[TaxCodePair] = {
    matchedTaxCodes(previous, current).filter(taxCodeRecordPair => taxCodeRecordPair.current.exists(_.primary))
  }

  def secondaryPairs(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]): Seq[TaxCodePair] = {
    matchedTaxCodes(previous, current).filterNot(taxCodeRecordPair => taxCodeRecordPair.current.exists(_.primary))
  }

  def unMatchedCurrentCodes(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]): Seq[TaxCodePair] = {
    val unpairedRecords = current.filterNot(record => matchedTaxCodes(previous, current).map(_.current).contains(Some(record)))

    unpairedRecords.map(record => TaxCodePair(None, Some(record)))
  }

  def unMatchedPreviousCodes(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]): Seq[TaxCodePair] = {
    val unpairedRecords = previous.filterNot(record => matchedTaxCodes(previous, current).map(_.previous).contains(Some(record)))

    unpairedRecords.map(record => TaxCodePair(Some(record), None))
  }

  def matchedTaxCodes(previous: Seq[TaxCodeRecord], current: Seq[TaxCodeRecord]): Seq[TaxCodePair] = {
    for {
      p <- previous
      c <- current
      if (p.primary && c.primary) || p.payrollNumber == c.payrollNumber
    } yield TaxCodePair(Some(p), Some(c))
  }
}