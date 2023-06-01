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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.{IncomeSources, TaxYear}
import uk.gov.hmrc.tai.viewModels.TaxAccountSummaryViewModel
import utils.{BaseSpec, TaxAccountSummaryTestData}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TaxAccountSummaryServiceSpec extends BaseSpec with BeforeAndAfterEach with TaxAccountSummaryTestData {

  override def beforeEach: Unit = {
    Mockito.reset(taxAccountService)
    Mockito.reset(employmentService)
  }

  "TaxAccountSummaryServiceSpec" should {
    "return a RuntimeException if ceasedEmployments fails" in { // TODO - Add more error scenarios
      val sut = createSUT

      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any(), any()))
        .thenReturn(
          EitherT[Future, UpstreamErrorResponse, Seq[Employment]](
            Future.successful(Left(UpstreamErrorResponse("", BAD_REQUEST)))
          )
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources Live PensionsIncome fails" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(PensionIncome), meq(Live))(any[HeaderCarrier]))
        .thenReturn(
          Future.failed(new BadRequestException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources Live EmploymentIncome fails" in {
      val sut = createSUT

      when(
        taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(EmploymentIncome), meq(Live))(any[HeaderCarrier])
      )
        .thenReturn(
          Future.failed(new BadRequestException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources NotLive EmploymentIncome fails" in {
      val sut = createSUT

      when(
        taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(EmploymentIncome), meq(NotLive))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(
          Future.failed(new BadRequestException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if NonTaxCodeIncome fails" in {
      val sut = createSUT

      when(
        taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(EmploymentIncome), meq(NotLive))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(
          Future.failed(new BadRequestException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources (PensionIncome) returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(PensionIncome), meq(Live))(any[HeaderCarrier]))
        .thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources (EmploymentIncome) returned failed payload" in {
      val sut = createSUT

      when(
        taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(EmploymentIncome), meq(Live))(any[HeaderCarrier])
      )
        .thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources (EmploymentIncome - NotLive) returned failed payload" in {
      val sut = createSUT

      when(
        taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(EmploymentIncome), meq(NotLive))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if nonTaxCodeIncomes returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.failed(new RuntimeException("Failed to fetch income details"))
      )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if ceasedEmployments returned failed payload" in {
      val sut = createSUT

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(PensionIncome), meq(Live))(any[HeaderCarrier]))
        .thenReturn(
          Future.failed(new RuntimeException("Failed to fetch income details"))
        )

      val caught = the[RuntimeException] thrownBy Await
        .result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a ViewModel" in {
      val sut = createSUT

      val result = Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      result equals TaxAccountSummaryViewModel(
        taxAccountSummary,
        ThreeWeeks,
        nonTaxCodeIncome,
        IncomeSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
        nonMatchedEmployments
      )
    }

  }

  def createSUT = new SUT()

  val personService: PersonService = mock[PersonService]
  val trackingService: TrackingService = mock[TrackingService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]

  class SUT()
      extends TaxAccountSummaryService(
        trackingService = trackingService,
        employmentService = employmentService,
        taxAccountService = taxAccountService,
        mcc = mcc
      ) {

    when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any(), any()))
      .thenReturn(EitherT[Future, UpstreamErrorResponse, Seq[Employment]](Future.successful(Right(Seq(employment)))))

    when(taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(PensionIncome), meq(Live))(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(Seq.empty[TaxedIncome])
      )

    when(taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(EmploymentIncome), meq(Live))(any[HeaderCarrier]))
      .thenReturn(
        Future.successful(Seq.empty[TaxedIncome])
      )

    when(
      taxAccountService.incomeSources(any[Nino], any[TaxYear], meq(EmploymentIncome), meq(NotLive))(any[HeaderCarrier])
    )
      .thenReturn(
        Future.successful(Seq.empty[TaxedIncome])
      )

    when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
      Future.successful(nonTaxCodeIncome)
    )

    when(trackingService.isAnyIFormInProgress(any())(any(), any())).thenReturn(Future.successful(ThreeWeeks))

    when(personService.personDetails(any())(any(), any())).thenReturn(Future.successful(fakePerson(nino)))
  }

  override def fakePerson(nino: Nino) = Person(nino, "firstname", "surname", false, false, address)

}
