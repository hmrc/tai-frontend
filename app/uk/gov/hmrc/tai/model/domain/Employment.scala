/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Format, JsPath, JsValue, Json, Reads, Writes}
import play.api.libs.ws.BodyWritable
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncomeSourceStatus

import java.time.LocalDate

case class Employment(
  name: String,
  employmentStatus: TaxCodeIncomeSourceStatus,
  payrollNumber: Option[String],
  startDate: Option[LocalDate],
  endDate: Option[LocalDate],
  annualAccounts: Seq[AnnualAccount],
  taxDistrictNumber: String,
  payeNumber: String,
  sequenceNumber: Int,
  cessationPay: Option[BigDecimal],
  hasPayrolledBenefit: Boolean,
  receivingOccupationalPension: Boolean
) {

  lazy val latestAnnualAccount: Option[AnnualAccount] = if (annualAccounts.isEmpty) None else Some(annualAccounts.max)
}

object Employment {
  private def filterDate(dateOption: Option[LocalDate]): Option[LocalDate] =
    dateOption.filter(_.isAfter(LocalDate.of(1950, 1, 1)))

  implicit val reads: Reads[Employment] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "employmentStatus").read[TaxCodeIncomeSourceStatus] and
      (JsPath \ "payrollNumber").readNullable[String] and
      (JsPath \ "startDate").readNullable[LocalDate].map(filterDate) and
      (JsPath \ "endDate").readNullable[LocalDate] and
      (JsPath \ "annualAccounts").read[Seq[AnnualAccount]] and
      (JsPath \ "taxDistrictNumber").read[String] and
      (JsPath \ "payeNumber").read[String] and
      (JsPath \ "sequenceNumber").read[Int] and
      (JsPath \ "cessationPay").readNullable[BigDecimal] and
      (JsPath \ "hasPayrolledBenefit").read[Boolean] and
      (JsPath \ "receivingOccupationalPension").read[Boolean]
  )(Employment.apply _)

  implicit val writes: Writes[Employment] = Json.writes[Employment]
}

case class AddEmployment(
  employerName: String,
  startDate: LocalDate,
  payrollNumber: String,
  telephoneContactAllowed: String,
  telephoneNumber: Option[String]
)

object AddEmployment {

  implicit val addEmploymentFormat: Format[AddEmployment] = Json.format[AddEmployment]
  implicit val writes: Writes[AddEmployment] = Json.writes[AddEmployment]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}

case class EndEmployment(endDate: LocalDate, telephoneContactAllowed: String, telephoneNumber: Option[String])

object EndEmployment {

  implicit val addEmploymentFormat: Format[EndEmployment] = Json.format[EndEmployment]
  implicit val writes: Writes[EndEmployment] = Json.writes[EndEmployment]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)

}

case class IncorrectIncome(whatYouToldUs: String, telephoneContactAllowed: String, telephoneNumber: Option[String])

object IncorrectIncome {
  implicit val formats: Format[IncorrectIncome] = Json.format[IncorrectIncome]
  implicit val writes: Writes[IncorrectIncome] = Json.writes[IncorrectIncome]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}
