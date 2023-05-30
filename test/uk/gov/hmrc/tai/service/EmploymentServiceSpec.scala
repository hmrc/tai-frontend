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

package uk.gov.hmrc.tai.service

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.EmploymentsConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}
import utils.BaseSpec

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future
import scala.language.postfixOps

class EmploymentServiceSpec extends BaseSpec { // TODO - Needs error scenarios

  lazy val connector: EmploymentsConnector = mock[EmploymentsConnector]
  lazy val service: EmploymentService = new EmploymentService(connector)

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  "EmploymentService" when {
    val employment1 = Employment(
      "company name 1",
      Live,
      Some("123"),
      LocalDate.parse("2016-05-26"),
      Some(LocalDate.parse("2016-05-26")),
      Nil,
      "",
      "",
      1,
      None,
      false,
      false
    )
    val employment2 = Employment(
      "company name 2",
      Live,
      Some("123"),
      LocalDate.parse("2016-05-26"),
      Some(LocalDate.parse("2016-05-26")),
      Nil,
      "",
      "",
      2,
      None,
      false,
      false
    )
    "employments is called" must {
      "return employments if retrieved with an OK response by the connector" in {
        when(connector.employments(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson(Seq(employment1, employment2)).toString)))
            )
          )
        service.employments(nino, year).value.futureValue.map(_ mustBe Seq(employment1, employment2))
      }
      "return an empty sequence if NO_CONTENT is returned from the connector" in {
        when(connector.employments(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(NO_CONTENT, "[]")))
            )
          )
        service.employments(nino, year).value.futureValue.map(_ mustBe Seq.empty[Employment])
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse containing $errorStatus is retrieved from the connector" in {
          when(connector.employments(any(), any())(any(), any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, HttpResponse](
                Future.successful(Left(UpstreamErrorResponse("", errorStatus)))
              )
            )
          service.employments(nino, year).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "ceasedEmployments is called" must {
      "return employments if retrieved with an OK response by the connector" in {
        when(connector.ceasedEmployments(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson(Seq(employment1, employment2)).toString)))
            )
          )
        service.ceasedEmployments(nino, year).value.futureValue.map(_ mustBe Seq(employment1, employment2))
      }
      "return an empty sequence if NO_CONTENT is returned from the connector" in {
        when(connector.ceasedEmployments(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(NO_CONTENT, "[]")))
            )
          )
        service.ceasedEmployments(nino, year).value.futureValue.map(_ mustBe Seq.empty[Employment])
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse containing $errorStatus is retrieved from the connector" in {
          when(connector.ceasedEmployments(any(), any())(any(), any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, HttpResponse](
                Future.successful(Left(UpstreamErrorResponse("", errorStatus)))
              )
            )
          service.ceasedEmployments(nino, year).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "employment is called" must {
      "return employments if retrieved with an OK response by the connector" in {
        when(connector.employment(any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson(employment1).toString)))
            )
          )
        service.employment(nino, 8).value.futureValue.map(_ mustBe Some(employment1))
      }
      "return an empty sequence if NO_CONTENT is returned from the connector" in {
        when(connector.employment(any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(NO_CONTENT, Json.toJson("test" -> "").toString)))
            )
          )
        service.employment(nino, 8).value.futureValue.map(_ mustBe None)
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse containing $errorStatus is retrieved from the connector" in {
          when(connector.employment(any(), any())(any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, HttpResponse](
                Future.successful(Left(UpstreamErrorResponse("", errorStatus)))
              )
            )
          service.employment(nino, 8).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "endEmployment is called" must {
      val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))
      "return String data if retrieved with an OK response by the connector" in {
        when(connector.endEmployment(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson("123-456-789").toString)))
            )
          )
        service.endEmployment(nino, 8, endEmploymentData).value.futureValue.map(_ mustBe "123-456-789")
      }
      "return an empty sequence if NO_CONTENT is returned from the connector" in {
        when(connector.endEmployment(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(NO_CONTENT, "[]")))
            )
          )
        service.endEmployment(nino, 8, endEmploymentData).value.futureValue.swap.map(_.statusCode mustBe BAD_GATEWAY)
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse containing $errorStatus is retrieved from the connector" in {
          when(connector.endEmployment(any(), any(), any())(any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, HttpResponse](
                Future.successful(Left(UpstreamErrorResponse("", errorStatus)))
              )
            )
          service.endEmployment(nino, 8, endEmploymentData).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "addEmployment is called" must {
      val addEmployment = AddEmployment(
        employerName = "testEmployment",
        payrollNumber = "12345",
        startDate = LocalDate.of(2017, 6, 6),
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )
      "return String data if retrieved with an OK response by the connector" in {
        when(connector.addEmployment(any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson("123-456-789").toString)))
            )
          )
        service.addEmployment(nino, addEmployment).value.futureValue.map(_ mustBe "123-456-789")
      }
      "return an empty sequence if NO_CONTENT is returned from the connector" in {
        when(connector.addEmployment(any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(NO_CONTENT, "[]")))
            )
          )
        service.addEmployment(nino, addEmployment).value.futureValue.swap.map(_.statusCode mustBe BAD_GATEWAY)
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse containing $errorStatus is retrieved from the connector" in {
          when(connector.addEmployment(any(), any())(any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, HttpResponse](
                Future.successful(Left(UpstreamErrorResponse("", errorStatus)))
              )
            )
          service.addEmployment(nino, addEmployment).value.futureValue.swap.map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "incorrectEmployment is called" must {
      val incorrectIncome =
        IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      "return String data if retrieved with an OK response by the connector" in {
        when(connector.incorrectEmployment(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson("123-456-789").toString)))
            )
          )
        service.incorrectEmployment(nino, 1, incorrectIncome).value.futureValue.map(_ mustBe "123-456-789")
      }
      "return an empty sequence if NO_CONTENT is returned from the connector" in {
        when(connector.incorrectEmployment(any(), any(), any())(any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(NO_CONTENT, "[]")))
            )
          )
        service
          .incorrectEmployment(nino, 1, incorrectIncome)
          .value
          .futureValue
          .swap
          .map(_.statusCode mustBe BAD_GATEWAY)
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse containing $errorStatus is retrieved from the connector" in {
          when(connector.incorrectEmployment(any(), any(), any())(any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, HttpResponse](
                Future.successful(Left(UpstreamErrorResponse("", errorStatus)))
              )
            )
          service
            .incorrectEmployment(nino, 1, incorrectIncome)
            .value
            .futureValue
            .swap
            .map(_.statusCode mustBe errorStatus)
        }
      }
    }
    "employmentNames is called" must {
      "return String data if retrieved with an OK response by the connector" in {
        when(connector.employments(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(OK, Json.toJson(List(employment1, employment2)).toString)))
            )
          )
        val result = service.employmentNames(nino, year).futureValue
        result mustBe Map(1 -> "company name 1", 2 -> "company name 2")
      }
      "return an empty sequence if NO_CONTENT is returned from the connector" in {
        when(connector.employments(any(), any())(any(), any()))
          .thenReturn(
            EitherT[Future, UpstreamErrorResponse, HttpResponse](
              Future.successful(Right(HttpResponse(NO_CONTENT, "[]")))
            )
          )
        val result = service.employmentNames(nino, year).futureValue
        result mustBe Map.empty[Int, String]
      }
      List(
        NOT_FOUND,
        BAD_REQUEST,
        IM_A_TEAPOT,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).foreach { errorStatus =>
        s"return an UpstreamErrorResponse containing $errorStatus is retrieved from the connector" in {
          when(connector.employments(any(), any())(any(), any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, HttpResponse](
                Future.successful(Left(UpstreamErrorResponse("", errorStatus)))
              )
            )
          service.employmentNames(nino, year).futureValue.map(_ mustBe Map.empty[Int, String])
        }
      }
    }
  }
}
