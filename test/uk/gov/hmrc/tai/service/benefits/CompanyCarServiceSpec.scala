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

package uk.gov.hmrc.tai.service.benefits

import uk.gov.hmrc.tai.connectors.CompanyCarConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiNoCompanyCarFoundResponse, TaiNotFoundResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload}
import org.joda.time.LocalDate
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCar, CompanyCarBenefit, WithdrawCarAndFuel}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

class CompanyCarServiceSpec extends PlaySpec
  with MockitoSugar
  with JourneyCacheConstants
  with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    Mockito.reset(journeyCacheService, carConnector)
  }

  "companyCarBenefits" must {
    "return empty seq of company car benefits" when {
      "connector returns empty seq" in {
        val sut = createSut
        val codingComponents = Seq(CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
          CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
          CodingComponent(CarBenefit, None, 1000, "CarBenefit description")
        )
        when(carConnector.companyCarsForCurrentYearEmployments(any())(any())).thenReturn(Future.successful(Seq.empty[CompanyCarBenefit]))

        val result = sut.companyCarOnCodingComponents(generateNino, codingComponents)
        Await.result(result, 5 seconds) mustBe Seq.empty[CompanyCarBenefit]
        verify(carConnector, times(1)).companyCarsForCurrentYearEmployments(any())(any())
      }

      "Coding components don't have company car benefit" in {
        val sut = createSut
        val codingComponents = Seq(CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
          CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"))

        val result = sut.companyCarOnCodingComponents(generateNino, codingComponents)
        Await.result(result, 5 seconds) mustBe Seq.empty[CompanyCarBenefit]
        verify(carConnector, times(0)).companyCarsForCurrentYearEmployments(any())(any())
      }
    }

    "return seq of company car by removing withdrawn company cars" when {
      "Coding components have company car benefit" in {
        val sut = createSut
        val codingComponents = Seq(CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
          CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description"),
          CodingComponent(CarBenefit, None, 1000, "CarBenefit description")
        )

        when(carConnector.companyCarsForCurrentYearEmployments(any())(any())).thenReturn(Future.successful(companyCars))
        val result = sut.companyCarOnCodingComponents(generateNino, codingComponents)
        Await.result(result, 5 seconds) mustBe Seq(companyCar)
      }
    }
  }

  "beginJourney method" must {
    "update the journey cache" when {
      "there is complete company car information" in {
        val sut = createSut
        when(carConnector.companyCarBenefitForEmployment(any(), any())(any())).thenReturn(Future.successful(Some(companyCar)))
        when(employmentService.employment(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(Some(employment)))
        val expectedMap = Map[String, String](
          CompanyCar_Version -> "1",
          CompanyCar_CarModelKey -> "Make Model",
          CompanyCar_CarProviderKey -> "The Man Plc",
          CompanyCar_CarSeqNoKey -> "10",
          CompanyCar_DateStartedKey -> "2016-10-10",
          CompanyCar_DateFuelBenefitStartedKey -> "2016-10-11",
          CompanyCar_HasActiveFuelBenefitdKey -> "true")
        val expectedResult = TaiSuccessResponseWithPayload(expectedMap)

        when(journeyCacheService.cache(Matchers.eq(expectedMap))(any())).thenReturn(Future.successful(expectedMap))

        Await.result(sut.beginJourney(generateNino, 1), 5 seconds) mustBe expectedResult
      }

      "there is partial company car information, where optional values are absent from the domain model" in {
        val sut = createSut
        when(carConnector.companyCarBenefitForEmployment(any(), any())(any())).thenReturn(Future.successful(Some(companyCarMissingStartDates)))
        when(employmentService.employment(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(Some(employment)))
        val expectedMap = Map[String, String](
          CompanyCar_Version -> "1",
          CompanyCar_CarModelKey -> "Make Model",
          CompanyCar_CarProviderKey -> "The Man Plc",
          CompanyCar_CarSeqNoKey -> "10",
          CompanyCar_HasActiveFuelBenefitdKey -> "true")
        val expectedResult = TaiSuccessResponseWithPayload(expectedMap)

        when(journeyCacheService.cache(Matchers.eq(expectedMap))(any())).thenReturn(Future.successful(expectedMap))

        Await.result(sut.beginJourney(generateNino, 1), 5 seconds) mustBe expectedResult
      }
    }

    "return TaiNoCompanyCarFoundResponse" when {
      "there is a company car with date withdrawn" in{
        val sut = createSut
        when(carConnector.companyCarBenefitForEmployment(any(), any())(any())).thenReturn(Future.successful(Some(companyCarWithDateWithDrawn)))
        when(employmentService.employment(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(Some(employment)))

        val expectedResult = TaiNoCompanyCarFoundResponse("No company car found")
        Await.result(sut.beginJourney(generateNino, 1), 5 seconds) mustBe expectedResult
      }

      "there are no company car returned from tai" in {
        val sut = createSut
        when(carConnector.companyCarBenefitForEmployment(any(), any())(any())).thenReturn(Future.successful(None))
        when(employmentService.employment(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(Some(employment)))

        val expectedResult = TaiNoCompanyCarFoundResponse("No company car found")
        Await.result(sut.beginJourney(generateNino, 1), 5 seconds) mustBe expectedResult
      }

      "there are no company cars with in company benefits" in  {
        val sut = createSut
        val companyCar = CompanyCarBenefit(10, 1000, Nil, Some(1))

        when(carConnector.companyCarBenefitForEmployment(any(), any())(any())).thenReturn(Future.successful(Some(companyCar)))
        when(employmentService.employment(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(Some(employment)))

        val expectedResult = TaiNoCompanyCarFoundResponse("No company car found")
        Await.result(sut.beginJourney(generateNino, 1), 5 seconds) mustBe expectedResult
      }
    }
    "return exception"when{
      "there are no employments" in  {
        val sut = createSut

        when(carConnector.companyCarBenefitForEmployment(any(), any())(any())).thenReturn(Future.successful(Some(companyCar)))
        when(employmentService.employment(any(), Matchers.eq(1))(any())).thenReturn(Future.successful(None))

        the[RuntimeException] thrownBy Await.result(sut.beginJourney(generateNino, 1), 5 seconds)
      }
    }
  }

  "isCompanyCarDateWithdrawn" must{
    "return false if there a company car without date withdrawn" in{
      val sut = createSut
      sut.isCompanyCarDateWithdrawn(companyCar) mustBe false
    }
    "return true" when{
      "there is a car with dateWithdrawn" in{
        val sut = createSut
        sut.isCompanyCarDateWithdrawn(companyCarWithDateWithDrawn) mustBe true
      }
      "there is a list of cars and one of them has dateWithdrawn" in{
        val sut = createSut
        sut.isCompanyCarDateWithdrawn(companyCarListWithDateWithDrawn) mustBe true
      }
    }
  }

  "companyCarEmploymentId" must {

    "return the employment id value from journey cache" in {
      val sut = createSut
      when(journeyCacheService.mandatoryValueAsInt(CompanyCar_EmployerIdKey)(hc)).thenReturn(Future.successful(1))
      Await.result(sut.companyCarEmploymentId, 5 seconds) mustBe 1
    }

    "throw a runtime exception when the value is not found in journey cache" in {
      val sut = createSut
      when(journeyCacheService.mandatoryValueAsInt(CompanyCar_EmployerIdKey)(hc)).thenReturn(Future.failed(new RuntimeException("not found")))
      val thrown = the[RuntimeException] thrownBy Await.result(sut.companyCarEmploymentId, 5 seconds)
      thrown.getMessage mustBe "not found"
    }
  }

  "withdrawCompanyCarAndFuel" must {

    "throw exception if the map is empty" in {
      val sut = createSut
      val nino = generateNino
      when(journeyCacheService.currentCache(hc)).thenReturn(Future.successful(Map[String, String]()))
      val thrownErrorResponse = the[RuntimeException] thrownBy Await.result(sut.withdrawCompanyCarAndFuel(nino, "NA"), 5 seconds)
      thrownErrorResponse.getMessage mustBe "Empty value in Company Car cache"
    }

    "throw exception if one of the mandatory field is not available in the cache" in {
      val sut = createSut
      val employmentSeqNum = 1
      val carSeqNum = 2
      val version = 1
      val nino = generateNino

      val sampleCache = Map(CompanyCar_EmployerIdKey -> employmentSeqNum.toString,
        CompanyCar_CarSeqNoKey -> carSeqNum.toString,
        CompanyCar_Version -> version.toString)

      when(journeyCacheService.currentCache(hc)).thenReturn(Future.successful(sampleCache))
      val thrownErrorResponse = the[RuntimeException] thrownBy Await.result(sut.withdrawCompanyCarAndFuel(nino, "NA"), 5 seconds)
      thrownErrorResponse.getMessage mustBe "Empty value in Company Car cache"
    }

    "call the current journey cache to form withdraw company car fuel object" when {
      "there is a fuel date provided" in {
        val sut = createSut
        val employmentSeqNum = 1
        val carSeqNum = 2
        val withdrawDate = new LocalDate("2017-06-09")
        val version = 1
        val withdrawCarAndFuel = WithdrawCarAndFuel(version, withdrawDate, Some(withdrawDate))
        val nino = generateNino
        val expectedResult = TaiSuccessResponse

        val sampleCache = Map(CompanyCar_EmployerIdKey -> employmentSeqNum.toString,
          CompanyCar_CarSeqNoKey -> carSeqNum.toString,
          CompanyCar_DateGivenBackKey -> withdrawDate.toString,
          CompanyCar_DateFuelBenefitStoppedKey -> withdrawDate.toString,
          CompanyCar_Version -> version.toString)

        when(journeyCacheService.currentCache(hc)).thenReturn(Future.successful(sampleCache))

        when(carConnector.withdrawCompanyCarAndFuel(Matchers.eq(nino), Matchers.eq(employmentSeqNum), Matchers.eq(carSeqNum),
          Matchers.eq(withdrawCarAndFuel))(any())).thenReturn(Future.successful(expectedResult))

        val result = Await.result(sut.withdrawCompanyCarAndFuel(nino, "NA"), 5 seconds)
        result mustBe expectedResult
        verify(carConnector, times(1)).withdrawCompanyCarAndFuel(nino, employmentSeqNum, carSeqNum, withdrawCarAndFuel)
        verify(journeyCacheService, times(1)).currentCache
      }
      "withdrawCompanyCarAndFuel results in a failure" in {
        val sut = createSut
        val employmentSeqNum = 1
        val carSeqNum = 2
        val withdrawDate = new LocalDate("2017-06-09")
        val version = 1
        val withdrawCarAndFuel = WithdrawCarAndFuel(version, withdrawDate, Some(withdrawDate))
        val nino = generateNino
        val expectedResult = TaiNotFoundResponse("Something went wrong")

        val sampleCache = Map(CompanyCar_EmployerIdKey -> employmentSeqNum.toString,
          CompanyCar_CarSeqNoKey -> carSeqNum.toString,
          CompanyCar_DateGivenBackKey -> withdrawDate.toString,
          CompanyCar_DateFuelBenefitStoppedKey -> withdrawDate.toString,
          CompanyCar_Version -> version.toString)

        when(journeyCacheService.currentCache(hc)).thenReturn(Future.successful(sampleCache))

        when(carConnector.withdrawCompanyCarAndFuel(Matchers.eq(nino), Matchers.eq(employmentSeqNum), Matchers.eq(carSeqNum),
          Matchers.eq(withdrawCarAndFuel))(any())).thenReturn(Future.successful(expectedResult))

        val result = Await.result(sut.withdrawCompanyCarAndFuel(nino, "NA"), 5 seconds)
        result mustBe expectedResult
        verify(carConnector, times(1)).withdrawCompanyCarAndFuel(nino, employmentSeqNum, carSeqNum, withdrawCarAndFuel)
        verify(journeyCacheService, times(1)).currentCache
      }

      "there is NO fuel date provided" in {
        val sut = createSut
        val employmentSeqNum = 1
        val carSeqNum = 2
        val withdrawDate = new LocalDate("2017-06-09")
        val version = 1
        val withdrawCarAndFuel = WithdrawCarAndFuel(version, withdrawDate, None)
        val nino = generateNino
        val expectedResult = TaiSuccessResponse

        val sampleCache = Map(CompanyCar_EmployerIdKey -> employmentSeqNum.toString,
          CompanyCar_CarSeqNoKey -> carSeqNum.toString,
          CompanyCar_DateGivenBackKey -> withdrawDate.toString,
          CompanyCar_Version -> version.toString)

        when(journeyCacheService.currentCache(hc)).thenReturn(Future.successful(sampleCache))

        when(carConnector.withdrawCompanyCarAndFuel(Matchers.eq(nino), Matchers.eq(employmentSeqNum), Matchers.eq(carSeqNum),
          Matchers.eq(withdrawCarAndFuel))(any())).thenReturn(Future.successful(expectedResult))

        val result = Await.result(sut.withdrawCompanyCarAndFuel(nino, "NA"), 5 seconds)
        result mustBe expectedResult
        verify(carConnector, times(1)).withdrawCompanyCarAndFuel(nino, employmentSeqNum, carSeqNum, withdrawCarAndFuel)
        verify(journeyCacheService, times(1)).currentCache
      }
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val companyCar = CompanyCarBenefit(10, 1000, List(
    CompanyCar(
      10,
      "Make Model",
      hasActiveFuelBenefit = true,
      dateMadeAvailable = Some(new LocalDate("2016-10-10")),
      dateActiveFuelBenefitMadeAvailable = Some(new LocalDate("2016-10-11")),
      dateWithdrawn = None)),
    Some(1)
  )

  val companyCarWithDateWithDrawn = CompanyCarBenefit(10, 1000, List(
    CompanyCar(
      10,
      "Make Model",
      hasActiveFuelBenefit = false,
      dateMadeAvailable = Some(new LocalDate("2016-10-10")),
      dateActiveFuelBenefitMadeAvailable = Some(new LocalDate("2016-10-11")),
      dateWithdrawn = Some(new LocalDate("2017-05-12")))),
    Some(1)
  )

  val companyCarListWithDateWithDrawn = CompanyCarBenefit(10, 1000, List(
    CompanyCar(
      10,
      "Make Model",
      hasActiveFuelBenefit = false,
      dateMadeAvailable = Some(new LocalDate("2016-10-10")),
      dateActiveFuelBenefitMadeAvailable = Some(new LocalDate("2016-10-11")),
      dateWithdrawn = Some(new LocalDate("2017-05-12"))),
    CompanyCar(
      11,
      "Make Model2",
      hasActiveFuelBenefit = true,
      dateMadeAvailable = Some(new LocalDate("2016-10-10")),
      dateActiveFuelBenefitMadeAvailable = Some(new LocalDate("2016-10-11")),
      dateWithdrawn = None)),
    Some(1)
  )

  val companyCarMissingStartDates = CompanyCarBenefit(10, 1000, List(
    CompanyCar(
      10,
      "Make Model",
      hasActiveFuelBenefit = true,
      dateMadeAvailable = None,
      dateActiveFuelBenefitMadeAvailable = None,
      dateWithdrawn = None
    )),
    Some(1)
  )

  val employment = Employment("The Man Plc", None, new LocalDate("2016-06-09"), None, Nil, "", "", 1, None, false, false)
  val companyCars = Seq(companyCar, companyCarWithDateWithDrawn)

  def generateNino: Nino = new Generator(new Random).nextNino

  private def createSut = new SUT

  val employmentService = mock[EmploymentService]
  val journeyCacheService = mock[JourneyCacheService]
  val carConnector = mock[CompanyCarConnector]

  private class SUT extends CompanyCarService(
    carConnector,
    employmentService,
    mock[AuditService],
    journeyCacheService
  )

}
