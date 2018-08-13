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

package uk.gov.hmrc.tai.service

import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.connectors.responses.TaiSuccessResponseWithPayload
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{TaxCodeHistory, TaxCodeRecord}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class TaxCodeChangeServiceSpec extends PlaySpec with MockitoSugar{

  "taxCodeHistory" must {
    "return the tax code history given a valid nino" in {
      val sut = createSut
      val nino = generateNino

      val taxCodeHistory = TaxCodeHistory(taxCodeRecord1, taxCodeRecord2)

      when(sut.taxCodeChangeConnector.taxCodeHistory(any())(any())).thenReturn(Future.successful(TaiSuccessResponseWithPayload(taxCodeHistory)))

      val result = sut.taxCodeHistory(nino)
      Await.result(result, 5 seconds) mustBe taxCodeHistory
    }
  }


  val date = new LocalDate(2018, 5, 23)
  val taxCodeRecord1 = TaxCodeRecord("A1111", date, date.plusDays(1),"Employer 1")
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = date.plusMonths(1), endDate = date.plusMonths(1).plusDays(1))

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private def generateNino: Nino = new Generator(new Random).nextNino
  private def createSut = new SUT

  private class SUT extends TaxCodeChangeService {
    override val taxCodeChangeConnector: TaxCodeChangeConnector = mock[TaxCodeChangeConnector]
  }

}
