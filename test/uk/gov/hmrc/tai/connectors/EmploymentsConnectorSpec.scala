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

package uk.gov.hmrc.tai.connectors

import java.time.LocalDateTime
import java.time.LocalDate
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income.{Ceased, Live}
import utils.BaseSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EmploymentsConnectorSpec extends BaseSpec with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    Mockito.reset(httpHandler)

  "EmploymentsConnector employments" must {
    "return a blank the service url" when {
      "no service url is provided" in {
        sut().serviceUrl mustBe ""
      }
    }

    "return a valid service url" when {
      "a service url is provided" in {
        val url = "test/serviceurl/"
        sut(url).serviceUrl mustBe url
      }
    }

    "return the URL of the employments API" when {
      "a nino is provided" in {
        sut("test/service")
          .employmentServiceUrl(nino, year) mustBe s"test/service/tai/$nino/employments/years/${year.year}"
      }
    }

    "return the URL of the employments API without service URL" when {
      "no serviceUrl is given" in {
        sut().employmentServiceUrl(nino, year) mustBe s"/tai/$nino/employments/years/${year.year}"
      }
    }

    "call the employments API with a URL containing a service URL" when {
      "the service URL is supplied" in {
        when(httpHandler.getFromApiV2(any())(any(), any())).thenReturn(Future.successful(Json.parse(oneEmployment)))

        val responseFuture = sut("test/service").employments(nino, year)

        Await.result(responseFuture, 5 seconds)
        verify(httpHandler).getFromApiV2(meq(s"test/service/tai/$nino/employments/years/${year.year}"))(any(), any())
      }
    }

    "call the employments API with a URL containing a service URL" when {

      "the service URL is not supplied" in {

        when(httpHandler.getFromApiV2(any())(any(), any())).thenReturn(Future.successful(Json.parse(oneEmployment)))

        val responseFuture = sut().employments(nino, year)

        Await.result(responseFuture, 5 seconds)

        verify(httpHandler).getFromApiV2(meq(s"/tai/$nino/employments/years/${year.year}"))(any(), any())
      }
    }

    "return employments from the employments API" when {

      "api provides one employments" in {

        when(httpHandler.getFromApiV2(any())(any(), any())).thenReturn(Future.successful(Json.parse(oneEmployment)))

        val responseFuture = sut().employments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe oneEmploymentDetails

        verify(httpHandler).getFromApiV2(meq(s"/tai/$nino/employments/years/${year.year}"))(any(), any())
      }

      "api provides multiple employments" in {

        when(httpHandler.getFromApiV2(any())(any(), any())).thenReturn(Future.successful(Json.parse(twoEmployments)))

        val responseFuture = sut("test/service").employments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe twoEmploymentsDetails

        verify(httpHandler).getFromApiV2(meq(s"test/service/tai/$nino/employments/years/${year.year}"))(any(), any())
      }
    }

    "return nil when api returns zero employments" in {

      when(httpHandler.getFromApiV2(any())(any(), any())).thenReturn(Future.successful(Json.parse(zeroEmployments)))

      val responseFuture = sut("test/service").employments(nino, year)

      val result = Await.result(responseFuture, 5 seconds)

      result mustBe Nil

      verify(httpHandler).getFromApiV2(meq(s"test/service/tai/$nino/employments/years/${year.year}"))(any(), any())
    }

    "throw an exception" when {
      "invalid json has returned by api" in {

        when(httpHandler.getFromApiV2(any())(any(), any()))
          .thenReturn(Future.successful(Json.parse("""{"test":"test"}""")))

        val ex = the[RuntimeException] thrownBy Await.result(sut("test/service").employments(nino, year), 5 seconds)
        ex.getMessage mustBe "Invalid employment json"
      }
    }

  }

  "EmploymentsConnector ceasedEmployments" must {

    "return employments from the employments API" when {

      "api provides one employments" in {

        when(httpHandler.getFromApiV2(any())(any(), any()))
          .thenReturn(Future.successful(Json.parse(oneCeasedEmployment)))

        val responseFuture = sut("test/service").ceasedEmployments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe oneCeasedEmploymentDetails

        verify(httpHandler).getFromApiV2(meq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"))(
          any(),
          any()
        )
      }

      "api provides multiple employments" in {

        when(httpHandler.getFromApiV2(any())(any(), any()))
          .thenReturn(Future.successful(Json.parse(twoCeasedEmployments)))

        val responseFuture = sut("test/service").ceasedEmployments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe twoCeasedEmploymentsDetails

        verify(httpHandler).getFromApiV2(meq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"))(
          any(),
          any()
        )
      }
    }

    "return nil when api returns zero employments" in {

      when(httpHandler.getFromApiV2(any())(any(), any()))
        .thenReturn(Future.successful(Json.parse(zeroCeasedEmployments)))

      val responseFuture = sut("test/service").ceasedEmployments(nino, year)

      val result = Await.result(responseFuture, 5 seconds)

      result mustBe Nil

      verify(httpHandler)
        .getFromApiV2(meq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"))(any(), any())
    }

    "throw an exception" when {
      "invalid json has returned by api" in {

        when(httpHandler.getFromApiV2(any())(any(), any()))
          .thenReturn(Future.successful(Json.parse("""{"test":"test"}""")))

        val ex = the[RuntimeException] thrownBy Await
          .result(sut("test/service").ceasedEmployments(nino, year), 5 seconds)
        ex.getMessage mustBe "Invalid employment json"
      }
    }
  }

  "EmploymentsConnector employment" must {

    "return service url" in {
      sut("test").employmentUrl(nino, "123") mustBe s"test/tai/$nino/employments/123"
    }

    "return an employment from current year" when {
      "valid id has been passed" in {
        when(httpHandler.getFromApiV2(any())(any(), any())).thenReturn(Future.successful(Json.parse(anEmployment)))

        val result = Await.result(sut().employment(nino, "123"), 5.seconds)

        result mustBe Some(anEmploymentObject)
        verify(httpHandler, times(1)).getFromApiV2(any())(any(), any())
      }
    }

    "return none" when {
      "invalid json returned by an api" in {
        when(httpHandler.getFromApiV2(any())(any(), any())).thenReturn(Future.successful(Json.parse(zeroEmployments)))

        Await.result(sut().employment(nino, "123"), 5.seconds) mustBe None
      }
    }
  }

  "EmploymentsConnector endEmployment" must {
    "return an envelope" when {
      "we send a PUT request to backend" in {
        val json = Json.obj("data" -> JsString("123-456-789"))
        when(httpHandler.putToApi(any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, json, Map[String, Seq[String]]())))

        val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))

        val result = Await.result(sut().endEmployment(nino, 1, endEmploymentData), 5.seconds)

        result mustBe "123-456-789"
      }
    }

    "return an exception" when {
      "json is invalid" in {
        val json = Json.obj("test" -> JsString("123-456-789"))
        when(httpHandler.putToApi(any(), any())(any(), any(), any(), any()))
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
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(
        httpHandler
          .postToApi(meq(sut().addEmploymentServiceUrl(nino)), meq(addEmployment))(any(), any(), any(), any())
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
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(httpHandler.postToApi(meq(s"/tai/$nino/employments/1/reason"), meq(model))(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, json, Map[String, Seq[String]]())))

      val result = Await.result(sut().incorrectEmployment(nino, 1, model), 5.seconds)

      result mustBe Some("123-456-789")
    }
  }

  val anEmploymentObject = Employment(
    "company name",
    Live,
    Some("123"),
    LocalDate.parse("2016-05-26"),
    Some(LocalDate.parse("2016-05-26")),
    Nil,
    "123",
    "321",
    2,
    None,
    false,
    false
  )

  private val oneEmploymentDetails = List(anEmploymentObject)

  private val oneCeasedEmploymentDetails = List(
    Employment(
      "company name",
      Ceased,
      Some("123"),
      LocalDate.parse("2016-05-26"),
      Some(LocalDate.parse("2016-05-26")),
      Nil,
      "123",
      "321",
      2,
      None,
      false,
      false
    )
  )

  private val twoEmploymentsDetails = oneEmploymentDetails.head :: oneEmploymentDetails.head.copy(
    taxDistrictNumber = "1234",
    payeNumber = "4321",
    sequenceNumber = 3,
    receivingOccupationalPension = true
  ) :: Nil

  private val twoCeasedEmploymentsDetails = oneCeasedEmploymentDetails.head :: oneCeasedEmploymentDetails.head.copy(
    taxDistrictNumber = "1234",
    payeNumber = "4321",
    sequenceNumber = 3,
    receivingOccupationalPension = true
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
            "endDate": "2016-05-26",
            "annualAccounts": [],
            "taxDistrictNumber": "123",
            "payeNumber": "321",
            "sequenceNumber": 2,
            "isPrimary": true,
            "hasPayrolledBenefit" : false,
            "receivingOccupationalPension": false
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
            "endDate": "2016-05-26",
            "annualAccounts": [],
            "taxDistrictNumber": "123",
            "payeNumber": "321",
            "sequenceNumber": 2,
            "isPrimary": true,
            "hasPayrolledBenefit" : false,
            "receivingOccupationalPension": false
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
      |            "endDate": "2016-05-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "123",
      |            "payeNumber": "321",
      |            "sequenceNumber": 2,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : false
      |          },
      |          {
      |            "name": "company name",
      |            "employmentStatus" : "Live",
      |            "payrollNumber": "123",
      |            "startDate": "2016-05-26",
      |            "endDate": "2016-05-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "1234",
      |            "payeNumber": "4321",
      |            "sequenceNumber": 3,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : true
      |          }]}
        }""".stripMargin

  private val oneCeasedEmployment =
    """{
          "data" : [{
            "name": "company name",
            "employmentStatus" : "Ceased",
            "payrollNumber": "123",
            "startDate": "2016-05-26",
            "endDate": "2016-05-26",
            "annualAccounts": [],
            "taxDistrictNumber": "123",
            "payeNumber": "321",
            "sequenceNumber": 2,
            "isPrimary": true,
            "hasPayrolledBenefit" : false,
            "receivingOccupationalPension": false
          }]
        }"""

  private val twoCeasedEmployments =
    """{
      |       "data" : [{
      |            "name": "company name",
      |            "employmentStatus" : "Ceased",
      |            "payrollNumber": "123",
      |            "startDate": "2016-05-26",
      |            "endDate": "2016-05-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "123",
      |            "payeNumber": "321",
      |            "sequenceNumber": 2,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : false
      |          },
      |          {
      |            "name": "company name",
      |            "employmentStatus" : "Ceased",
      |            "payrollNumber": "123",
      |            "startDate": "2016-05-26",
      |            "endDate": "2016-05-26",
      |            "annualAccounts": [],
      |            "taxDistrictNumber": "1234",
      |            "payeNumber": "4321",
      |            "sequenceNumber": 3,
      |            "isPrimary": true,
      |            "hasPayrolledBenefit" : false,
      |            "receivingOccupationalPension" : true
      |          }]
        }""".stripMargin

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  val httpHandler: HttpClientResponse = mock[HttpClientResponse]

  def sut(servUrl: String = "") = new EmploymentsConnector(httpHandler, servicesConfig) {
    override val serviceUrl: String = servUrl
  }

}
