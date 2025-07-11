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

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import uk.gov.hmrc.tai.connectors.PensionProviderConnector
import uk.gov.hmrc.tai.model.domain.{AddPensionProvider, IncorrectPensionProvider}
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class PensionProviderServiceSpec extends BaseSpec {

  "add pension provider" must {
    "return an envelope id" in {
      val sut   = createSUT
      val model = AddPensionProvider("name", LocalDate.of(2017, 6, 9), "12345", "Yes", Some("123456789"))
      when(pensionProviderConnector.addPensionProvider(meq(nino), meq(model))(any()))
        .thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.addPensionProvider(nino, model), 5 seconds)

      envId mustBe "123-456-789"
    }

    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut   = createSUT
        val model = AddPensionProvider("name", LocalDate.of(2017, 6, 9), "12345", "Yes", Some("123456789"))
        when(pensionProviderConnector.addPensionProvider(meq(nino), meq(model))(any()))
          .thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy Await.result(sut.addPensionProvider(nino, model), 5.seconds)
        rte.getMessage mustBe s"No envelope id was generated when adding the new pension provider for ${nino.nino}"
      }
    }
  }

  "incorrect pension provider" must {
    "return an envelope id" in {
      val sut   = createSUT
      val model = IncorrectPensionProvider(
        whatYouToldUs = "TEST",
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )
      when(pensionProviderConnector.incorrectPensionProvider(meq(nino), meq(1), meq(model))(any()))
        .thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.incorrectPensionProvider(nino, 1, model), 5 seconds)

      envId mustBe "123-456-789"
    }

    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut   = createSUT
        val model = IncorrectPensionProvider(
          whatYouToldUs = "TEST",
          telephoneContactAllowed = "Yes",
          telephoneNumber = Some("123456789")
        )
        when(pensionProviderConnector.incorrectPensionProvider(meq(nino), meq(1), meq(model))(any()))
          .thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy Await.result(sut.incorrectPensionProvider(nino, 1, model), 5.seconds)
        rte.getMessage mustBe s"No envelope id was generated when submitting incorrect pension for ${nino.nino}"
      }
    }
  }

  private def createSUT = new PensionProviderServiceTest

  val pensionProviderConnector = mock[PensionProviderConnector]

  private class PensionProviderServiceTest
      extends PensionProviderService(
        pensionProviderConnector
      )
}
