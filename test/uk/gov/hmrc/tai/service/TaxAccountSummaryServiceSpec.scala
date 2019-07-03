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

import controllers.FakeTaiPlayApplication
import mocks.MockTemplateRenderer
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.{IncomesSources, TaxAccountSummaryViewModel}
import utils.TaxAccountSummaryTestData

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TaxAccountSummaryServiceSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with AuditConstants
  with BeforeAndAfterEach
  with TaxAccountSummaryTestData {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  override def beforeEach: Unit = {
    Mockito.reset(taxAccountService)
    Mockito.reset(employmentService)
  }

  "TaxAccountSummaryServiceSpec" should {
    "return a BadRequestException if ceasedEmployments fails" in {
      val sut = createSUT

      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if incomeSources Live PensionsIncome fails" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if incomeSources Live EmploymentIncome fails" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if incomeSources NotLive EmploymentIncome fails" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(NotLive))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if NonTaxCodeIncome fails" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(NotLive))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a RuntimeException if incomeSources (PensionIncome) returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }


    "return a RuntimeException if incomeSources (EmploymentIncome) returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources (EmploymentIncome - NotLive) returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(NotLive))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )


      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if nonTaxCodeIncomes returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE"))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if ceasedEmployments returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a ViewModel" in {
      val sut = createSUT

      val result = Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      result equals TaxAccountSummaryViewModel(taxAccountSummary,
        ThreeWeeks,
        nonTaxCodeIncome,
        IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
        nonMatchedEmployments)
    }

  }

  def createSUT = new SUT()

  val personService: PersonService = mock[PersonService]
  val trackingService: TrackingService = mock[TrackingService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]

  class SUT() extends TaxAccountSummaryService(
    trackingService = trackingService,
    employmentService = employmentService,
    taxAccountService = taxAccountService,
    partialRetriever = mock[FormPartialRetriever],
    templateRenderer = MockTemplateRenderer
  ) {

    when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
      Future.successful(Seq(employment))
    )

    when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
      Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
    )

    when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
      Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
    )

    when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(NotLive))(any[HeaderCarrier])).thenReturn(
      Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
    )

    when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
      Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
    )

    when(trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(ThreeWeeks))

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakePerson(nino: Nino) = Person(nino, "firstname", "surname", false, false)


}
