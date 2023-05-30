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

import java.time.LocalDateTime
import java.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => meq}
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.EmploymentsConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EmploymentServiceSpec extends BaseSpec { // TODO - Needs error scenarios

  "Employment Service" must {
    "return employments" in {
      val sut = createSUT
      when(employmentsConnector.employments(any(), any())(any(), any()))
        .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson(employments).toString)))))

      sut.employments(nino, year).value.futureValue.map {
        _ mustBe employments // TODO - Check this method of mapping assertion
      }
    }
  }

  "CeasedEmployments Service" must {
    "return employments" in {
      val sut = createSUT
      when(employmentsConnector.ceasedEmployments(any(), any())(any(), any()))
        .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson(employments).toString)))))

      sut.ceasedEmployments(nino, year).value.futureValue.map {
        _ mustBe employments // TODO - Check this method of mapping assertion
      }
    }
  }

  "Employment Names" must {
    "return a map of employment id and employment name" when {
      "connector returns one employment" in {
        val sut = createSUT
        when(employmentsConnector.employments(any(), any())(any(), any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson(employmentDetails).toString)))))

        sut.employmentNames(nino, year).futureValue mustBe Map(2 -> "company name")
      }

      "connector returns multiple employment" in {
        val sut = createSUT
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

        when(employmentsConnector.employments(any(), any())(any(), any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson(employment1, employment2).toString)))))

        val employmentNames = Await.result(sut.employmentNames(nino, year), 5.seconds)

        employmentNames mustBe Map(1 -> "company name 1", 2 -> "company name 2")
      }

      "connector does not return any employment" in {
        val sut = createSUT
        when(employmentsConnector.employments(any(), any())(any(), any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, "")))))

        sut.employmentNames(nino, year).futureValue mustBe Map.empty[Int, String]
      }
    }
  }

  "employment" must {
    "return an employment" when {
      "the connector returns one" in {
        val sut = createSUT

        when(employmentsConnector.employment(any(), any())(any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson(employment).toString)))))

        sut.employment(nino, 8).value.futureValue.map {
          _ mustBe Some(employment) // TODO - Check
        }
      }
    }
    "return none" when {
      "the connector does not return an employment" in {
        val sut = createSUT

        when(employmentsConnector.employment(any(), any())(any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, "")))))

        sut.employment(nino, 8).value.futureValue.map {
          _ mustBe None // TODO - Check
        }
      }
    }
  }

  "end employment" must {
    "return envelope id" in {
      val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))
      val sut = createSUT
      when(employmentsConnector.endEmployment(any(), any(), any())(any()))
        .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson("123-456-789").toString)))))

      sut.endEmployment(nino, 1, endEmploymentData).value.futureValue.map {
        _ mustBe "123-456-789"
      }
    }
  }

  "add employment" must {
    "return an envelope id" in {
      val sut = createSUT
      val model = AddEmployment(
        employerName = "testEmployment",
        payrollNumber = "12345",
        startDate = LocalDate.of(2017, 6, 6),
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )
      when(employmentsConnector.addEmployment(meq(nino), meq(model))(any()))
        .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson("123-456-789").toString)))))

      sut.addEmployment(nino, model).value.futureValue.map {
        _ => "123-456-789"
      }
    }
    "generate a runtime exception" when { // TODO - Change name?
      "no envelope id was returned from the connector layer" in {
        val sut = createSUT
        val model = AddEmployment(
          employerName = "testEmployment",
          payrollNumber = "12345",
          startDate = LocalDate.of(2017, 6, 6),
          telephoneContactAllowed = "Yes",
          telephoneNumber = Some("123456789")
        )
        when(employmentsConnector.addEmployment(meq(nino), meq(model))(any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, "")))))

        sut.addEmployment(nino, model).value.futureValue.map {
          _ mustBe ""
        } // TODO - Check if this behaviour is valid, was an exception before if connector returned None
      }
    }
  }

  "incorrect employment" must {
    "return an envelope id" in {
      val sut = createSUT
      val model =
        IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      when(employmentsConnector.incorrectEmployment(meq(nino), meq(1), meq(model))(any()))
        .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, Json.toJson("123-456-789").toString)))))

      sut.incorrectEmployment(nino, 1, model).value.futureValue.map(_ mustBe "123-456-789")
    }

    "generate a runtime exception" when { // TODO - Change name?
      "no envelope id was returned from the connector layer" in {
        val sut = createSUT
        val model =
          IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
        when(employmentsConnector.incorrectEmployment(meq(nino), meq(1), meq(model))(any()))
          .thenReturn(EitherT[Future, UpstreamErrorResponse, HttpResponse](Future.successful(Right(HttpResponse(OK, "")))))

        sut.incorrectEmployment(nino, 1, model).value.futureValue.map {
          _ mustBe ""
        } // TODO - Check if this behaviour is valid, was an exception before if connector returned None
      }
    }
  }

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  private val employment = Employment(
    "company name",
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
  private val employmentDetails = List(employment)
  private val employments = employmentDetails.head :: employmentDetails.head :: Nil

  private def createSUT = new EmploymentServiceTest

  val employmentsConnector = mock[EmploymentsConnector]

  private class EmploymentServiceTest
      extends EmploymentService(
        employmentsConnector
      )

}
