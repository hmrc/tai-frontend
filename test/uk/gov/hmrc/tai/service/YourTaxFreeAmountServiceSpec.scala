/*
 * Copyright 2020 HM Revenue & Customs
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

import builders.RequestBuilder
import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.util.yourTaxFreeAmount._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class YourTaxFreeAmountServiceSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "taxFreeAmountComparison" must {
    "return a TaxFreeAmountComparison with a previous and current" in {
      val previousCodingComponents = Seq(codingComponent1)
      val currentCodingComponents = Seq(codingComponent2)
      val taxFreeAmountComparison = TaxFreeAmountComparison(previousCodingComponents, currentCodingComponents)

      when(codingComponentService.taxFreeAmountComparison(Matchers.eq(nino))(any()))
        .thenReturn(Future.successful(taxFreeAmountComparison))
      when(taxCodeChangeService.taxCodeChange(Matchers.eq(nino))(any()))
        .thenReturn(Future.successful(taxCodeChange))

      val expectedModel: YourTaxFreeAmountComparison =
        YourTaxFreeAmountComparison(
          Some(TaxFreeInfo("previousTaxDate", 0, 0)),
          TaxFreeInfo("currentTaxDate", 0, 0),
          AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
        )

      val service = createTestService
      implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")
      val result = service.taxFreeAmountComparison(nino)

      Await.result(result, 5.seconds) mustBe expectedModel
    }
  }

  trait YourTaxFreeAmountMock {
    this: YourTaxFreeAmount =>
    override def buildTaxFreeAmount(
      unused1: LocalDate,
      previous: Option[Seq[CodingComponent]],
      unused3: Seq[CodingComponent])(implicit messages: Messages): YourTaxFreeAmountComparison = {

      val previousTaxFreeInfo = previous.map(_ => TaxFreeInfo("previousTaxDate", 0, 0))

      YourTaxFreeAmountComparison(
        previousTaxFreeInfo,
        TaxFreeInfo("currentTaxDate", 0, 0),
        AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
      )
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val nino: Nino = new Generator(new Random).nextNino
  private def createTestService = new TestService

  private val taxCodeChangeService: TaxCodeChangeService = mock[TaxCodeChangeService]
  private val codingComponentService: CodingComponentService = mock[CodingComponentService]

  val startDate = TaxYear().start
  val taxCodeRecord1 = TaxCodeRecord(
    "D0",
    startDate,
    startDate.plusDays(1),
    OtherBasisOfOperation,
    "Employer 1",
    false,
    Some("1234"),
    true)
  val taxCodeRecord2 = taxCodeRecord1.copy(startDate = startDate.plusDays(1), endDate = TaxYear().end)
  val taxCodeChange = TaxCodeChange(Seq(taxCodeRecord1), Seq(taxCodeRecord2))

  private val codingComponent1 = CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description")
  private val codingComponent2 = CodingComponent(GiftsSharesCharity, None, 1000, "GiftsSharesCharity description")

  private class TestService
      extends YourTaxFreeAmountService(
        taxCodeChangeService: TaxCodeChangeService,
        codingComponentService: CodingComponentService) with YourTaxFreeAmountMock
}
