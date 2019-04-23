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

import builders.{AuthBuilder, RequestBuilder}
import controllers.{FakeTaiPlayApplication, TaxAccountSummaryController}
import mocks.MockTemplateRenderer
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.{Matchers, Mockito}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.{contentAsString, status, _}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.util.TaxYearRangeUtil
import uk.gov.hmrc.tai.util.constants.AuditConstants
import uk.gov.hmrc.tai.viewModels.{IncomesSources, TaxAccountSummaryViewModel}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class TaxAccountSummaryServiceSpec extends PlaySpec
  with MockitoSugar
  with FakeTaiPlayApplication
  with I18nSupport
  with AuditConstants
  with BeforeAndAfterEach {

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

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if incomeSources Live PensionsIncome fails" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if incomeSources Live EmploymentIncome fails" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if incomeSources Ceased EmploymentIncome fails" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a BadRequestException if NonTaxCodeIncome fails" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.failed(new BadRequestException("Failed Call"))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[BadRequestException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed Call"
    }

    "return a RuntimeException if incomeSources (PensionIncome) returned failed payload" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }


    "return a RuntimeException if incomeSources (EmploymentIncome) returned failed payload" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
          Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if incomeSources (EmploymentIncome - ceased) returned failed payload" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if nonTaxCodeIncomes returned failed payload" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE"))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a RuntimeException if ceasedEmployments returned failed payload" in {
      val sut = createSUT
      when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
        Future.successful(Seq(employment))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiTaxAccountFailureResponse("FAILURE!"))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
      )

      when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
        Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
      )

      val caught = the[RuntimeException] thrownBy Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
      caught.getMessage mustBe "Failed to fetch income details"
    }

    "return a ViewModel" in {
        val sut = createSUT
        when(employmentService.ceasedEmployments(any[Nino], any[TaxYear])(any[HeaderCarrier])).thenReturn(
          Future.successful(Seq(employment))
        )

        when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(PensionIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
        )

        when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Live))(any[HeaderCarrier])).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
        )

        when(taxAccountService.incomeSources(any[Nino], any[TaxYear], Matchers.eq(EmploymentIncome), Matchers.eq(Ceased))(any[HeaderCarrier])).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[Seq[TaxedIncome]](Seq.empty[TaxedIncome]))
        )

        when(taxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(TaiSuccessResponseWithPayload[NonTaxCodeIncome](nonTaxCodeIncome))
        )

        val result = Await.result(sut.taxAccountSummaryViewModel(nino, taxAccountSummary), 5.seconds)
        result equals TaxAccountSummaryViewModel(taxAccountSummary,
          ThreeWeeks,
          nonTaxCodeIncome,
          IncomesSources(livePensionIncomeSources, liveEmploymentIncomeSources, ceasedEmploymentIncomeSources),
          nonMatchedEmployments)
      }


  }

  def createSUT = new SUT()
  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(111, 222, 333.33, 444.44, 111.11)
  val nonTaxCodeIncome = NonTaxCodeIncome(Some(uk.gov.hmrc.tai.model.domain.income.UntaxedInterest(UntaxedInterestIncome,
    None, 100, "Untaxed Interest", Seq.empty[BankAccount])), Seq(
    OtherNonTaxCodeIncome(Profit, None, 100, "Profit")
  ))

  val liveEmployment1 = TaxCodeIncome(EmploymentIncome, Some(1), 1111, "employment", "1150L", "employer1", OtherBasisOfOperation, Live)
  val liveEmployment2 = TaxCodeIncome(EmploymentIncome, Some(2), 2222, "employment", "BR", "employer2", Week1Month1BasisOfOperation, Live)
  val livePension3 = TaxCodeIncome(PensionIncome, Some(3), 3333, "employment", "1150L", "employer3", OtherBasisOfOperation, Live)
  val livePension4 = TaxCodeIncome(PensionIncome, Some(4), 4444, "employment", "BR", "employer4", Week1Month1BasisOfOperation, Live)
  val potentiallyCeasedEmployment9 = TaxCodeIncome(EmploymentIncome, Some(9), 1111, "employment", "1150L", "employer9", OtherBasisOfOperation, PotentiallyCeased)
  val ceasedEmployment10 = TaxCodeIncome(EmploymentIncome, Some(10), 2222, "employment", "BR", "employer10", Week1Month1BasisOfOperation, Ceased)
  val empEmployment1 = Employment("Employer name1", Some("1ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST1", "PAYE1", 1, None, false, false)
  val empEmployment2 = Employment("Employer name2", Some("1ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST2", "PAYE2", 2, None, false, false)
  val pensionEmployment3 = Employment("Pension name1", Some("3ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST3", "PAYE3", 3, None, false, false)
  val pensionEmployment4 = Employment("Pension name2", Some("4ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST4", "PAYE4", 4, None, false, false)
  val empEmployment9 = Employment("Employer name3", Some("9ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST9", "PAYE9", 9, None, false, false)
  val empEmployment10 = Employment("Employer name4", Some("10ABC"), new LocalDate(2017, 3, 1), Some(new LocalDate(2018, 4, 21)), Seq.empty[AnnualAccount], "DIST10", "PAYE10", 10, None, false, false)


  val livePensionIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(livePension3, pensionEmployment3),
    TaxedIncome(livePension4, pensionEmployment4)
  )

  val liveEmploymentIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(liveEmployment1, empEmployment1),
    TaxedIncome(liveEmployment2, empEmployment2)
  )

  val ceasedEmploymentIncomeSources: Seq[TaxedIncome] = Seq(
    TaxedIncome(potentiallyCeasedEmployment9, empEmployment9),
    TaxedIncome(ceasedEmployment10, empEmployment10)
  )
  val personService: PersonService = mock[PersonService]
  val trackingService: TrackingService = mock[TrackingService]
  val employmentService: EmploymentService = mock[EmploymentService]
  val taxAccountService: TaxAccountService = mock[TaxAccountService]

  class SUT() extends TaxAccountSummaryService(
    trackingService = trackingService,
    employmentService = employmentService,
    taxAccountService = taxAccountService,
    partialRetriever =  mock[FormPartialRetriever],
    templateRenderer = MockTemplateRenderer
  ) {
    when(trackingService.isAnyIFormInProgress(any())(any())).thenReturn(Future.successful(ThreeWeeks))

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
  }

  val payment = Payment(new LocalDate(), BigDecimal(123.45), BigDecimal(678.90), BigDecimal(123.12), BigDecimal(444.44), BigDecimal(555.55), BigDecimal(666.66), Monthly)
  val annualAccount = AnnualAccount("key", uk.gov.hmrc.tai.model.TaxYear(), Available, Seq(payment), Nil)
  val ceasedEmployment = Employment("Ceased employer name", Some("123ABC"), new LocalDate(2017, 3, 1), Some(new LocalDate(2018, 4, 21)), Seq(annualAccount), "DIST123", "PAYE543", 1, None, false, false)

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  override def fakePerson(nino:Nino) = Person(nino, "firstname", "surname", false, false)
  val nino: Nino = new Generator(new Random).nextNino
  val employment = Employment("employment1", None, new LocalDate(), None, Nil, "", "", 1, None, false, false)
  val nonMatchedEmployments = Seq(
    ceasedEmployment.copy(sequenceNumber = 998),
    Employment("Pension name1", Some("3ABC"), new LocalDate(2017, 3, 1), None, Seq.empty[AnnualAccount], "DIST3", "PAYE3", 999, None, false, false)
  )
}
