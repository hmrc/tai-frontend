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

import hmrc.nps2.Income.{EmploymentStatus, IncomeType}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.slf4j._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.tai.model.nps2.{TaxDetail, IabdUpdateSource}
import uk.gov.hmrc.tai.model.tai.JsonExtra

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

  implicit val formatIabd: Format[Iabd] = (
    (__ \ "grossAmount").format[BigDecimal] and
      (__ \ "type").format[Int].
        inmap[IabdType](IabdType(_), _.code) and
      (__ \ "source").format[Int].
        inmap[IabdUpdateSource](IabdUpdateSource(_), _.code) and
      (__ \ "typeDescription").formatNullable[String].inmap[String](
        _.getOrElse(""), Some(_)
      ) and
      (__ \ "employmentSequenceNumber").formatNullable[Int]
    )(Iabd.apply, unlift(Iabd.unapply))

  implicit val formatIabdList: Format[List[Iabd]] =
    JsonExtra.bodgeList[Iabd]

  implicit val formatComponent: Format[Component] = (
    (__ \ "amount").format[BigDecimal] and
      (__ \ "sourceAmount").formatNullable[BigDecimal] and
      (__ \ "iabdSummaries").format[Seq[Iabd]]
    )(Component.apply, unlift(Component.unapply))

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

  implicit val formatIncome: Format[Income] = Format(
    new Reads[Income]{
      def reads(json: JsValue) = {
        val iType = (
          (json \ "jsaIndicator").asOpt[Boolean].getOrElse(false),
          (json \ "pensionIndicator").asOpt[Boolean].getOrElse(false),
          (json \ "otherIncomeSourceIndicator").asOpt[Boolean].getOrElse(false)
          ) match {
          case (true,  false, _) => IncomeType.JobSeekersAllowance
          case (false, true , _) => IncomeType.Pension
          case (false, false, true ) => IncomeType.OtherIncome
          case (false, false, false) => IncomeType.Employment
          case (jsa,pen,oth) => throw new IllegalArgumentException(
            s"Unknown Income Type (jsa:$jsa, pension:$pen, other:$oth)")
        }

        JsSuccess{Income(
          (json \ "employmentId").asOpt[Int],
          (json \ "employmentType").as[Int] == 1,
          iType,
          (json \ "employmentStatus").asOpt[Int] match {
            case Some(1) => EmploymentStatus.Live
            case Some(2) => EmploymentStatus.Ceased
            case Some(3) => EmploymentStatus.PotentiallyCeased
          },
          (json \ "employmentTaxDistrictNumber").asOpt[Int],
          (json \ "employmentPayeRef").asOpt[String].getOrElse(""),
          (json \ "name").asOpt[String].getOrElse(""),
          (json \ "worksNumber").asOpt[String],
          (json \ "taxCode").asOpt[String].getOrElse(""),
          (json \ "potentialUnderpayment").asOpt[BigDecimal].getOrElse(0),
          (json \ "employmentRecord").asOpt[NpsEmployment]
        )}
      }
    },
    new Writes[Income]{
      def writes(v: Income) = JsObject(Seq(
        "employmentId" -> v.employmentId.map{
          x => JsNumber(x)
        }.getOrElse{JsNull},
        "employmentType" -> JsNumber(if (v.isPrimary) 1 else 2),
        "employmentStatus" -> JsNumber(v.status.code),
        "employmentTaxDistrictNumber" -> v.taxDistrict.map(x =>
          JsNumber(x)).getOrElse(JsNull),
        "employmentPayeRef" -> JsString(v.payeRef),
        "pensionIndicator" -> JsBoolean(v.incomeType == IncomeType.Pension),
        "jsaIndicator" -> JsBoolean(
          v.incomeType == IncomeType.JobSeekersAllowance),
        "otherIncomeSourceIndicator" -> JsBoolean(
          v.incomeType == IncomeType.OtherIncome),
        "name" -> JsString(v.name),
        "endDate" -> (v.status match {
          case Income.EmploymentStatus.Ceased => Json.toJson(Income.EmploymentStatus.Ceased.code)
          case _ => JsNull
        }),
        "worksNumber" -> v.worksNumber.map{JsString}.getOrElse{JsNull},
        "taxCode" -> JsString(v.taxCode),
        "potentialUnderpayment" -> JsNumber(v.potentialUnderpayment),
        "employmentRecord" -> v.employmentRecord.map{
          x => Json.toJson(x)
        }.getOrElse{JsNull}
      ))
    }
  )

  implicit val formatNpsEmployment: Format[NpsEmployment] = (
    (__ \ "employerName").formatNullable[String] and
      (__ \ "employmentType").format[Int].inmap[Boolean](
        _ == 1,
        x => if (x) 1 else 2
      ) and
      (__ \ "sequenceNumber").format[Int] and
      (__ \ "worksNumber").formatNullable[String] and
      (__ \ "taxDistrictNumber").format[String].inmap[Int](
        a =>  a.toInt,
        x => x.toString
      ) and
      (__ \ "iabds").formatNullable[List[Iabd]].
        inmap[List[Iabd]](_.getOrElse(Nil), Some(_)) and
      (__ \ "cessationPayThisEmployment").formatNullable[BigDecimal] and
      (__ \ "startDate").format[LocalDate]
    )(NpsEmployment.apply, unlift(NpsEmployment.unapply))

  implicit val formatTaxAccount: Format[TaxAccount] = (
    (__ \ "taxAcccountId").formatNullable[Long] and
      (__ \ "date").formatNullable[LocalDate] and
      (__ \ "totalEstTax").formatNullable[BigDecimal].
        inmap[BigDecimal](_.getOrElse(0), Some(_)) and
      (__ \ "totalLiability").format[Map[TaxObjectType, TaxDetail]] and
      (__ \ "incomeSources").formatNullable[Seq[Income]].
        inmap[Seq[Income]](_.getOrElse(Nil), Some(_))
    )(TaxAccount.apply, unlift(TaxAccount.unapply))

  implicit val formatHon = enumerationNumFormat(NpsPerson.Honorific)

  implicit val formatPersonName: Format[NpsPerson.Name] = (
    (__ \ "title").formatNullable[NpsPerson.Honorific.Value] and
      (__ \ "firstForenameOrInitial").format[String] and
      (__ \ "secondForenameOrInitial").formatNullable[String] and
      (__ \ "surname").format[String] and
      (__ \ "start").format[LocalDate] and
      (__ \ "end").formatNullable[LocalDate]
    )(NpsPerson.Name.apply, unlift(NpsPerson.Name.unapply))

  implicit val formatGender = enumerationFormat(NpsPerson.Gender)

  implicit val formatNameSeq: Format[Seq[NpsPerson.Name]] = Format(
    new Reads[Seq[NpsPerson.Name]] {
      def reads(j: JsValue) = j match {
        case o: JsObject => JsSuccess(o.values.map{_.as[NpsPerson.Name]}.toList)
        case JsArray(v) => JsSuccess(v.map{_.as[NpsPerson.Name]})
        case a => JsError(s"Expecting JsObject or JsArray - found $a")
      }
    },
    new Writes[Seq[NpsPerson.Name]] {
      def writes(ps: Seq[NpsPerson.Name]) = JsObject(
        ps.zipWithIndex.map{ case (a,b) =>
          (b.toString, Json.toJson(a))
        })
    }
  )

  implicit val formatPerson: Format[NpsPerson] = (
    (__ \ "nino").format[String] and
      (__ \ "names").format[Seq[NpsPerson.Name]] and
      (__ \ "sex").format[NpsPerson.Gender.Value] and
      (__ \ "dateOfBirth").format[LocalDate] and
      (__ \ "dateOfDeath").formatNullable[LocalDate]
    )(NpsPerson.apply, unlift(NpsPerson.unapply))

}
