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

import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.{reset, verify, when}
import play.api.libs.json.{JsResultException, JsString, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.{Ceased, Live}
import utils.BaseSpec

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EmploymentsConnectorSpec extends BaseSpec {

  private val anEmploymentObject: Employment = Employment(
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

  private val twoEmploymentsDetails =
    oneEmploymentDetails.head ::
      oneEmploymentDetails.head.copy(
        taxDistrictNumber = "1234",
        payeNumber = "4321",
        sequenceNumber = 3,
        receivingOccupationalPension = true,
        employmentType = PensionIncome
      ) ::
      Nil

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

  private val twoCeasedEmploymentsDetails =
    oneCeasedEmploymentDetails.head ::
      oneCeasedEmploymentDetails.head.copy(
        taxDistrictNumber = "1234",
        payeNumber = "4321",
        sequenceNumber = 3,
        receivingOccupationalPension = true,
        employmentType = PensionIncome
      ) ::
      Nil

  private val zeroEmploymentsJson =
    """{
      |  "data": { "employments": [] }
      |}""".stripMargin

  private val zeroCeasedEmploymentsJson =
    """{
      |  "data": []
      |}""".stripMargin

  private val anEmploymentJson =
    """{
      |  "data" : {
      |    "name": "company name",
      |    "employmentStatus" : "Live",
      |    "payrollNumber": "123",
      |    "startDate": "2016-05-26",
      |    "endDate": "2016-06-26",
      |    "annualAccounts": [],
      |    "taxDistrictNumber": "123",
      |    "payeNumber": "321",
      |    "sequenceNumber": 2,
      |    "isPrimary": true,
      |    "hasPayrolledBenefit" : false,
      |    "receivingOccupationalPension": false,
      |    "employmentType": "EmploymentIncome"
      |  }
      |}""".stripMargin

  private val oneEmploymentJson =
    """{
      |  "data" : {
      |    "employments": [
      |      {
      |        "name": "company name",
      |        "employmentStatus" : "Live",
      |        "payrollNumber": "123",
      |        "startDate": "2016-05-26",
      |        "endDate": "2016-06-26",
      |        "annualAccounts": [],
      |        "taxDistrictNumber": "123",
      |        "payeNumber": "321",
      |        "sequenceNumber": 2,
      |        "isPrimary": true,
      |        "hasPayrolledBenefit" : false,
      |        "receivingOccupationalPension": false,
      |        "employmentType": "EmploymentIncome"
      |      }
      |    ]
      |  }
      |}""".stripMargin

  private val twoEmploymentsJson =
    """{
      |  "data" : {
      |    "employments": [
      |      {
      |        "name": "company name",
      |        "employmentStatus" : "Live",
      |        "payrollNumber": "123",
      |        "startDate": "2016-05-26",
      |        "endDate": "2016-06-26",
      |        "annualAccounts": [],
      |        "taxDistrictNumber": "123",
      |        "payeNumber": "321",
      |        "sequenceNumber": 2,
      |        "isPrimary": true,
      |        "hasPayrolledBenefit" : false,
      |        "receivingOccupationalPension" : false,
      |        "employmentType": "EmploymentIncome"
      |      },
      |      {
      |        "name": "company name",
      |        "employmentStatus" : "Live",
      |        "payrollNumber": "123",
      |        "startDate": "2016-05-26",
      |        "endDate": "2016-06-26",
      |        "annualAccounts": [],
      |        "taxDistrictNumber": "1234",
      |        "payeNumber": "4321",
      |        "sequenceNumber": 3,
      |        "isPrimary": true,
      |        "hasPayrolledBenefit" : false,
      |        "receivingOccupationalPension" : true,
      |        "employmentType": "PensionIncome"
      |      }
      |    ]
      |  }
      |}""".stripMargin

  private val oneCeasedEmploymentJson =
    """{
      |  "data" : [{
      |    "name": "company name",
      |    "employmentStatus" : "Ceased",
      |    "payrollNumber": "123",
      |    "startDate": "2016-05-26",
      |    "endDate": "2016-06-26",
      |    "annualAccounts": [],
      |    "taxDistrictNumber": "123",
      |    "payeNumber": "321",
      |    "sequenceNumber": 2,
      |    "isPrimary": true,
      |    "hasPayrolledBenefit" : false,
      |    "receivingOccupationalPension": false,
      |    "employmentType": "EmploymentIncome"
      |  }]
      |}""".stripMargin

  private val twoCeasedEmploymentsJson =
    """{
      |  "data" : [{
      |    "name": "company name",
      |    "employmentStatus" : "Ceased",
      |    "payrollNumber": "123",
      |    "startDate": "2016-05-26",
      |    "endDate": "2016-06-26",
      |    "annualAccounts": [],
      |    "taxDistrictNumber": "123",
      |    "payeNumber": "321",
      |    "sequenceNumber": 2,
      |    "isPrimary": true,
      |    "hasPayrolledBenefit" : false,
      |    "receivingOccupationalPension" : false,
      |    "employmentType": "EmploymentIncome"
      |  },{
      |    "name": "company name",
      |    "employmentStatus" : "Ceased",
      |    "payrollNumber": "123",
      |    "startDate": "2016-05-26",
      |    "endDate": "2016-06-26",
      |    "annualAccounts": [],
      |    "taxDistrictNumber": "1234",
      |    "payeNumber": "4321",
      |    "sequenceNumber": 3,
      |    "isPrimary": true,
      |    "hasPayrolledBenefit" : false,
      |    "receivingOccupationalPension" : true,
      |    "employmentType": "PensionIncome"
      |  }]
      |}""".stripMargin

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  private val httpHandler: HttpHandler = mock[HttpHandler]

  private def sut(servUrl: String = ""): EmploymentsConnector = new EmploymentsConnector(httpHandler, appConfig) {
    override val serviceUrl: String = servUrl
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(httpHandler)
  }

  "EmploymentsConnector employments" must {

    "expose the configured serviceUrl (overridden in test)" in {
      sut("test/service").serviceUrl mustBe "test/service"
      sut().serviceUrl mustBe ""
    }

    "build the employments URL" in {
      sut("svc").employmentServiceUrl(nino, year) mustBe s"svc/tai/$nino/employments/years/${year.year}"
      sut().employmentServiceUrl(nino, year) mustBe s"/tai/$nino/employments/years/${year.year}"
    }

    "GET employments with/without serviceUrl and return parsed list" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(oneEmploymentJson)))

      Await.result(sut().employments(nino, year), 5.seconds)
      verify(httpHandler).getFromApiV2(
        meq(s"/tai/$nino/employments/years/${year.year}"),
        any[Option[DurationInt]]
      )(any(), any())

      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(twoEmploymentsJson)))

      val result = Await.result(sut("svc").employments(nino, year), 5.seconds)
      result mustBe twoEmploymentsDetails
      verify(httpHandler).getFromApiV2(
        meq(s"/tai/$nino/employments/years/${year.year}"),
        any[Option[DurationInt]]
      )(any(), any())
    }

    "filter startDate older than the configured cutoff" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(oneEmploymentJson.replace("2016-05-26", "1945-05-26"))))

      val result = Await.result(sut().employments(nino, year), 5.seconds)
      result mustBe oneEmploymentDetails.map(_.copy(startDate = None))
    }

    "return Nil when API returns an empty list" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(zeroEmploymentsJson)))

      val result = Await.result(sut("svc").employments(nino, year), 5.seconds)
      result mustBe Nil
    }

    "fail with JsResultException on invalid JSON" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse("""{"bogus":"value"}""")))

      val f = sut("svc").employments(nino, year)
      whenReady(f.failed)(e => e mustBe a[JsResultException])
    }
  }

  "EmploymentsConnector ceasedEmployments" must {

    "parse one/multiple ceased employments" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(oneCeasedEmploymentJson)))

      val r1 = Await.result(sut("svc").ceasedEmployments(nino, year), 5.seconds)
      r1 mustBe oneCeasedEmploymentDetails

      verify(httpHandler).getFromApiV2(
        meq(s"svc/tai/$nino/employments/year/${year.year}/status/ceased"),
        any[Option[DurationInt]]
      )(any(), any())

      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(twoCeasedEmploymentsJson)))

      val r2 = Await.result(sut("svc").ceasedEmployments(nino, year), 5.seconds)
      r2 mustBe twoCeasedEmploymentsDetails
    }

    "filter startDate older than the cutoff" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(oneCeasedEmploymentJson.replace("2016-05-26", "1950-01-01"))))

      val r = Await.result(sut("svc").ceasedEmployments(nino, year), 5.seconds)
      r mustBe oneCeasedEmploymentDetails.map(_.copy(startDate = None))
    }

    "return Nil on empty payload" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(zeroCeasedEmploymentsJson)))

      val r = Await.result(sut("svc").ceasedEmployments(nino, year), 5.seconds)
      r mustBe Nil
    }

    "fail with JsResultException on invalid JSON" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse("""{"nope":true}""")))

      val f = sut("svc").ceasedEmployments(nino, year)
      whenReady(f.failed)(e => e mustBe a[JsResultException])
    }
  }

  "EmploymentsConnector employment" must {

    "build employment URL" in {
      sut("test").employmentUrl(nino, "123") mustBe s"test/tai/$nino/employments/123"
    }

    "return an employment by id and filter old startDate" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(anEmploymentJson)))

      val r1 = Await.result(sut().employment(nino, "123"), 5.seconds)
      r1 mustBe Some(anEmploymentObject)

      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(anEmploymentJson.replace("2016-05-26", "1945-05-26"))))

      val r2 = Await.result(sut().employment(nino, "123"), 5.seconds)
      r2 mustBe Some(anEmploymentObject.copy(startDate = None))
    }

    "return None on empty/shape mismatch" in {
      when(httpHandler.getFromApiV2(any[String], any[Option[DurationInt]])(any(), any()))
        .thenReturn(Future.successful(Json.parse(zeroEmploymentsJson)))

      Await.result(sut().employment(nino, "123"), 5.seconds) mustBe None
    }
  }

  "EmploymentsConnector endEmployment" must {

    "return envelope id when backend returns data string" in {
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(httpHandler.putToApi(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, json, Map.empty)))

      val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))
      val r                 = Await.result(sut().endEmployment(nino, 1, endEmploymentData), 5.seconds)

      r mustBe "123-456-789"
    }

    "fail with RuntimeException('Invalid json') when data missing" in {
      val json = Json.obj("test" -> JsString("123-456-789"))
      when(httpHandler.putToApi(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, json, Map.empty)))

      val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))

      val thrown = the[RuntimeException] thrownBy Await.result(
        sut().endEmployment(nino, 1, endEmploymentData),
        5.seconds
      )
      thrown.getMessage mustBe "Invalid json"
    }
  }

  "EmploymentsConnector addEmployment" must {
    "return envelope id on success" in {
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
        .thenReturn(Future.successful(HttpResponse(200, json, Map.empty)))

      val r = Await.result(sut().addEmployment(nino, addEmployment), 5.seconds)
      r mustBe Some("123-456-789")
    }
  }

  "EmploymentsConnector incorrectEmployment" must {
    "return envelope id on success" in {
      val model = IncorrectIncome("TEST", "Yes", Some("123456789"))
      val json  = Json.obj("data" -> JsString("123-456-789"))

      when(httpHandler.postToApi(meq(s"/tai/$nino/employments/1/reason"), meq(model), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, json, Map.empty)))

      val r = Await.result(sut().incorrectEmployment(nino, 1, model), 5.seconds)
      r mustBe Some("123-456-789")
    }
  }
}
