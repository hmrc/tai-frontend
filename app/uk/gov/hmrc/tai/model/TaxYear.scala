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

package uk.gov.hmrc.tai.model

import play.api.libs.json._
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.util.constants.TaiConstants.LondonEuropeTimezone

import java.time.{LocalDate, ZoneId}
import javax.inject.Inject

case class TaxYear(year: Int) extends Ordered[TaxYear] {
  require(year.toString.length == 4, "Invalid year")
  val TaxMonthApril = 4
  val StartDate = 6
  val EndDate = 5
  val One = 1

  def start: LocalDate = LocalDate.of(year, TaxMonthApril, StartDate)
  def end: LocalDate = LocalDate.of(year + One, TaxMonthApril, EndDate)
  def next: TaxYear = TaxYear(year + One)
  private def next(add: Int) = TaxYear(year + add)
  def prev: TaxYear = TaxYear(year - One)
  def startPrev: LocalDate = LocalDate.of(prev.year, TaxMonthApril, StartDate)
  def endPrev: LocalDate = LocalDate.of(prev.year + One, TaxMonthApril, EndDate)
  def compare(that: TaxYear): Int = this.year compare that.year
  def twoDigitRange: String = s"${start.getYear % 100}-${end.getYear % 100}"
  def fourDigitRange: String = s"${start.getYear}-${end.getYear}"
  def within(currentDate: LocalDate): Boolean =
    (currentDate.isEqual(start) || currentDate.isAfter(start)) &&
      (currentDate.isBefore(end) || currentDate.isEqual(end))
}

object TaxYear {
  def apply(from: LocalDate = LocalDate.now(ZoneId.of(LondonEuropeTimezone))): TaxYear = {
    val naiveYear = TaxYear(from.getYear)
    if (from isBefore naiveYear.start) {
      naiveYear.prev
    } else { naiveYear }
  }

  def apply(from: String): TaxYear = {
    val YearRange = "([0-9]+)-([0-9]+)".r

    object Year {
      val SimpleYear = "([12][0-9])?([0-9]{2})".r
      val NineteenthCentury = 1900
      val TwentiethCentury = 2000
      val Century = 100
      val CutOffYear = 70

      def unapply(in: String): Option[Int] = in match {
        case SimpleYear(cenStr, yearStr) =>
          val year = yearStr.toInt
          val century = Option(cenStr).filter(_.nonEmpty) match {
            case None if year > CutOffYear => NineteenthCentury
            case None                      => TwentiethCentury
            case Some(x)                   => x.toInt * Century
          }
          Some(century + year)
        case _ => None
      }
    }

    from match {
      case Year(year) => TaxYear(year)
      case YearRange(Year(fYear), Year(tYear)) if tYear == fYear + 1 =>
        TaxYear(fYear)
      case x => throw new IllegalArgumentException(s"Cannot parse $x")
    }
  }

  def fromNow(yearsFromNow: Int): TaxYear = {
    val currentYear: TaxYear = TaxYear()

    if (yearsFromNow == 0) {
      currentYear
    } else {
      currentYear.next(yearsFromNow)
    }
  }

  implicit val formatTaxYear: Format[TaxYear] = new Format[TaxYear] {
    override def reads(j: JsValue): JsResult[TaxYear] = j match {
      case JsNumber(n) => JsSuccess(TaxYear(n.toInt))
      case x           => JsError(s"Expected JsNumber, found $x")
    }
    override def writes(v: TaxYear): JsValue = JsNumber(v.year)
  }

}
