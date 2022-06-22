/*
 * Copyright 2022 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.tai.connectors.responses.{TaiNotFoundResponse, TaiResponse}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import utils.{BaseSpec, TaxAccountSummaryTestData}
import views.html.incomeTaxHistory.IncomeTaxHistoryView

import scala.concurrent.Future

class IncomeTaxHistoryControllerSpec
    extends BaseSpec with TaxAccountSummaryTestData with BeforeAndAfterEach with ScalaFutures {

  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]
  val personService = mock[PersonService]

  override def beforeEach: Unit =
    Mockito.reset(taxAccountService, employmentService, personService)

  class TestController
      extends IncomeTaxHistoryController(
        appConfig,
        personService,
        FakeAuthAction,
        FakeValidatePerson,
        inject[IncomeTaxHistoryView],
        mcc,
        taxAccountService,
        employmentService,
        inject[ErrorPagesHandler]
      )

  implicit val request = RequestBuilder.buildFakeRequestWithAuth("GET")
  val taxYears = (TaxYear().year to (TaxYear().year - 5) by -1).map(TaxYear(_)).toList

  "onPageLoad" must {
    "display the income tax history page" when {
      "employment data is returned" in {

        for (taxYear <- taxYears) {

          when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.successful(
            Right(Seq(taxCodeIncome)))

          when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(
            Seq(empEmployment1, empEmployment2))

          when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))

        }

        val controller = new TestController
        val result = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomeTax.history.pageTitle"))

        verify(employmentService, times(6)).employments(any(), any())(any())
        verify(taxAccountService, times(6)).taxCodeIncomesV2(any(), any())(any())

      }

      "pension data is returned" in {
        for (taxYear <- taxYears) {

          when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.successful(
            Right(Seq(taxCodeIncome)))
          when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(
            Seq(pensionEmployment3, pensionEmployment4))
          when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))

        }

        val controller = new TestController
        val result = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomeTax.history.pageTitle"))

        verify(taxAccountService, times(6)).taxCodeIncomesV2(Matchers.any(), any())(Matchers.any())
        verify(employmentService, times(6)).employments(Matchers.any(), any())(Matchers.any())

      }

      "tax code is empty if the tax account can't be found" in {
        for (_ <- taxYears) {
          when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.successful(
            Left(TaiNotFoundResponse("not found")))
          when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(
            Seq(pensionEmployment3, pensionEmployment4))
          when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))
        }

        val controller = new TestController
        val result = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().text() must include(Messages("tai.incomeTax.history.unavailable"))
      }

      "tax code is empty if the tax account throws an exception" in {
        for (_ <- taxYears) {
          when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.failed(
            new Exception("exception"))
          when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(
            Seq(pensionEmployment3, pensionEmployment4))
          when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))
        }

        val controller = new TestController
        val result = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.body().text() must include(Messages("tai.incomeTax.history.unavailable"))
      }
    }
  }

  "getIncomeTaxYear" must {

    "return an empty tax code if there isn't one" in {
      when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.successful(Right(Seq()))
      when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(Seq(empEmployment1))
      when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))

      val controller = new TestController
      val result = controller.getIncomeTaxYear(nino, TaxYear())

      result.futureValue.incomeTaxHistory.map(_.maybeTaxCode) mustBe List(None)
    }

    "return an empty tax code if the taxAccountService fails to retrieve" in {
      when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.failed(new Exception("exception"))
      when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(Seq(empEmployment1))
      when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))

      val controller = new TestController
      val result = controller.getIncomeTaxYear(nino, TaxYear())

      result.futureValue.incomeTaxHistory.map(_.maybeTaxCode) mustBe List(None)
    }

    "return a tax code if it's returned by the taxAccountService" in {
      when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.successful(
        Right(Seq(taxCodeIncome)))
      when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(Seq(empEmployment1))
      when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))

      val controller = new TestController
      val result = controller.getIncomeTaxYear(nino, TaxYear())

      result.futureValue.incomeTaxHistory.map(_.maybeTaxCode) mustBe List(Some(taxCodeIncome.taxCode))
    }

  }
}
