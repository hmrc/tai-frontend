package uk.gov.hmrc.tai.service.yourTaxFreeAmount

import builders.RequestBuilder
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.income.OtherBasisOfOperation
import uk.gov.hmrc.tai.service.benefits.CompanyCarService
import uk.gov.hmrc.tai.service.{EmploymentService, YourTaxFreeAmountService}
import uk.gov.hmrc.tai.util.yourTaxFreeAmount.{AllowancesAndDeductionPairs, TaxFreeInfo, YourTaxFreeAmountComparison}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.{Await, Future}
import scala.util.Random

class DescribedYourTaxFreeAmountServiceSpec extends PlaySpec with MockitoSugar {

  "taxFreeAmountComparison" must {
    "return a TaxFreeAmountComparison with a previous and current" in {
//      val previousCodingComponents = Seq(codingComponent1)
//      val currentCodingComponents = Seq(codingComponent2)
//      val yourTaxFreeAmountComparison = YourTaxFreeAmountComparison(
//        TaxFreeInfo("Previous", S
//      )

//      when(yourTaxFreeAmountService.taxFreeAmountComparison(Matchers.eq(nino))(any(), any()))
//        .thenReturn(Future.successful(taxFreeAmountComparison))
//
//      when(taxCodeChangeService.taxCodeChange(Matchers.eq(nino))(any()))
//        .thenReturn(Future.successful(taxCodeChange))
//
//      val expectedModel: YourTaxFreeAmountComparison =
//        YourTaxFreeAmountComparison(
//          Some(TaxFreeInfo("previousTaxDate", 0, 0)),
//          TaxFreeInfo("currentTaxDate", 0, 0),
//          AllowancesAndDeductionPairs(Seq.empty, Seq.empty)
//        )
//
//      val service = createTestService
//      implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")
//      val result = service.taxFreeAmountComparison(nino)
//
//      Await.result(result, 5.seconds) mustBe expectedModel
    }
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val nino: Nino = new Generator(new Random).nextNino
  private def createTestService = new TestService

  private val yourTaxFreeAmountService: YourTaxFreeAmountService = mock[YourTaxFreeAmountService]
  private val companyCarService: CompanyCarService = mock[CompanyCarService]
  private val employmentService: EmploymentService = mock[EmploymentService]

  private val codingComponent1 = CodingComponent(GiftAidPayments, None, 1000, "GiftAidPayments description")
  private val codingComponent2 = CodingComponent(GiftsSharesCharity, None, 1000, "GiftsSharesCharity description")

  private class TestService extends DescribedYourTaxFreeAmountService(
    yourTaxFreeAmountService: YourTaxFreeAmountService,
    companyCarService: CompanyCarService,
    employmentService: EmploymentService
  )
}