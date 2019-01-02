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

import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.PreviousYearsIncomeConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IncorrectIncome

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class PreviousYearsIncomeServiceSpec extends PlaySpec with MockitoSugar {

  "previous years income" must {
    "return an envelope id" in {
      val sut = createSUT
      val model = IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      when(previousYearsIncomeConnector.incorrectIncome(Matchers.eq(nino), Matchers.eq(2016), Matchers.eq(model))(any())).thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.incorrectIncome(nino, 2016, model), 5.seconds)

      envId mustBe "123-456-789"
    }

    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut = createSUT
        val model = IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
        when(previousYearsIncomeConnector.incorrectIncome(Matchers.eq(nino), Matchers.eq(2016), Matchers.eq(model))(any())).thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy Await.result(sut.incorrectIncome(nino, 2016, model), 5.seconds)
        rte.getMessage mustBe s"No envelope id was generated when sending previous years income details for ${nino.nino}"
      }
    }
  }

  private val year: TaxYear = TaxYear(DateTime.now().getYear)
  private val nino: Nino = new Generator(new Random).nextNino
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val previousYearsIncomeConnector = mock[PreviousYearsIncomeConnector]

  private def createSUT = new PreviousYearsIncomeDetailsServiceTest

  private class PreviousYearsIncomeDetailsServiceTest extends PreviousYearsIncomeService(
    previousYearsIncomeConnector
  )
}
