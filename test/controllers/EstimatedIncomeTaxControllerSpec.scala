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

package controllers

import builders.{AuthBuilder, RequestBuilder, UserBuilder}
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.{FormPartialRetriever, HtmlPartial}
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.connectors.responses.{TaiSuccessResponseWithPayload, TaiTaxAccountFailureResponse}
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income._
import uk.gov.hmrc.tai.model.domain.tax.{IncomeCategory, NonSavingsIncomeCategory, TaxBand, TotalTax}
import uk.gov.hmrc.tai.service.{CodingComponentService, HasFormPartialService, PersonService, TaxAccountService}
import uk.gov.hmrc.tai.viewModels._
import uk.gov.hmrc.tai.viewModels.estimatedIncomeTax.SimpleTaxView

import scala.concurrent.Future
import scala.util.Random

class EstimatedIncomeTaxControllerSpec extends PlaySpec with MockitoSugar with FakeTaiPlayApplication {

  "EstimatedIncomeTaxController" must {
    "return Ok" when {
      "loading the estimated income tax page" in {

        val taxBands = List(basicRateBand, higherRateBand, additionalRateBand)

        val totalTax = TotalTax(7834,
          List(IncomeCategory(NonSavingsIncomeCategory,7834,36335,99999,taxBands)),
          None,
          None,
          None,
          None,
          None)

        val viewModelBands = List(
          Band("TaxFree",24.04,"0%",11500,0,"ZeroBand"),
          Band("Band",75.95, raw"""<a id="taxExplanation" href="/check-income-tax/tax-explanation"><span aria-hidden="true">Check in more detail</span> <span class="visually-hidden">Check tax on income</span></a>""",36335,7834,"NonZeroBand")
        )
        val viewModelBandedGraph = BandedGraph("taxGraph", viewModelBands, 0, 150000, 47835,24.04,11500, 99.99,7834, Some("You can earn £102,165 more before your income reaches the next tax band."),Some(Swatch(16.37,7834)))

        val viewModelTaxBands = List(
          TaxBand("pa", "", 11500, 0, Some(0), None, 0),
          basicRateBand,
          higherRateBand)

        val viewModel = EstimatedIncomeTaxViewModel(true, 7834, 47835,11500, viewModelBandedGraph, Seq.empty, 0,
          Seq.empty, 0, None, false, None, None, None, "UK", taxViewType = SimpleTaxView,mergedTaxBands = viewModelTaxBands)

        val sut = createSUT
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            taxAccountSummary
          )))
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            totalTax
          )))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(codingComponents))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            nonTaxCodeIncome
          )))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            Seq(taxCodeIncome)
          )))
        when(sut.partialService.getIncomeTaxPartial(any())) .thenReturn(Future.successful[HtmlPartial]
          (HtmlPartial.Success(Some("title"), Html("<title/>"))))

        val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
        println(result.toString)

        contentAsString(result) mustEqual(views.html.estimatedIncomeTaxTemp(viewModel,Html("<title/>")).toString())
      }

      "loading the estimated income tax for a complex view " in {

        val startingSaversRateBand = TaxBand("SR","",1500,0,Some(0),Some(5000),0)
        val taxBands = List(basicRateBand, higherRateBand, additionalRateBand, startingSaversRateBand)

        val totalTax = TotalTax(7834,
          List(IncomeCategory(NonSavingsIncomeCategory,7834,36335,99999,taxBands)),
          None,
          None,
          None,
          None,
          None)

        val viewModelBands = List(
          Band("TaxFree",24.04,"0%",11500,0,"ZeroBand"),
          Band("Band",75.95, raw"""<a id="taxExplanation" href="/check-income-tax/tax-explanation"><span aria-hidden="true">Check in more detail</span> <span class="visually-hidden">Check tax on income</span></a>""",36335,7834,"NonZeroBand")
        )
        val viewModelBandedGraph = BandedGraph("taxGraph", viewModelBands, 0, 150000, 47835,24.04,11500, 99.99,7834, Some("You can earn £102,165 more before your income reaches the next tax band."),Some(Swatch(16.37,7834)))

        val viewModelTaxBands = List(
          TaxBand("pa", "", 11500, 0, Some(0), None, 0),
          basicRateBand,
          higherRateBand,
          startingSaversRateBand)

        val viewModel = EstimatedIncomeTaxViewModel(true, 7834, 47835,11500, viewModelBandedGraph, Seq.empty, 0,
          Seq.empty, 0, None, false, None, None, None, "UK", taxViewType = SimpleTaxView,mergedTaxBands = viewModelTaxBands)

        val sut = createSUT
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            taxAccountSummary
          )))
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            totalTax
          )))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(codingComponents))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            nonTaxCodeIncome
          )))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            Seq(taxCodeIncome)
          )))
        when(sut.partialService.getIncomeTaxPartial(any())) .thenReturn(Future.successful[HtmlPartial]
          (HtmlPartial.Success(Some("title"), Html("<title/>"))))

        val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK

        contentAsString(result) mustEqual(views.html.estimatedIncomeTaxTemp(viewModel,Html("<title/>")).toString())

      }
    }

    "return error" when {
      "failed to fetch details" in {
        val sut = createSUT
        when(sut.taxAccountService.taxAccountSummary(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            TotalTax(0 , Seq.empty[IncomeCategory], None, None, None)
          )))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(Seq.empty[CodingComponent]))
        when(sut.taxAccountService.nonTaxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            NonTaxCodeIncome(None, Seq.empty[OtherNonTaxCodeIncome])
          )))
        when(sut.taxAccountService.taxCodeIncomes(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            Seq.empty[TaxCodeIncome]
          )))

        val result = sut.estimatedIncomeTax()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }


  "Tax Relief" must {
    "return Ok" when {
      "loading the tax relief page" in {
        val sut = createSUT
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiSuccessResponseWithPayload(
            TotalTax(0 , Seq.empty[IncomeCategory], None, None, None)
          )))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(Seq.empty[CodingComponent]))

        val result = sut.taxRelief()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe OK
      }
    }

    "return error" when {
      "failed to fetch details" in {
        val sut = createSUT
        when(sut.taxAccountService.totalTax(any(), any())(any())).
          thenReturn(Future.successful(TaiTaxAccountFailureResponse("Failed")))
        when(sut.codingComponentService.taxFreeAmountComponents(any(), any())(any())).
          thenReturn(Future.successful(Seq.empty[CodingComponent]))

        val result = sut.taxRelief()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  implicit val request = FakeRequest()
  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages
  implicit val templateRenderer = MockTemplateRenderer
  implicit val partialRetriever = MockPartialRetriever
  implicit val user = UserBuilder()

  val taxCodeIncome = TaxCodeIncome(EmploymentIncome,Some(1),BigDecimal(39107),"EmploymentIncome","277L","TestName",OtherBasisOperation,Live,None,Some(new LocalDate(2015,11,26)),Some(new LocalDate(2015,11,26)))
  val taxAccountSummary = TaxAccountSummary(7834,2772,0,0,0,47835,11500)
  val codingComponents = Seq(
    CodingComponent(PersonalAllowancePA,None,11500,"Personal Allowance",Some(11500)),
    CodingComponent(CarBenefit,Some(1),8026,"Car Benefit",None),
    CodingComponent(MedicalInsurance,Some(1),637,"Medical Insurance",None),
    CodingComponent(OtherItems,Some(1),65,"Other Items",None)
  )

  val basicRateBand = TaxBand("B","",33500,6700,Some(0),Some(33500),20)
  val higherRateBand = TaxBand("D0","",2835,1134,Some(33500),Some(150000),40)
  val additionalRateBand = TaxBand("D1","",0,0,Some(150000),Some(0),45)

  val nonTaxCodeIncome = NonTaxCodeIncome(None,List.empty)



  val nino: Nino = new Generator(new Random).nextNino
  private def createSUT = new SUT

  class SUT extends EstimatedIncomeTaxController {
    override val personService: PersonService = mock[PersonService]
    override val partialService: HasFormPartialService = mock[HasFormPartialService]
    override val codingComponentService: CodingComponentService = mock[CodingComponentService]
    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: FormPartialRetriever = mock[FormPartialRetriever]

    when(personService.personDetails(any())(any())).thenReturn(Future.successful(fakePerson(nino)))
    when(authConnector.currentAuthority(any(), any())).thenReturn(AuthBuilder.createFakeAuthData)
  }

}
