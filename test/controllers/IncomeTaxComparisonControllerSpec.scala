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

package controllers

import builders.RequestBuilder
import controllers.actions.FakeValidatePerson
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.{Live, OtherBasisOfOperation, TaxCodeIncome, Week1Month1BasisOfOperation}
import uk.gov.hmrc.tai.service.{CodingComponentService, EmploymentService, TaxAccountService}

import scala.concurrent.Future
import scala.util.Random

class IncomeTaxComparisonControllerSpec extends PlaySpec
  with FakeTaiPlayApplication
  with MockitoSugar
  with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "onPageLoad" must {
    "display the cy plus one page" in {
      val sut = createSut
      when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxCodeIncome]](taxCodeIncomes)))
      when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary)))
      when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(
        Future.successful(Seq.empty[CodingComponent]))
      when(employmentService.employments(Matchers.any(), Matchers.eq(TaxYear()))(Matchers.any())).thenReturn(
        Future.successful(Seq(employment)))

      val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      status(result) mustBe OK

      val doc = Jsoup.parse(contentAsString(result))
      doc.title() must include(Messages("tai.incomeTaxComparison.heading"))

      verify(employmentService, times(1)).employments(Matchers.any(), Matchers.eq(TaxYear()))(Matchers.any())

    }

    "throw an error page" when {
      "not able to fetch comparision details" in {
        val sut = createSut
        when(taxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiNotFoundResponse("Not Found")))
        when(taxAccountService.taxAccountSummary(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[TaxAccountSummary](taxAccountSummary)))
        when(codingComponentService.taxFreeAmountComponents(any(), any())(any())).thenReturn(
          Future.successful(Seq.empty[CodingComponent]))

        val result = sut.onPageLoad()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

  }

  val nino: Nino = new Generator(new Random).nextNino
  val employment = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1, None, false, false)
  val taxAccountSummary = TaxAccountSummary(111, 222, 333, 444, 111)

  val taxCodeIncomes = Seq(
    TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment1", "1150L", "employment", OtherBasisOfOperation, Live),
    TaxCodeIncome(PensionIncome, Some(2), 1111, "employment2", "150L", "employment", Week1Month1BasisOfOperation, Live)
  )

  val codingComponentService = mock[CodingComponentService]
  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]

  def createSut = new SUT()

  class SUT() extends IncomeTaxComparisonController(
    mock[AuditConnector],
    taxAccountService,
    employmentService,
    codingComponentService,
    FakeAuthAction,
    FakeValidatePerson,
    mock[FormPartialRetriever],
    MockTemplateRenderer
  )

}
