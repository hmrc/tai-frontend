/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.income.estimatedPay.update

import builders.RequestBuilder
import controllers.ControllerViewTestHelper
import controllers.auth.{AuthedUser, DataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import pages.income._
import play.api.data.FormBinding.Implicits.formBinding
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsFormUrlEncoded, BodyParser, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.tai.forms._
import uk.gov.hmrc.tai.forms.income.incomeCalculator.{BonusOvertimeAmountForm, BonusPaymentsForm}
import uk.gov.hmrc.tai.model.domain.income.IncomeSource
import repository.JourneyCacheNewRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.util.constants._
import utils.BaseSpec
import views.html.incomes.{BonusPaymentAmountView, BonusPaymentsView}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class IncomeUpdateBonusControllerSpec extends BaseSpec with ControllerViewTestHelper {

  def randomNino(): Nino = new Generator(new Random()).nextNino

  val empId: Int = 1
  val sessionId: String = "testSessionId"
  val employer: IncomeSource = IncomeSource(id = 1, name = "sample employer")
  val mockJourneyCacheNewRepository: JourneyCacheNewRepository = mock[JourneyCacheNewRepository]

  override implicit val fakeRequest: FakeRequest[AnyContent] = RequestBuilder.buildFakeGetRequestWithAuth()

  private val bonusPaymentsView = inject[BonusPaymentsView]
  private val bonusPaymentAmountView = inject[BonusPaymentAmountView]

  class TestIncomeUpdateBonusController()
      extends IncomeUpdateBonusController(
        mockAuthJourney,
        mcc,
        bonusPaymentsView,
        bonusPaymentAmountView,
        mockJourneyCacheNewRepository
      ) {
    when(mockJourneyCacheNewRepository.get(any(), any()))
      .thenReturn(Future.successful(Some(UserAnswers(sessionId, randomNino().nino))))
  }

  val sut = new TestIncomeUpdateBonusController

  private def setup(ua: UserAnswers): OngoingStubbing[ActionBuilder[DataRequest, AnyContent]] =
    when(mockAuthJourney.authWithDataRetrieval) thenReturn new ActionBuilder[DataRequest, AnyContent] {
      override def invokeBlock[A](
        request: Request[A],
        block: DataRequest[A] => Future[Result]
      ): Future[Result] =
        block(
          DataRequest(
            request,
            taiUser = AuthedUser(
              Nino(nino.toString()),
              Some("saUtr"),
              None
            ),
            fullName = "",
            userAnswers = ua
          )
        )

      override def parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser

      override protected def executionContext: ExecutionContext = ec
    }

  val baseUserAnswers: UserAnswers = UserAnswers(sessionId, randomNino().nino)
    .setOrException(UpdateIncomeIdPage, employer.id)
    .setOrException(UpdateIncomeNamePage, employer.name)

  override def beforeEach(): Unit = {
    super.beforeEach()
    setup(UserAnswers(sessionId, randomNino().nino))
    reset(mockJourneyCacheNewRepository)
  }

  "bonusPaymentsPage" must {
    "display bonusPayments with back link to deductions page" in {
      val bonusPayments = "yes"

      val mockUserAnswers: UserAnswers = baseUserAnswers
        .setOrException(UpdateIncomeBonusPaymentsPage, bonusPayments)

      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

      val result = sut.bonusPaymentsPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusPaymentsForm.createForm.fill(YesNoForm(Some(bonusPayments)))
      val expectedView =
        bonusPaymentsView(
          expectedForm,
          employer,
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController.payslipDeductionsPage().url
        )(fakeRequest, messages, authedUser)

      result rendersTheSameViewAs expectedView
    }

    "display bonusPayments with back link to enter your taxable pay for the month" in {

      val bonusPayments = "yes"
      val taxableAmount = "1000"

      val mockUserAnswers: UserAnswers = baseUserAnswers
        .setOrException(UpdateIncomeBonusPaymentsPage, bonusPayments)
        .setOrException(UpdateIncomeTaxablePayPage, taxableAmount)

      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

      val result = sut.bonusPaymentsPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusPaymentsForm.createForm.fill(YesNoForm(Some(bonusPayments)))
      val expectedView =
        bonusPaymentsView(
          expectedForm,
          employer,
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController
            .taxablePayslipAmountPage()
            .url
        )(fakeRequest, messages, authedUser)

      result rendersTheSameViewAs expectedView
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        val result = sut.bonusPaymentsPage(fakeRequest)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handleBonusPayments" must {
    "redirect the user to bonusOvertimeAmountPage page" when {
      "user selected yes" in {

        val mockUserAnswers = baseUserAnswers
          .setOrException(UpdateIncomeBonusOvertimeAmountPage, "100")

        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = sut.handleBonusPayments(employer.id)(
          RequestBuilder.buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.YesValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateBonusController.bonusOvertimeAmountPage().url
        )
      }
    }

    "redirect the user to checkYourAnswers page" when {
      "user selected no" in {

        setup(baseUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(baseUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = sut.handleBonusPayments(employer.id)(
          RequestBuilder.buildFakePostRequestWithAuth(FormValuesConstants.YesNoChoice -> FormValuesConstants.NoValue)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
            .checkYourAnswersPage(employer.id)
            .url
        )
      }
    }

    "redirect user back to how to bonusPayments page" when {
      "user input has error" in {

        val mockUserAnswers = baseUserAnswers.setOrException(UpdateIncomeTaxablePayPage, "1000")

        setup(mockUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(mockUserAnswers)))

        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = sut.handleBonusPayments(employer.id)(fakeRequest)

        status(result) mustBe BAD_REQUEST
        result rendersTheSameViewAs bonusPaymentsView(
          BonusPaymentsForm.createForm.bindFromRequest(),
          employer,
          controllers.income.estimatedPay.update.routes.IncomeUpdatePayslipAmountController
            .taxablePayslipAmountPage()
            .url
        )(
          fakeRequest,
          messages,
          authedUser
        )
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {

        implicit val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = RequestBuilder.buildFakeGetRequestWithAuth()

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        val result = sut.handleBonusPayments(employer.id)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "bonusOvertimeAmountPage" must {
    "display bonusPaymentAmount" in {
      val cachedAmount = "313321"

      val mockUserAnswers = baseUserAnswers
        .setOrException(UpdateIncomeBonusOvertimeAmountPage, cachedAmount)

      setup(mockUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any()))
        .thenReturn(Future.successful(Some(mockUserAnswers)))

      val result = sut.bonusOvertimeAmountPage(fakeRequest)

      status(result) mustBe OK

      val expectedForm = BonusOvertimeAmountForm.createForm.fill(BonusOvertimeAmountForm(Some(cachedAmount)))
      result rendersTheSameViewAs bonusPaymentAmountView(expectedForm, employer)(
        fakeRequest,
        messages,
        authedUser
      )
    }

    "Redirect to /income-summary page" when {
      "user reaches page with no data in cache" in {

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        val result = sut.bonusOvertimeAmountPage(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }

  "handleBonusOvertimeAmount" must {
    "redirect the user to checkYourAnswers page" in {

      setup(baseUserAnswers)

      when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(baseUserAnswers)))
      when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

      val result = sut
        .handleBonusOvertimeAmount(employer.id)(RequestBuilder.buildFakePostRequestWithAuth("amount" -> "£3,000"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.income.estimatedPay.update.routes.IncomeUpdateCalculatorController
          .checkYourAnswersPage(employer.id)
          .url
      )
    }

    "redirect the user to bonusPaymentAmount page" when {
      "user input has error" in {

        setup(baseUserAnswers)

        when(mockJourneyCacheNewRepository.get(any(), any())).thenReturn(Future.successful(Some(baseUserAnswers)))
        when(mockJourneyCacheNewRepository.set(any[UserAnswers])) thenReturn Future.successful(true)

        val result = sut
          .handleBonusOvertimeAmount(employer.id)(RequestBuilder.buildFakePostRequestWithAuth("amount" -> ""))

        status(result) mustBe BAD_REQUEST

        result rendersTheSameViewAs bonusPaymentAmountView(
          BonusOvertimeAmountForm.createForm.bindFromRequest(),
          employer
        )(fakeRequest, messages, authedUser)
      }
    }

    "Redirect to /income-summary page" when {
      "IncomeSource.create returns a left" in {

        when(mockJourneyCacheNewRepository.get(any(), any()))
          .thenReturn(Future.successful(None))

        implicit val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          RequestBuilder.buildFakePostRequestWithAuth("" -> "")

        val result = sut.handleBonusOvertimeAmount(employer.id)(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TaxAccountSummaryController.onPageLoad().url)
      }
    }
  }
}
