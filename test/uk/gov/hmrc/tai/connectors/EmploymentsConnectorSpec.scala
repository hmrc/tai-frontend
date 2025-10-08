/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.data.EitherT
import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsResultException, JsString, Json}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.{Ceased, Live}
import utils.{BaseSpec, WireMockHelper}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

class EmploymentsConnectorSpec extends BaseSpec with WireMockHelper {

  val anEmploymentObject: Employment = Employment(
    "company name",
    Live,
    Some("123"),
    Some(LocalDate.parse("2016-05-26")),
    Some(LocalDate.parse("2016-06-26")),
    Nil,
    "123",
    "321",
    2,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  private val oneEmploymentDetails = List(anEmploymentObject)

  private val oneCeasedEmploymentDetails = List(
    Employment(
      "company name",
      Ceased,
      Some("123"),
      Some(LocalDate.parse("2016-05-26")),
      Some(LocalDate.parse("2016-06-26")),
      Nil,
      "123",
      "321",
      2,
      None,
      hasPayrolledBenefit = false,
      receivingOccupationalPension = false,
      EmploymentIncome
    )
  )

  private val twoCeasedEmploymentsDetails = oneCeasedEmploymentDetails.head :: oneCeasedEmploymentDetails.head.copy(
    taxDistrictNumber = "1234",
    payeNumber = "4321",
    sequenceNumber = 3,
    receivingOccupationalPension = true,
    employmentType = PensionIncome
  ) :: Nil

  private val zeroEmployments =
    """|{
       |   "data":{
       |      "employments":[
       |
       |      ]
       |   }
       |}""".stripMargin

  private val zeroCeasedEmployments =
    """|{
       |   "data":[
       |
       |      ]
       |
       |}""".stripMargin

  private val anEmployment =
    """{
          "data" : {
            "name": "company name",
            "employmentStatus" : "Live",
            "payrollNumber": "123",
            "startDate": "2016-05-26",
            "endDate": "2016-06-26",
            "annualAccounts": [],
            "taxDistrictNumber": "123",
            "payeNumber": "321",
            "sequenceNumber": 2,
            "isPrimary": true,
            "hasPayrolledBenefit" : false,
            "receivingOccupationalPension": false,
            "employmentType": "EmploymentIncome"
          }
        }"""

  private val oneEmployment =
    """{
          "data" : {
            "employments": [
          {
            "name": "company name",
            "employmentStatus" : "Live",
            "payrollNumber": "123",
            "startDate": "2016-05-26",
            "endDate": "2016-06-26",
            "annualAccounts": [],
            "taxDistrictNumber": "123",
            "payeNumber": "321",
            "sequenceNumber": 2,
            "isPrimary": true,
            "hasPayrolledBenefit" : false,
            "receivingOccupationalPension": false,
            "employmentType": "EmploymentIncome"
          }]}
        }"""

  private val twoEmployments =
    """{
      |       "data" : {
      |           "employments": [
      |         {
      |            "name": "company name",
      |            "employmentStatus" : "Live",
      |            "payrollNumber": "123",
      |            "startDate": "2016-05-26",
      |            "endDate": "2016-06-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "123",
      |            "payeNumber": "321",
      |            "sequenceNumber": 2,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : false,
      |            "employmentType": "EmploymentIncome"
      |          },
      |          {
      |            "name": "company name",
      |            "employmentStatus" : "Live",
      |            "payrollNumber": "123",
      |            "startDate": "2016-05-26",
      |            "endDate": "2016-06-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "1234",
      |            "payeNumber": "4321",
      |            "sequenceNumber": 3,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : true,
      |            "employmentType": "PensionIncome"
      |          }]}
        }""".stripMargin

  private val oneCeasedEmployment =
    """{
          "data" : [{
            "name": "company name",
            "employmentStatus" : "Ceased",
            "payrollNumber": "123",
            "startDate": "2016-05-26",
            "endDate": "2016-06-26",
            "annualAccounts": [],
            "taxDistrictNumber": "123",
            "payeNumber": "321",
            "sequenceNumber": 2,
            "isPrimary": true,
            "hasPayrolledBenefit" : false,
            "receivingOccupationalPension": false,
            "employmentType": "EmploymentIncome"
          }]
        }"""

  private val twoCeasedEmployments =
    """{
      |       "data" : [{
      |            "name": "company name",
      |            "employmentStatus" : "Ceased",
      |            "payrollNumber": "123",
      |            "startDate": "2016-05-26",
      |            "endDate": "2016-06-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "123",
      |            "payeNumber": "321",
      |            "sequenceNumber": 2,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : false,
      |            "employmentType": "EmploymentIncome"
      |          },
      |          {
      |            "name": "company name",
      |            "employmentStatus" : "Ceased",
      |            "payrollNumber": "123",
      |            "startDate": "2016-05-26",
      |            "endDate": "2016-06-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "1234",
      |            "payeNumber": "4321",
      |            "sequenceNumber": 3,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : true,
      |            "employmentType": "PensionIncome"
      |          }]
        }""".stripMargin

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  val httpHandler: HttpHandler       = mock[HttpHandler]
  val httpClientV2: HttpClientV2     = inject[HttpClientV2]
  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.tai.port" -> server.port())
    .build()

  def sut(servUrl: String = ""): EmploymentsConnector =
    new EmploymentsConnector(httpHandler, appConfig) {
      override val serviceUrl: String = servUrl
    }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpHandler)
    when(httpHandler.httpClient).thenReturn(httpClientV2)
    when(httpHandler.read(any[Future[Either[UpstreamErrorResponse, HttpResponse]]]())(any[ExecutionContext]))
      .thenAnswer((invocation: InvocationOnMock) => EitherT(invocation.getArgument(0)))
  }

  "EmploymentsConnector ceasedEmployments" must {
    "return employments from the employments API" when {
      "api provides one employments" in {
        when(httpHandler.getFromApiV2(any(), any())(any(), any()))
          .thenReturn(Future.successful(Json.parse(oneCeasedEmployment)))

        val responseFuture = sut("test/service").ceasedEmployments(nino, year)

        val result = Await.result(responseFuture, 5.seconds)

        result mustBe oneCeasedEmploymentDetails

        verify(httpHandler)
          .getFromApiV2(meq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"), any())(any(), any())
      }

      "api provides multiple employments" in {
        when(httpHandler.getFromApiV2(any(), any())(any(), any()))
          .thenReturn(Future.successful(Json.parse(twoCeasedEmployments)))

        val responseFuture = sut("test/service").ceasedEmployments(nino, year)

        val result = Await.result(responseFuture, 5.seconds)

        result mustBe twoCeasedEmploymentsDetails

        verify(httpHandler)
          .getFromApiV2(meq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"), any())(any(), any())
      }
    }

    "startDate older than 1950 are filtered out" in {
      when(httpHandler.getFromApiV2(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.parse(oneCeasedEmployment.replace("2016-05-26", "1950-01-01"))))

      val responseFuture = sut("test/service").ceasedEmployments(nino, year)

      val result = Await.result(responseFuture, 5.seconds)

      result mustBe oneCeasedEmploymentDetails.map(_.copy(startDate = None))

      verify(httpHandler)
        .getFromApiV2(meq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"), any())(any(), any())
    }

    "return nil when api returns zero employments" in {
      when(httpHandler.getFromApiV2(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.parse(zeroCeasedEmployments)))

      val responseFuture = sut("test/service").ceasedEmployments(nino, year)

      val result = Await.result(responseFuture, 5.seconds)

      result mustBe Nil

      verify(httpHandler)
        .getFromApiV2(meq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"), any())(any(), any())
    }

    "throw an exception" when {
      "invalid json has returned by api" in {
        when(httpHandler.getFromApiV2(any(), any())(any(), any()))
          .thenReturn(Future.successful(Json.parse("""{"test":"test"}""")))

        val result = sut("test/service").ceasedEmployments(nino, year)
        whenReady(result.failed) { e =>
          e mustBe a[JsResultException]
        }
      }
    }
  }

  "EmploymentsConnector endEmployment" must {
    "return an envelope" when {
      "we send a PUT request to backend" in {
        val json = Json.obj("data" -> JsString("123-456-789"))
        when(httpHandler.putToApi(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, json, Map[String, Seq[String]]())))

        val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))

        val result = Await.result(sut().endEmployment(nino, 1, endEmploymentData), 5.seconds)

        result mustBe "123-456-789"
      }
    }

    "return an exception" when {
      "json is invalid" in {
        val json              = Json.obj("test" -> JsString("123-456-789"))
        when(httpHandler.putToApi(any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, json, Map[String, Seq[String]]())))
        val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))

        val ex = the[RuntimeException] thrownBy Await.result(sut().endEmployment(nino, 1, endEmploymentData), 5.seconds)

        ex.getMessage mustBe "Invalid json"
      }
    }
  }

  "EmploymentsConnector addEmployment" must {
    "return an envelope id on a successful invocation" in {
      val addEmployment = AddEmployment(
        employerName = "testEmployment",
        payrollNumber = "12345",
        startDate = LocalDate.of(2017, 6, 6),
        payeRef = "123/AB456",
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )
      val json          = Json.obj("data" -> JsString("123-456-789"))
      when(
        httpHandler.postToApi(meq(sut().addEmploymentServiceUrl(nino)), meq(addEmployment), any())(any(), any(), any())
      )
        .thenReturn(Future.successful(HttpResponse(200, json, Map[String, Seq[String]]())))

      val result = Await.result(sut().addEmployment(nino, addEmployment), 5.seconds)

      result mustBe Some("123-456-789")
    }
  }

  "EmploymentsConnector incorrectEmployment" must {
    "return an envelope id on a successful invocation" in {
      val model =
        IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      val json  = Json.obj("data" -> JsString("123-456-789"))
      when(httpHandler.postToApi(meq(s"/tai/$nino/employments/1/reason"), meq(model), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, json, Map[String, Seq[String]]())))

      val result = Await.result(sut().incorrectEmployment(nino, 1, model), 5.seconds)

      result mustBe Some("123-456-789")
    }
  }

  "EmploymentsConnector employmentOnly" must {

    "return an employment from current year" when {
      "valid id has been passed" in {
        when(httpHandler.getFromApiV2(any(), any())(any(), any()))
          .thenReturn(Future.successful(Json.parse(anEmployment)))

        val result = Await.result(sut().employmentOnly(nino, 123, year), 5.seconds)

        result mustBe Some(anEmploymentObject)
        verify(httpHandler, times(1)).getFromApiV2(any(), any())(any(), any())
      }

      "startDate older than 1950 are filtered out" in {
        when(httpHandler.getFromApiV2(any(), any())(any(), any()))
          .thenReturn(
            Future.successful(Json.parse(anEmployment.replace("2016-05-26", "1945-05-26")))
          )

        val result = Await.result(sut().employmentOnly(nino, 123, year), 5.seconds)

        result mustBe Some(anEmploymentObject.copy(startDate = None))
        verify(httpHandler, times(1)).getFromApiV2(any(), any())(any(), any())
      }
    }

    "return none" when {
      "invalid json returned by api" in {
        when(httpHandler.getFromApiV2(any(), any())(any(), any()))
          .thenReturn(Future.successful(Json.parse(zeroEmployments)))

        Await.result(sut().employmentOnly(nino, 123, year), 5.seconds) mustBe None
      }
    }
  }

  "EmploymentsConnector employmentsOnly" must {

    "return employments from the employments-only API" when {
      "api provides one employment" in {
        server.stubFor(
          get(s"/tai/$nino/employments-only/years/${year.year}")
            .willReturn(aResponse().withStatus(OK).withBody(oneEmployment))
        )

        val result = Await.result(sut(appConfig.taiServiceUrl).employmentsOnly(nino, year).value, 5.seconds)

        result mustBe Right(oneEmploymentDetails)
      }
    }

    "return multiple employments" when {
      "api provides more than one employment" in {
        server.stubFor(
          get(s"/tai/$nino/employments-only/years/${year.year}")
            .willReturn(aResponse().withStatus(OK).withBody(twoEmployments))
        )

        val result = Await.result(sut(appConfig.taiServiceUrl).employmentsOnly(nino, year).value, 5.seconds)
        result.isRight mustBe true
        result.toOption.get.size mustBe 2
      }
    }

    "filter out employments" when {
      "startDate older than 1950" in {
        val outdatedEmployment = oneEmployment.replace("2016-05-26", "1945-05-26")
        server.stubFor(
          get(s"/tai/$nino/employments-only/years/${year.year}")
            .willReturn(aResponse().withStatus(OK).withBody(outdatedEmployment))
        )

        val result = Await.result(sut(appConfig.taiServiceUrl).employmentsOnly(nino, year).value, 5.seconds)
        result.isRight mustBe true
        result.toOption.get.head.startDate mustBe None
      }
    }

    "return empty list" when {
      "api returns zero employments" in {
        server.stubFor(
          get(s"/tai/$nino/employments-only/years/${year.year}")
            .willReturn(aResponse().withStatus(OK).withBody(zeroEmployments))
        )

        val result = Await.result(sut(appConfig.taiServiceUrl).employmentsOnly(nino, year).value, 5.seconds)
        result mustBe Right(Nil)
      }
    }

    "return Left(error) when API call fails" in {
      server.stubFor(
        get(s"/tai/$nino/employments-only/years/${year.year}")
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("internal server error"))
      )

      val result = Await.result(sut(appConfig.taiServiceUrl).employmentsOnly(nino, year).value, 5.seconds)
      result.isLeft mustBe true
    }
  }
}
