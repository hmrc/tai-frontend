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

package uk.gov.hmrc.tai.connectors

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CoreDelete, CoreGet, CorePost, CorePut, _}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.tai.AnnualAccount

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TaiConnector extends RawResponseReads{
  def http: CoreGet with CorePost with CorePut with CoreDelete

  def serviceUrl: String

  def url(path: String) = s"$serviceUrl$path"

  def responseTo[T](uri: String)(response: HttpResponse)(implicit rds: Reads[T]) = response.json.as[T]

  val STATUS_OK = 200
  val STATUS_EMAIL_RESPONSE = 201

  def taxSummary(nino : Nino, year : Int)(implicit hc: HeaderCarrier): Future[TaxSummaryDetails] = {
    http.GET[HttpResponse](url = url(s"/tai/$nino/tax-summary-full/$year")) map {
      response =>
        response.status match {
          case OK => {
            response.json.as[TaxSummaryDetails]
          }
          case NOT_FOUND => {
            Logger.warn(s"TaxForCitizens:Frontend -  No taxSummary Data can be found")
            throw new NotFoundException(Json.stringify(response.json))
          }
          case BAD_REQUEST => {
            Logger.warn(s"TaxForCitizens:Frontend -  Bad Request")
            throw new BadRequestException(Json.stringify(response.json))
          }
          case SERVICE_UNAVAILABLE => {
            Logger.warn(s"TaxForCitizens:Frontend -  Service Unavailable")
            throw new ServiceUnavailableException(Json.stringify(response.json))
          }

          case INTERNAL_SERVER_ERROR => {
            Logger.warn(s"TaxForCitizens:Frontend -  Internal System Error")
            throw new InternalServerException(Json.stringify(response.json))
          }
          case _ => {
            Logger.warn(s"TaxForCitizens:Frontend -  Unsuccessful return of data for Unknown Reason")
            throw new HttpException(Json.stringify(response.json), response.status)
          }
        }
    }
  }

  def rtiData(nino : Nino, year : Int)(implicit hc: HeaderCarrier): Future[AnnualAccount] = {
    http.GET[AnnualAccount](url = url(s"/tai/$nino/rti-data/$year"))
  }

  def root(rootUri: String)(implicit hc: HeaderCarrier): Future[TaiRoot] = {
    http.GET[TaiRoot](url = url(rootUri.replace("paye","tai")))
  }

  def updateEmployments(nino: Nino, year: Int, editIadb :IabdUpdateEmploymentsRequest )
                       (implicit hc: HeaderCarrier): Future[IabdUpdateEmploymentsResponse] = {

    val postUrl = url(s"/tai/$nino/incomes/$year/update")
    http.POST(postUrl, editIadb).map(responseTo[IabdUpdateEmploymentsResponse](postUrl))
  }

  def calculateEstimatedPay(payDetails : PayDetails)(implicit hc: HeaderCarrier): Future[CalculatedPay] = {
    val postUrl = url(s"/tai/calculator/calculate-estimated-pay")
    http.POST(postUrl, payDetails).map(responseTo[CalculatedPay](postUrl))
  }
}

object TaiConnector extends TaiConnector with ServicesConfig {


  lazy val serviceUrl = baseUrl("tai")
  override def http = WSHttp

}
