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

import controllers.FakeTaiPlayApplication
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TaiConnector
import uk.gov.hmrc.tai.model._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random


class TaiServiceSpec extends PlaySpec
    with MockitoSugar
    with I18nSupport
    with FakeTaiPlayApplication {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private implicit val hc = HeaderCarrier()

  "personDetails" should {
    "expose core customer details in TaiRoot form" in {
      val sut = createSut
      val nino = generateNino

      val taiRoot = TaiRoot(nino.nino, 0, "mr", "ggg", Some("reginald"), "ppp", "ggg ppp", false, None)
      when(sut.taiClient.root(any())(any())).thenReturn(Future.successful(taiRoot))

      val result = sut.personDetails("dummy/root/uri")
      Await.result(result, testTimeout) mustBe taiRoot
    }
  }

  val testTimeout = 5 seconds

  def generateNino: Nino = new Generator(new Random).nextNino

  def createSut = new TaiServiceTest

  class TaiServiceTest extends TaiService {
    override val taiClient: TaiConnector = mock[TaiConnector]
  }

}
