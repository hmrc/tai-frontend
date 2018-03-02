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

package controllers

import builders.{AuthBuilder, RequestBuilder, UserBuilder}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import uk.gov.hmrc.tai.model.SessionData
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.service.{ActivityLoggerService, EmploymentService, TaiService}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.tai.{AnnualAccount, TaxYear}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.tai.model.{SessionData, TaxSummaryDetails}
import uk.gov.hmrc.tai.model.rti.RtiStatus

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class YourIncomeCalculationControllerSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport with MockitoSugar {

  "Calling the YourIncomeCalculation method" should {

    "display the current year" in {

      val SUT = createSUT()

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionData)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "display the current year with missing data" in {

      val SUT = createSUT()

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionData)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "display the current year with ceased employments data" in {

      val SUT = createSUT()

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionData)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "display the current year page with rti down message when Rti is Down" in {

      val sessionDataWithRTIError = AuthBuilder.createFakeSessionDataWithPY.copy(
        taxSummaryDetailsCY = TaxSummaryDetails("", 0, accounts = Seq(AnnualAccount(TaxYear(2016),
          None, None, Some(RtiStatus(500, "Response"))))))

      val SUT = createSUT(Some(sessionDataWithRTIError))

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionDataWithRTIError)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      val contents = doc.body()

      contents.toString.contains(Messages("tai.income.calculation.rtiUnavailableCurrentYear.message")) mustBe true
      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "call yourIncomeCalculationPage() successfully with an authorised session " in {

      val SUT = createSUT()

      val result = SUT.yourIncomeCalculationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe 200
    }
  }

  "Calling printYourIncomeCalculation method" should {

    "call printYourIncomeCalculationPage() successfully with an authorised session " in {

      val SUT = createSUT()

      val result = SUT.printYourIncomeCalculationPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe 200
    }

    "print the current year" in {

      val SUT = createSUT()

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None, true)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionData)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "print the current year with missing data" in {

      val SUT = createSUT()

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None, true)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionData)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "print the current year with ceased emplployments data" in {

      val SUT = createSUT()

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None, true)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionData)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)

      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "print the current year page with rti down message when Rti is Down" in {

      val SUT = createSUT()

      val sessionDataWithRTIError = AuthBuilder.createFakeSessionDataWithPY.copy(
        taxSummaryDetailsCY = TaxSummaryDetails("", 0, accounts = Seq(AnnualAccount(TaxYear(2016),
          None, None, Some(RtiStatus(500, "Response"))))))

      val result = SUT.showIncomeCalculationPageForCurrentYear(nino, None, true)(FakeRequest("GET", ""),
        UserBuilder.apply(), sessionDataWithRTIError)

      status(result) mustBe 200
      val content = contentAsString(result)
      val doc = Jsoup.parse(content)
      val contents = doc.body()

      contents.toString.contains(Messages("tai.income.calculation.rtiUnavailableCurrentYear.message")) mustBe true
      doc.title() mustBe Messages("tai.yourIncome.heading")
    }

    "Calling the yourIncomeCalculation for any year" should {

      "display the previous year" in {

        val SUT = createSUT()

        val result = SUT.showHistoricIncomeCalculation(nino, 1, year = TaxYear().prev)(FakeRequest("GET", ""),
          UserBuilder.apply(), sessionData)

        status(result) mustBe 200
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)

        doc.title() mustBe Messages("tai.yourIncome.heading")
      }

      "display the previous year with missing data" in {

        val SUT = createSUT()

        val result = SUT.showHistoricIncomeCalculation(nino, 1)(FakeRequest("GET", ""),
          UserBuilder.apply(), sessionData)

        status(result) mustBe 200
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)

        doc.title() mustBe Messages("tai.yourIncome.heading")
      }

      "display the previous year with ceased employments data" in {

        val SUT = createSUT()

        val result = SUT.showHistoricIncomeCalculation(nino, 1)(FakeRequest("GET", ""),
          UserBuilder.apply(), sessionData)

        status(result) mustBe 200
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)

        doc.title() mustBe Messages("tai.yourIncome.heading")
      }

      "call yourIncomeCalculationPreviousYearPage() successfully with an authorised session " in {

        val SUT = createSUT()

        val result = SUT.yourIncomeCalculationPreviousYearPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe 200
      }

      "call yourIncomeCalculation() successfully with an authorised session " in {

        val SUT = createSUT()

        val result = SUT.yourIncomeCalculation(TaxYear(TaxYear().year - 2), 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe 200
      }

      "call employments service to retrieve employments" in {
        val SUT = createSUT()

        Await.result(SUT.yourIncomeCalculationPreviousYearPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
        verify(SUT.employmentService, times(1)).employments(any(), any())(any())
      }
    }

    "Calling printYourIncomeCalculationPreviousYear method" should {
      "call printYourIncomeCalculationPreviousYearPage() successfully with an authorised session " in {
        val SUT = createSUT()
        val result = SUT.printYourIncomeCalculationPreviousYearPage(1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe 200
      }
    }

    "Calling printYourIncomeCalculation method" should {
      "call printYourIncomeCalculation() successfully with an authorised session " in {
        val SUT = createSUT()
        val result = SUT.printYourIncomeCalculation(TaxYear(TaxYear().year - 2), 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe 200
      }
    }

    "Your income calculation" should {
      "show current year data" when {
        "current year has been passed" in {
          val sut = createSUT()

          val result = sut.yourIncomeCalculation(TaxYear(), 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe 200
        }
      }

      "show historic data" when {
        "historic data has been passed" in {
          val sut = createSUT()

          val result = sut.yourIncomeCalculation(TaxYear().prev, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe 200

          val content = contentAsString(result)
          val doc = Jsoup.parse(content)

          doc.select("#backLink").text() mustBe Messages("tai.back-link.upper")
        }
      }

      "throw bad request" when {
        "next year has been passed" in {
          val sut = createSUT()

          val result = sut.yourIncomeCalculation(TaxYear().next, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe 400
        }
      }
    }

    "Print your income calculation" should {
      "show current year data" when {
        "current year has been passed" in {
          val sut = createSUT()

          val result = sut.printYourIncomeCalculation(TaxYear(), 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe 200

          val content = contentAsString(result)
          val doc = Jsoup.parse(content)

          doc.select("#backLink").text() mustBe Messages("tai.label.back")
        }
      }
      "throw bad request" when {
        "next year has been passed" in {
          val sut = createSUT()

          val result = sut.printYourIncomeCalculation(TaxYear().next, 1)(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe 400
        }
      }
    }
  }

  val sessionData = AuthBuilder.createFakeSessionDataWithPY

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val hc = HeaderCarrier()

  val nino = new Generator().nextNino

  def createSUT(sessionData: Option[SessionData] = None) = new SUT(sessionData)

  class SUT(sessionData: Option[SessionData] = None) extends YourIncomeCalculationController {

    override val taiService = mock[TaiService]
    override val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override val authConnector: AuthConnector = mock[AuthConnector]
    override val activityLoggerService: ActivityLoggerService = mock[ActivityLoggerService]
    override implicit val templateRenderer = MockTemplateRenderer
    override implicit val partialRetriever = MockPartialRetriever
    override val employmentService = mock[EmploymentService]

    val ad = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    val sampleEmployment = Employment("empName", None, new LocalDate(2017, 6, 9), None, Nil, "taxNumber", "payeNumber", 1)
    val emp = when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(sampleEmployment)))

    val sd = sessionData.getOrElse(AuthBuilder.createFakeSessionDataWithPY)
    when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(sd))
  }
}
