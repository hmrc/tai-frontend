/*
 * Copyright 2025 HM Revenue & Customs
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

import builders.{RequestBuilder, UserBuilder}
import cats.data.EitherT
import cats.instances.future.*
import controllers.auth.{AuthedUser, AuthenticatedRequest}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.Html
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.*
import uk.gov.hmrc.tai.model.domain.tax.*
import uk.gov.hmrc.tai.service.{CodingComponentService, HasFormPartialService, TaxAccountService}
import uk.gov.hmrc.tai.util.Money.pounds
import uk.gov.hmrc.tai.util.constants.BandTypesConstants
import uk.gov.hmrc.tai.util.constants.TaxRegionConstants.*
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.*
import utils.BaseSpec
import views.html.estimatedIncomeTax.{ComplexEstimatedIncomeTaxView, NoCurrentIncomeView, SimpleEstimatedIncomeTaxView, ZeroTaxEstimatedIncomeTaxView}
import views.html.includes.link

import scala.concurrent.Future
import uk.gov.hmrc.tai.service.IabdService

import java.time.LocalDate

class EstimatedIncomeTaxControllerSpec extends BaseSpec {

  implicit val request: Request[_]                                 = FakeRequest()
  implicit val fakeAuthenticatedRequest: AuthenticatedRequest[Any] =
    AuthenticatedRequest(request, authedUser, fakePerson(nino))

  private val noCurrentIncomeView           = inject[NoCurrentIncomeView]
  private val simpleEstimatedIncomeTaxView  = inject[SimpleEstimatedIncomeTaxView]
  private val complexEstimatedIncomeTaxView = inject[ComplexEstimatedIncomeTaxView]
  private val zeroTaxEstimatedIncomeTaxView = inject[ZeroTaxEstimatedIncomeTaxView]

  implicit val user: AuthedUser = UserBuilder()

  private def createSUT = new SUT

  val mockCodingComponentService: CodingComponentService = mock[CodingComponentService]
  val mockTaxAccountService: TaxAccountService           = mock[TaxAccountService]
  val mockPartialService: HasFormPartialService          = mock[HasFormPartialService]
  val mockIabdService: IabdService                       = mock[IabdService]

  class SUT
      extends EstimatedIncomeTaxController(
        mockCodingComponentService,
        mockPartialService,
        mockTaxAccountService,
        mockIabdService,
        mockAuthJourney,
        noCurrentIncomeView,
        complexEstimatedIncomeTaxView,
        simpleEstimatedIncomeTaxView,
        zeroTaxEstimatedIncomeTaxView,
        mcc,
        inject[ErrorPagesHandler]
      )

  "EstimatedIncomeTaxController" must {
    "return Ok" when {
      "loading the simple view" in {

        val taxCodeIncome = TaxCodeIncome(
          EmploymentIncome,
          Some(1),
          BigDecimal(39107),
          "EmploymentIncome",
          "277L",
          "TestName",
          OtherBasisOfOperation,
          Live
        )

        val taxAccountSummary = TaxAccountSummary(7834, 2772, 0, 0, 0, 47835, 11500)
        val codingComponents  = Seq(
          CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500)),
          CodingComponent(CarBenefit, Some(1), 8026, "Car Benefit", None),
          CodingComponent(MedicalInsurance, Some(1), 637, "Medical Insurance", None),
          CodingComponent(OtherItems, Some(1), 65, "Other Items", None)
        )

        val basicRateBand      = TaxBand("B", "", 33500, 6700, Some(0), Some(33500), 20)
        val higherRateBand     = TaxBand("D0", "", 2835, 1134, Some(33500), Some(150000), 40)
        val additionalRateBand = TaxBand("D1", "", 0, 0, Some(150000), Some(0), 45)

        val nonTaxCodeIncome = NonTaxCodeIncome(None, List.empty)

        val taxBands = List(basicRateBand, higherRateBand, additionalRateBand)

        val totalTax = TotalTax(
          7834,
          List(IncomeCategory(NonSavingsIncomeCategory, 7834, 36335, 99999, taxBands)),
          None,
          None,
          None,
          None,
          None
        )

        val viewModelBands       = List(
          Band("TaxFree", 24.04, 11500, 0, BandTypesConstants.ZeroBand),
          Band("Band", 75.95, 36335, 7834, BandTypesConstants.NonZeroBand)
        )
        val viewModelBandedGraph = BandedGraph(
          BandTypesConstants.TaxGraph,
          viewModelBands,
          0,
          150000,
          47835,
          24.04,
          11500,
          99.99,
          7834,
          Some("You can earn £102,165 more before your income reaches the next tax band."),
          Some(Swatch(16.37, 7834))
        )

        val viewModelTaxBands = List(TaxBand("pa", "", 11500, 0, Some(0), None, 0), basicRateBand, higherRateBand)

        val viewModel = SimpleEstimatedIncomeTaxViewModel(
          7834,
          47835,
          11500,
          viewModelBandedGraph,
          UkTaxRegion,
          viewModelTaxBands,
          messages("tax.on.your.employment.income"),
          messages(
            "your.total.income.from.employment.desc",
            pounds(47835),
            link(
              id = Some("taxFreeAmountLink"),
              linkClasses = Seq("display-for-print"),
              url = routes.TaxFreeAmountController.taxFreeAmount().url,
              copy = messages("tai.estimatedIncome.taxFree.link")
            ),
            pounds(11500)
          ),
          false
        )

        val sut = createSUT
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.rightT(taxAccountSummary))

        when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
          Future.successful(
            totalTax
          )
        )
        when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
          .thenReturn(Future.successful(codingComponents))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(
            nonTaxCodeIncome
          )
        )
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq(taxCodeIncome)
            )
          )
        )
        when(mockPartialService.getIncomeTaxPartial(any()))
          .thenReturn(Future.successful[HtmlPartial](HtmlPartial.Success(Some("title"), Html("<title/>"))))
        when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
        )

        val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        val content  = Jsoup.parse(contentAsString(result)).getElementsByTag("main").html()
        val expected = Jsoup
          .parse(simpleEstimatedIncomeTaxView(viewModel, Html("<title/>")).toString)
          .getElementsByTag("main")
          .html()
        content mustEqual expected
      }

      "there is an iabd income estimate" when {
        "loading the complex view" in {

          val taxCodeIncome = TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            BigDecimal(15000),
            "EmploymentIncome",
            "1150L",
            "TestName",
            OtherBasisOfOperation,
            Live
          )

          val taxAccountSummary = TaxAccountSummary(700, 11500, 0, 0, 0, 16500, 11500)

          val nonTaxCodeIncome = NonTaxCodeIncome(None, List.empty)

          val codingComponents = Seq(
            CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500))
          )

          val startingSaversRateBand = TaxBand("SR", "", 1500, 0, Some(0), Some(5000), 0)
          val basicRateBand          = TaxBand("B", "", 3500, 700, Some(0), Some(33500), 20)
          val higherRateBand         = TaxBand("D0", "", 0, 0, Some(33500), Some(150000), 40)
          val additionalRateBand     = TaxBand("D1", "", 0, 0, Some(150000), Some(0), 45)

          val nonSavingsTaxBands      = List(basicRateBand, higherRateBand, additionalRateBand)
          val untaxedInterestTaxBands = List(startingSaversRateBand)

          val totalTax = TotalTax(
            700,
            List(
              IncomeCategory(NonSavingsIncomeCategory, 700, 3500, 15000, nonSavingsTaxBands),
              IncomeCategory(UntaxedInterestIncomeCategory, 0, 1500, 1500, untaxedInterestTaxBands)
            ),
            None,
            None,
            None,
            None,
            None
          )

          val viewModelBands       = List(
            Band(BandTypesConstants.TaxFree, 78.78, 13000, 0, BandTypesConstants.ZeroBand),
            Band("Band", 21.21, 3500, 700, BandTypesConstants.NonZeroBand)
          )
          val viewModelBandedGraph = BandedGraph(
            "taxGraph",
            viewModelBands,
            0,
            45000,
            16500,
            78.78,
            13000,
            99.99,
            700,
            Some("You can earn £28,500 more before your income reaches the next tax band."),
            Some(Swatch(4.24, 700))
          )

          val expectedViewModel =
            ComplexEstimatedIncomeTaxViewModel(700, 16500, 11500, viewModelBandedGraph, UkTaxRegion, false)

          val sut = createSUT
          when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
            .thenReturn(EitherT.rightT(taxAccountSummary))
          when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
            Future.successful(
              totalTax
            )
          )
          when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
            .thenReturn(Future.successful(codingComponents))
          when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              nonTaxCodeIncome
            )
          )
          when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              Right(
                Seq(taxCodeIncome)
              )
            )
          )
          when(mockPartialService.getIncomeTaxPartial(any()))
            .thenReturn(Future.successful[HtmlPartial](HtmlPartial.Success(Some("title"), Html("<title/>"))))

          when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              Seq(IabdDetails(Some(1), None, Some(27), None, Some(LocalDate.now), Some(BigDecimal(345678))))
            )
          )

          val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe OK

          val fakeAuthenticatedRequest = AuthenticatedRequest(
            RequestBuilder.buildFakeRequestWithAuth("GET"),
            user,
            Person(nino, "Firstname", "Surname", false, Address(None, None, None, None, None))
          )

          val content  = Jsoup.parse(contentAsString(result))
          val expected = Jsoup
            .parse(
              complexEstimatedIncomeTaxView(
                expectedViewModel.copy(newIncomeEstimateAvailable = true),
                Html("<title/>")
              )(
                fakeAuthenticatedRequest,
                implicitly,
                implicitly
              ).toString
            )
            .getElementsByTag("main")
            .html()

          content.getElementsByTag("main").html() mustEqual expected
          Option(content.getElementById("new-income-estimate")).map(_.text()) mustBe Some(
            "We’re updating your income tax estimate. It will be ready tomorrow."
          )

        }

        "loading the zero tax view" in {

          val taxCodeIncome = TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            BigDecimal(8000),
            "EmploymentIncome",
            "1050L",
            "TestName",
            OtherBasisOfOperation,
            Live
          )

          val taxAccountSummary = TaxAccountSummary(0, 10500, 0, 0, 0, 9000, 11500)

          val nonTaxCodeIncome = NonTaxCodeIncome(None, List.empty)

          val codingComponents = Seq(
            CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500)),
            CodingComponent(
              UntaxedInterestIncome,
              None,
              1000,
              "interest without tax taken off (gross interest)",
              Some(1000)
            )
          )

          val startingSaversRateBand = TaxBand("SR", "", 0, 0, Some(0), Some(5000), 0)
          val personalSaversRateBand = TaxBand("PSR", "", 0, 0, Some(0), Some(5000), 0)

          val untaxedInterestTaxBands = List(startingSaversRateBand, personalSaversRateBand)

          val totalTax = TotalTax(
            0,
            List(
              IncomeCategory(NonSavingsIncomeCategory, 0, 0, 8000, List.empty[TaxBand]),
              IncomeCategory(UntaxedInterestIncomeCategory, 0, 0, 1000, untaxedInterestTaxBands)
            ),
            None,
            None,
            None,
            None,
            None
          )

          val viewModelBands       = List(
            Band(BandTypesConstants.TaxFree, 78.26, 11500, 0, "pa")
          )
          val viewModelBandedGraph =
            BandedGraph("taxGraph", viewModelBands, 0, 11500, 11500, 78.26, 11500, 78.26, 0, None, None)

          val expectedViewModel =
            ZeroTaxEstimatedIncomeTaxViewModel(0, 9000, 11500, viewModelBandedGraph, UkTaxRegion, true)

          val sut = createSUT
          when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
            .thenReturn(EitherT.rightT(taxAccountSummary))
          when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
            Future.successful(
              totalTax
            )
          )
          when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
            .thenReturn(Future.successful(codingComponents))
          when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              nonTaxCodeIncome
            )
          )
          when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              Right(
                Seq(taxCodeIncome)
              )
            )
          )
          when(mockPartialService.getIncomeTaxPartial(any()))
            .thenReturn(Future.successful[HtmlPartial](HtmlPartial.Success(Some("title"), Html("<title/>"))))
          when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              Seq(IabdDetails(Some(1), None, Some(27), None, Some(LocalDate.now), Some(BigDecimal(345678))))
            )
          )

          val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe OK

          val fakeAuthenticatedRequest = AuthenticatedRequest(
            RequestBuilder.buildFakeRequestWithAuth("GET"),
            user,
            Person(nino, "Firstname", "Surname", false, Address(None, None, None, None, None))
          )
          val content                  = Jsoup.parse(contentAsString(result))
          val expected                 = Jsoup
            .parse(
              zeroTaxEstimatedIncomeTaxView(expectedViewModel, Html("<title/>"))(
                fakeAuthenticatedRequest,
                implicitly,
                implicitly
              ).toString()
            )
            .getElementsByTag("main")
            .html()

          content.getElementsByTag("main").html() mustEqual expected
          Option(content.getElementById("new-income-estimate")).map(_.text()) mustBe Some(
            "We’re updating your income tax estimate. It will be ready tomorrow."
          )

        }

        "loading the no income tax view" in {

          val sut = createSUT
          when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
            .thenReturn(EitherT.rightT(TaxAccountSummary(0, 0, 0, 0, 0)))
          when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
            Future.successful(
              TotalTax(0, List.empty[IncomeCategory], None, None, None, None, None)
            )
          )
          when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
            .thenReturn(Future.successful(Seq.empty[CodingComponent]))
          when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
            )
          )
          when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              Right(
                Seq.empty[TaxCodeIncome]
              )
            )
          )
          when(mockPartialService.getIncomeTaxPartial(any()))
            .thenReturn(Future.successful[HtmlPartial](HtmlPartial.Success(Some("title"), Html("<title/>"))))
          when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](
              Seq(IabdDetails(Some(1), None, Some(27), None, Some(LocalDate.now), Some(BigDecimal(345678))))
            )
          )

          val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe OK

          val fakeAuthenticatedRequest = AuthenticatedRequest(
            RequestBuilder.buildFakeRequestWithAuth("GET"),
            user,
            Person(nino, "Firstname", "Surname", false, Address(None, None, None, None, None))
          )
          val content                  = Jsoup.parse(contentAsString(result))
          val expected                 = Jsoup
            .parse(
              noCurrentIncomeView()(
                fakeAuthenticatedRequest,
                implicitly,
                implicitly
              ).toString()
            )
            .getElementsByTag("main")
            .html()

          content.getElementsByTag("main").html() mustEqual expected
          Option(content.getElementById("new-income-estimate")).map(_.text()) mustBe None

        }
      }

      "there is no iabd income estimate" when {
        "loading the complex view" in {

          val taxCodeIncome = TaxCodeIncome(
            EmploymentIncome,
            Some(1),
            BigDecimal(15000),
            "EmploymentIncome",
            "1150L",
            "TestName",
            OtherBasisOfOperation,
            Live
          )

          val taxAccountSummary = TaxAccountSummary(700, 11500, 0, 0, 0, 16500, 11500)

          val nonTaxCodeIncome = NonTaxCodeIncome(None, List.empty)

          val codingComponents = Seq(
            CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500))
          )

          val startingSaversRateBand = TaxBand("SR", "", 1500, 0, Some(0), Some(5000), 0)
          val basicRateBand          = TaxBand("B", "", 3500, 700, Some(0), Some(33500), 20)
          val higherRateBand         = TaxBand("D0", "", 0, 0, Some(33500), Some(150000), 40)
          val additionalRateBand     = TaxBand("D1", "", 0, 0, Some(150000), Some(0), 45)

          val nonSavingsTaxBands      = List(basicRateBand, higherRateBand, additionalRateBand)
          val untaxedInterestTaxBands = List(startingSaversRateBand)

          val totalTax = TotalTax(
            700,
            List(
              IncomeCategory(NonSavingsIncomeCategory, 700, 3500, 15000, nonSavingsTaxBands),
              IncomeCategory(UntaxedInterestIncomeCategory, 0, 1500, 1500, untaxedInterestTaxBands)
            ),
            None,
            None,
            None,
            None,
            None
          )

          val viewModelBands       = List(
            Band(BandTypesConstants.TaxFree, 78.78, 13000, 0, BandTypesConstants.ZeroBand),
            Band("Band", 21.21, 3500, 700, BandTypesConstants.NonZeroBand)
          )
          val viewModelBandedGraph = BandedGraph(
            "taxGraph",
            viewModelBands,
            0,
            45000,
            16500,
            78.78,
            13000,
            99.99,
            700,
            Some("You can earn £28,500 more before your income reaches the next tax band."),
            Some(Swatch(4.24, 700))
          )

          val expectedViewModel =
            ComplexEstimatedIncomeTaxViewModel(700, 16500, 11500, viewModelBandedGraph, UkTaxRegion, false)

          val sut = createSUT
          when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
            .thenReturn(EitherT.rightT(taxAccountSummary))
          when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
            Future.successful(
              totalTax
            )
          )
          when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
            .thenReturn(Future.successful(codingComponents))
          when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              nonTaxCodeIncome
            )
          )
          when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              Right(
                Seq(taxCodeIncome)
              )
            )
          )
          when(mockPartialService.getIncomeTaxPartial(any()))
            .thenReturn(Future.successful[HtmlPartial](HtmlPartial.Success(Some("title"), Html("<title/>"))))
          when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
          )

          val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe OK

          val fakeAuthenticatedRequest = AuthenticatedRequest(
            RequestBuilder.buildFakeRequestWithAuth("GET"),
            user,
            Person(nino, "Firstname", "Surname", false, Address(None, None, None, None, None))
          )

          val content  = Jsoup.parse(contentAsString(result))
          val expected = Jsoup
            .parse(
              complexEstimatedIncomeTaxView(expectedViewModel, Html("<title/>"))(
                fakeAuthenticatedRequest,
                implicitly,
                implicitly
              ).toString
            )
            .getElementsByTag("main")
            .html()

          content.getElementsByTag("main").html() mustEqual expected
          Option(content.getElementById("new-income-estimate")).map(_.text()) mustBe None

        }

        "loading the zero tax view" in {

          val taxCodeIncome = TaxCodeIncome(
            EmploymentIncome,
            Some(2),
            BigDecimal(8000),
            "EmploymentIncome",
            "1050L",
            "TestName",
            OtherBasisOfOperation,
            Live
          )

          val taxAccountSummary = TaxAccountSummary(0, 10500, 0, 0, 0, 9000, 11500)

          val nonTaxCodeIncome = NonTaxCodeIncome(None, List.empty)

          val codingComponents = Seq(
            CodingComponent(PersonalAllowancePA, None, 11500, "Personal Allowance", Some(11500)),
            CodingComponent(
              UntaxedInterestIncome,
              None,
              1000,
              "interest without tax taken off (gross interest)",
              Some(1000)
            )
          )

          val startingSaversRateBand = TaxBand("SR", "", 0, 0, Some(0), Some(5000), 0)
          val personalSaversRateBand = TaxBand("PSR", "", 0, 0, Some(0), Some(5000), 0)

          val untaxedInterestTaxBands = List(startingSaversRateBand, personalSaversRateBand)

          val totalTax = TotalTax(
            0,
            List(
              IncomeCategory(NonSavingsIncomeCategory, 0, 0, 8000, List.empty[TaxBand]),
              IncomeCategory(UntaxedInterestIncomeCategory, 0, 0, 1000, untaxedInterestTaxBands)
            ),
            None,
            None,
            None,
            None,
            None
          )

          val viewModelBands       = List(
            Band(BandTypesConstants.TaxFree, 78.26, 11500, 0, "pa")
          )
          val viewModelBandedGraph =
            BandedGraph("taxGraph", viewModelBands, 0, 11500, 11500, 78.26, 11500, 78.26, 0, None, None)

          val expectedViewModel =
            ZeroTaxEstimatedIncomeTaxViewModel(0, 9000, 11500, viewModelBandedGraph, UkTaxRegion, false)

          val sut = createSUT
          when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
            .thenReturn(EitherT.rightT(taxAccountSummary))
          when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
            Future.successful(
              totalTax
            )
          )
          when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
            .thenReturn(Future.successful(codingComponents))
          when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              nonTaxCodeIncome
            )
          )
          when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              Right(
                Seq(taxCodeIncome)
              )
            )
          )
          when(mockPartialService.getIncomeTaxPartial(any()))
            .thenReturn(Future.successful[HtmlPartial](HtmlPartial.Success(Some("title"), Html("<title/>"))))
          when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
            EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
          )

          val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe OK

          val fakeAuthenticatedRequest = AuthenticatedRequest(
            RequestBuilder.buildFakeRequestWithAuth("GET"),
            user,
            Person(nino, "Firstname", "Surname", false, Address(None, None, None, None, None))
          )
          val content                  = Jsoup.parse(contentAsString(result)).getElementsByTag("main").html()
          val expected                 = Jsoup
            .parse(
              zeroTaxEstimatedIncomeTaxView(expectedViewModel, Html("<title/>"))(
                fakeAuthenticatedRequest,
                implicitly,
                implicitly
              ).toString()
            )
            .getElementsByTag("main")
            .html()

          content mustEqual expected

        }

        "loading the no income tax view" in {

          val sut = createSUT
          when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
            .thenReturn(EitherT.rightT(TaxAccountSummary(0, 0, 0, 0, 0)))
          when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
            Future.successful(
              TotalTax(0, List.empty[IncomeCategory], None, None, None, None, None)
            )
          )
          when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
            .thenReturn(Future.successful(Seq.empty[CodingComponent]))
          when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
            )
          )
          when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
            Future.successful(
              Right(
                Seq.empty[TaxCodeIncome]
              )
            )
          )
          when(mockPartialService.getIncomeTaxPartial(any()))
            .thenReturn(Future.successful[HtmlPartial](HtmlPartial.Success(Some("title"), Html("<title/>"))))

          val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

          status(result) mustBe OK

          val fakeAuthenticatedRequest = AuthenticatedRequest(
            RequestBuilder.buildFakeRequestWithAuth("GET"),
            user,
            Person(nino, "Firstname", "Surname", false, Address(None, None, None, None, None))
          )
          val content                  = Jsoup.parse(contentAsString(result)).getElementsByTag("main").html()
          val expected                 = Jsoup
            .parse(
              noCurrentIncomeView()(
                fakeAuthenticatedRequest,
                implicitly,
                implicitly
              ).toString()
            )
            .getElementsByTag("main")
            .html()

          content mustEqual expected

        }
      }
    }

    "return error" when {
      "failed to fetch details" in {
        val sut = createSUT
        when(mockTaxAccountService.taxAccountSummary(any(), any())(any()))
          .thenReturn(EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR)))
        when(mockTaxAccountService.totalTax(any(), any())(any())).thenReturn(
          Future.successful(
            TotalTax(0, Seq.empty[IncomeCategory], None, None, None)
          )
        )
        when(mockCodingComponentService.taxFreeAmountComponents(any(), any())(any()))
          .thenReturn(Future.successful(Seq.empty[CodingComponent]))
        when(mockTaxAccountService.nonTaxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(
            NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          )
        )
        when(mockTaxAccountService.taxCodeIncomes(any(), any())(any())).thenReturn(
          Future.successful(
            Right(
              Seq.empty[TaxCodeIncome]
            )
          )
        )
        when(mockIabdService.getIabds(any(), any())(any())).thenReturn(
          EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty)
        )

        val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
