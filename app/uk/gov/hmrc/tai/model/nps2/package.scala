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

package hmrc

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.slf4j._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.tai.model.nps2.TaxDetail

import scala.language.implicitConversions

package object nps2 {

  implicit val log: Logger = LoggerFactory.getLogger(this.getClass)

  def enumerationFormat(a: Enumeration) = new Format[a.Value] {
    def reads(json: JsValue) = JsSuccess(a.withName(json.as[String]))
    def writes(v: a.Value) = JsString(v.toString)
  }

  def enumerationNumFormat(a: Enumeration) = new Format[a.Value] {
    def reads(json: JsValue) = JsSuccess(a(json.as[Int]))
    def writes(v: a.Value) = JsNumber(v.id)
  }

  implicit val formatLocalDate: Format[LocalDate] = Format(
    new Reads[LocalDate]{
      val dateRegex = """^(\d\d)/(\d\d)/(\d\d\d\d)$""".r
      override def reads(json: JsValue): JsResult[LocalDate] = json match {
        case JsString(dateRegex(d, m, y)) =>
          JsSuccess(new LocalDate(y.toInt, m.toInt, d.toInt))
        case invalid => JsError(ValidationError(
          s"Invalid date format [dd/MM/yyyy]: $invalid"))
      }
    },
    new Writes[LocalDate]{
      val dateFormat = DateTimeFormat.forPattern("dd/MM/yyyy")
      override def writes(date: LocalDate): JsValue =
        JsString(dateFormat.print(date))
    }
  )

  import nps2.TaxObject.Type.{Value => TaxObjectType}

  implicit val formatTaxBand: Format[TaxBand] = (
    (__ \ "bandType").formatNullable[String] and
    (__ \ "code").formatNullable[String] and
    (__ \ "income").formatNullable[BigDecimal].
      inmap[BigDecimal](_.getOrElse(0), Some(_)) and
    (__ \ "tax").formatNullable[BigDecimal].
      inmap[BigDecimal](_.getOrElse(0), Some(_)) and
    (__ \ "lowerBand").formatNullable[BigDecimal] and
    (__ \ "upperBand").formatNullable[BigDecimal] and
    (__ \ "rate").format[BigDecimal]
  )(TaxBand.apply, unlift(TaxBand.unapply))

  implicit val formatliabilityMap: Format[Map[TaxObjectType, TaxDetail]] = {

    val fieldNames: Map[TaxObject.Type.Value,String] =
      TaxObject.Type.values.toSeq.map{ x =>
        (x, x.toString.head.toLower + x.toString.tail)
      }.toMap

    new Format[Map[TaxObjectType, TaxDetail]] {
      def reads(json: JsValue): JsResult[Map[TaxObjectType, TaxDetail]] = {
        JsSuccess(fieldNames.mapValues { x => TaxDetail(taxBands =
          (json \ x \ "taxBands").asOpt[Seq[TaxBand]],
          totalTax = (json \ x \ "totalTax").asOpt[BigDecimal],
          totalIncome = (json \ x \ "totalIncome").asOpt[BigDecimal],
          totalTaxableIncome = (json \ x \ "totalTaxableIncome").asOpt[BigDecimal]
        )
        })
      }

      def writes(data: Map[TaxObjectType, TaxDetail]): JsValue =
        JsObject(data.toSeq.map{ case (f,v) =>
          (fieldNames(f), JsObject(
            Seq(
              ("taxBands",Json.toJson(v.taxBands)),
              ("totalTax", v.totalTax.map( x => JsNumber(x)).getOrElse(JsNull)),
              ("totalTaxableIncome", v.totalTaxableIncome.map(x => JsNumber(x)).getOrElse(JsNull)),
              ("totalIncome", v.totalIncome.map( x => JsNumber(x)).getOrElse(JsNull))
            )))
        })
    }

  }

}
