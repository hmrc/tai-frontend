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

package uk.gov.hmrc.tai.service

import builders.UserBuilder
import com.codahale.metrics.Timer
import controllers.FakeTaiPlayApplication
import controllers.auth.TaiUser
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http._
import uk.gov.hmrc.tai.config.WSHttp
import uk.gov.hmrc.tai.metrics.Metrics
import uk.gov.hmrc.tai.model.TaxCalculation

import scala.concurrent.Future
import scala.math.BigDecimal

class TaxCalculationServiceSpec extends PlaySpec with MockitoSugar with I18nSupport with FakeTaiPlayApplication {

  "getTaxCalculation" should {

    "return a tax calculation when the service call successfully receives a tax calculation" in {

      val taxCalculation = TaxCalculation("", BigDecimal(1), taxYear, None)

      val SUT = spy(createSUT(taxCalculation))

      doReturn(Future.successful(TaxCalculationSuccessResponse(taxCalculation))).when(SUT).getTaxCalc(any(), any())(any())

      val response = SUT.getTaxCalculation(nino, taxYear)

      ScalaFutures.whenReady(response) { taxCalcOption =>
        taxCalcOption mustBe Some(taxCalculation)
      }
    }

    "return None when the service call cannot successfully receive a tax calculation" in {

      val taxCalculation = TaxCalculation("", BigDecimal(1), taxYear, None)

      val SUT = spy(createSUT(taxCalculation))

      doReturn(Future.successful(TaxCalculationNotFoundResponse)).when(SUT).getTaxCalc(any(), any())(any())

      val response = SUT.getTaxCalculation(nino, taxYear)

      ScalaFutures.whenReady(response) { taxCalcOption =>
        taxCalcOption mustBe None
      }
    }
  }

  "getTaxCalc" should {

    "use create and use the correct url in the service get request" in {

      val taxCalcUrl = "testUrl"
      val taxCalculation = TaxCalculation("", BigDecimal(1), taxYear, None)
      val taxCalcUrlCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val SUT = createSUT(taxCalculation, taxCalcUrl)

      when(SUT.http.GET[TaxCalculation](any())(any(), any(), any())).thenReturn(Future.successful(taxCalculation))

      val response = SUT.getTaxCalc(nino, taxYear)

      ScalaFutures.whenReady(response) { _ =>

        verify(SUT.http).GET(taxCalcUrlCaptor.capture())(any(), any(), any())

        taxCalcUrlCaptor.getValue mustBe s"$taxCalcUrl/taxcalc/${nino.nino}/taxSummary/$taxYear"
      }
    }

    "return a TaxCalculationSuccessResponse containing the tax calculation when the service call successfully " +
      "receives a tax calculation" in {

      val taxCalculation = TaxCalculation("", BigDecimal(1), taxYear, None)

      val SUT = createSUT(taxCalculation)

      when(SUT.http.GET[TaxCalculation](any())(any(), any(), any())).thenReturn(Future.successful(taxCalculation))

      val response = SUT.getTaxCalc(nino, taxYear)

      ScalaFutures.whenReady(response) { taxCalcOption =>

        taxCalcOption mustBe TaxCalculationSuccessResponse(taxCalculation)

        verify(SUT.metrics, times(1)).incrementSuccessCounter(any())
        verify(SUT.metrics, never()).incrementFailedCounter(any())
      }
    }

    "return a TaxCalculationNotFoundResponse when the service call results in a NotFoundException exception being thrown" in {

      val taxCalculation = TaxCalculation("", BigDecimal(1), taxYear, None)

      val SUT = createSUT(taxCalculation)

      when(SUT.http.GET[TaxCalculation](any())(any(), any(), any())).thenReturn(Future.failed(new NotFoundException("Test Exception")))

      val response = SUT.getTaxCalc(nino, taxYear)

      ScalaFutures.whenReady(response) { taxCalcOption =>

        taxCalcOption mustBe TaxCalculationNotFoundResponse

        verify(SUT.metrics, never()).incrementSuccessCounter(any())
        verify(SUT.metrics, times(1)).incrementFailedCounter(any())
      }
    }

    "return a TaxCalculationForbiddenResponse when the service call results in a Upstream4xxResponse-Forbidden exception " +
      "being thrown" in {

      val taxCalculation = TaxCalculation("", BigDecimal(1), taxYear, None)

      val SUT = createSUT(taxCalculation)

      when(SUT.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.failed(new Upstream4xxResponse("", FORBIDDEN, 0)))

      val response = SUT.getTaxCalc(nino, taxYear)

      ScalaFutures.whenReady(response) { taxCalcOption =>

        taxCalcOption mustBe TaxCalculationForbiddenResponse

        verify(SUT.metrics, never()).incrementSuccessCounter(any())
        verify(SUT.metrics, times(1)).incrementFailedCounter(any())
      }
    }

    "return a TaxCalculationErrorResponse containing the exception when the service call results in any " +
      "exception other than NotFound or Upstream4xxResponse-Forbidden being thrown" in {

      val taxCalculation = TaxCalculation("", BigDecimal(1), taxYear, None)

      val SUT = createSUT(taxCalculation)

      val anotherException = new Exception()

      when(SUT.http.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.failed(anotherException))

      val response = SUT.getTaxCalc(nino, taxYear)

      ScalaFutures.whenReady(response) { taxCalcOption =>

        taxCalcOption mustBe TaxCalculationErrorResponse(anotherException)

        verify(SUT.metrics, never()).incrementSuccessCounter(any())
        verify(SUT.metrics, times(1)).incrementFailedCounter(any())
      }
    }
  }

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val user: TaiUser = UserBuilder.apply()
  private val nino = new Generator().nextNino
  private val taxYear: Int = DateTime.now().getYear

  private def createSUT(taxCalculation: TaxCalculation,
                        taxCalculationUrl: String = "") = new TaxCalculationServiceTest(taxCalculation, taxCalculationUrl)

  private class TaxCalculationServiceTest(taxCalculation: TaxCalculation,
                                          taxCalculationUrl: String = "") extends TaxCalculationService {

    override val http: HttpGet with HttpPost with HttpPut with HttpDelete = mock[WSHttp]
    override val metrics: Metrics = mock[Metrics]
    override val taxCalcUrl: String = taxCalculationUrl

    when(metrics.startTimer(any())).thenReturn(mock[Timer.Context])
  }
}
