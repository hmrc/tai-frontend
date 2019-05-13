/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.FakeTaiPlayApplication
import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tai.config.DefaultServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random

class EmploymentsConnectorSpec extends PlaySpec
  with MockitoSugar
  with DefaultServicesConfig
  with FakeTaiPlayApplication
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(httpHandler)
  }

  "EmploymentsConnector employments" must {
    "return a blank the service url" when {
      "no service url is provided" in {
        val SUT = createSUT()
        SUT.serviceUrl mustBe ""
      }
    }

    "return a valid service url" when {
      "a service url is provided" in {
        val SUT = createSUT("test/serviceurl/")
        SUT.serviceUrl mustBe "test/serviceurl/"
      }
    }

    "return the URL of the employments API" when {
      "a nino is provided" in {
        val SUT = createSUT("test/service")
        SUT.employmentServiceUrl(nino, year) mustBe s"test/service/tai/$nino/employments/years/${year.year}"
      }
    }

    "return the URL of the employments API without service URL" when {
      "no serviceUrl is given" in {
        val SUT = createSUT()
        SUT.employmentServiceUrl(nino, year) mustBe s"/tai/$nino/employments/years/${year.year}"
      }
    }

    "call the employments API with a URL containing a service URL" when {
      "the service URL is supplied" in {
        val SUT = createSUT("test/service")
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(oneEmployment)))

        val responseFuture = SUT.employments(nino, year)

        Await.result(responseFuture, 5 seconds)
        verify(httpHandler).getFromApi(Matchers.eq(s"test/service/tai/$nino/employments/years/${year.year}"))(any())
      }
    }

    "call the employments API with a URL containing a service URL" when {

      "the service URL is not supplied" in {

        val SUT = createSUT()

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(oneEmployment)))

        val responseFuture = SUT.employments(nino, year)

        Await.result(responseFuture, 5 seconds)

        verify(httpHandler).getFromApi(Matchers.eq(s"/tai/$nino/employments/years/${year.year}"))(any())
      }
    }

    "return employments from the employments API" when {

      "api provides one employments" in {

        val SUT = createSUT("test/service")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(oneEmployment)))

        val responseFuture = SUT.employments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe oneEmploymentDetails

        verify(httpHandler).getFromApi(Matchers.eq(s"test/service/tai/$nino/employments/years/${year.year}"))(any())
      }

      "api provides multiple employments" in {

        val SUT = createSUT("test/service")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(twoEmployments)))

        val responseFuture = SUT.employments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe twoEmploymentsDetails

        verify(httpHandler).getFromApi(Matchers.eq(s"test/service/tai/$nino/employments/years/${year.year}"))(any())
      }
    }

    "return nil when api returns zero employments" in {
      val SUT = createSUT("test/service")

      when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(zeroEmployments)))

      val responseFuture = SUT.employments(nino, year)

      val result = Await.result(responseFuture, 5 seconds)

      result mustBe Nil

      verify(httpHandler).getFromApi(Matchers.eq(s"test/service/tai/$nino/employments/years/${year.year}"))(any())
    }

    "throw an exception" when {
      "invalid json has returned by api" in {
        val SUT = createSUT("test/service")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse("""{"test":"test"}""")))

        val ex = the[RuntimeException] thrownBy Await.result(SUT.employments(nino, year), 5 seconds)
        ex.getMessage mustBe "Invalid employment json"
      }
    }

  }

  "EmploymentsConnector ceasedEmployments" must {

    "return employments from the employments API" when {

      "api provides one employments" in {

        val SUT = createSUT("test/service")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(oneCeasedEmployment)))

        val responseFuture = SUT.ceasedEmployments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe oneEmploymentDetails

        verify(httpHandler).getFromApi(Matchers.eq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"))(any())
      }

      "api provides multiple employments" in {

        val SUT = createSUT("test/service")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(twoCeasedEmployments)))

        val responseFuture = SUT.ceasedEmployments(nino, year)

        val result = Await.result(responseFuture, 5 seconds)

        result mustBe twoEmploymentsDetails

        verify(httpHandler).getFromApi(Matchers.eq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"))(any())
      }
    }

    "return nil when api returns zero employments" in {
      val SUT = createSUT("test/service")

      when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(zeroCeasedEmployments)))

      val responseFuture = SUT.ceasedEmployments(nino, year)

      val result = Await.result(responseFuture, 5 seconds)

      result mustBe Nil

      verify(httpHandler).getFromApi(Matchers.eq(s"test/service/tai/$nino/employments/year/${year.year}/status/ceased"))(any())
    }

    "throw an exception" when {
      "invalid json has returned by api" in {
        val SUT = createSUT("test/service")

        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse("""{"test":"test"}""")))

        val ex = the[RuntimeException] thrownBy Await.result(SUT.ceasedEmployments(nino, year), 5 seconds)
        ex.getMessage mustBe "Invalid employment json"
      }
    }
  }

  "EmploymentsConnector employment" must {

    "return service url" in {
      val sut = createSUT("test")

      sut.employmentUrl(nino, "123") mustBe s"test/tai/${nino}/employments/123"
    }

    "return an employment from current year" when {
      "valid id has been passed" in {
        val sut = createSUT()
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(anEmployment)))

        val result = Await.result(sut.employment(nino, "123"), 5.seconds)

        result mustBe Some(anEmploymentObject)
        verify(httpHandler, times(1)).getFromApi(any())(any())
      }
    }

    "return none" when {
      "invalid json returned by an api" in {
        val sut = createSUT()
        when(httpHandler.getFromApi(any())(any())).thenReturn(Future.successful(Json.parse(zeroEmployments)))

        Await.result(sut.employment(nino, "123"), 5.seconds) mustBe None
      }
    }
  }

  "EmploymentsConnector endEmployment" must {
    "return an envelope" when {
      "we send a PUT request to backend" in {
        val sut = createSUT()
        val json = Json.obj("data" -> JsString("123-456-789"))
        when(httpHandler.putToApi(any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

        val endEmploymentData = EndEmployment(new LocalDate(2017, 10, 15),"YES", Some("EXT-TEST"))

        val result = Await.result(sut.endEmployment(nino, 1, endEmploymentData), 5.seconds)

        result mustBe "123-456-789"
      }
    }

    "return an exception" when {
      "json is invalid" in {
        val sut = createSUT()
        val json = Json.obj("test" -> JsString("123-456-789"))
        when(httpHandler.putToApi(any(), any())(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))
        val endEmploymentData = EndEmployment(new LocalDate(2017, 10, 15),"YES", Some("EXT-TEST"))

        val ex = the[RuntimeException] thrownBy Await.result(sut.endEmployment(nino, 1, endEmploymentData), 5.seconds)

        ex.getMessage mustBe "Invalid json"
      }
    }
  }

  "EmploymentsConnector addEmployment" must {
    "return an envelope id on a successful invocation" in {
      val sut = createSUT()
      val addEmployment = AddEmployment(employerName = "testEmployment", payrollNumber = "12345", startDate = new LocalDate(2017, 6, 6), telephoneContactAllowed = "Yes", telephoneNumber=Some("123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(httpHandler.postToApi(Matchers.eq(sut.addEmploymentServiceUrl(nino)), Matchers.eq(addEmployment))(any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.addEmployment(nino, addEmployment), 5.seconds)

      result mustBe Some("123-456-789")
    }
  }

  "EmploymentsConnector incorrectEmployment" must {
    "return an envelope id on a successful invocation" in {
      val sut = createSUT()
      val model = IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      val json = Json.obj("data" -> JsString("123-456-789"))
      when(httpHandler.postToApi(Matchers.eq(s"/tai/$nino/employments/1/reason"), Matchers.eq(model))
      (any(), any(), any())).thenReturn(Future.successful(HttpResponse(200, Some(json))))

      val result = Await.result(sut.incorrectEmployment(nino, 1, model), 5.seconds)

      result mustBe Some("123-456-789")
    }
  }


  private val anEmploymentObject = Employment("company name", Some("123"),
    new LocalDate("2016-05-26"), Some(new LocalDate("2016-05-26")), Nil, "123", "321", 2, None, false, false)

  private val oneEmploymentDetails = List(anEmploymentObject)
  private val twoEmploymentsDetails = oneEmploymentDetails.head :: oneEmploymentDetails.head.copy(taxDistrictNumber = "1234",
    payeNumber = "4321", sequenceNumber = 3, receivingOccupationalPension = true) :: Nil

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


  private val year: TaxYear = TaxYear(DateTime.now().getYear)
  private val nino: Nino = new Generator(new Random).nextNino
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private def createSUT(servUrl: String = "") = new EmploymentsConnectorTest(servUrl)

  val httpHandler: HttpHandler = mock[HttpHandler]
  
  private class EmploymentsConnectorTest(servUrl: String = "") extends EmploymentsConnector (httpHandler) {
    override val serviceUrl: String = servUrl
  }

}
