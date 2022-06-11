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
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.service.{EmploymentService, PersonService, TaxAccountService}
import utils.{BaseSpec, TaxAccountSummaryTestData}
import views.html.incomeTaxHistory.IncomeTaxHistoryView

import scala.concurrent.Future

class IncomeTaxHistoryControllerSpec extends BaseSpec with TaxAccountSummaryTestData with BeforeAndAfterEach {

  val employmentService = mock[EmploymentService]
  val taxAccountService = mock[TaxAccountService]
  val personService = mock[PersonService]

  override def beforeEach: Unit =
    Mockito.reset(taxAccountService, employmentService)

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

        when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.successful(
          Right(Seq(taxCodeIncome)))
        when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(
          Seq(empEmployment1, empEmployment2))

        when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))

        val controller = new TestController
        val result = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomeTax.history.pageTitle"))

        for (taxYear <- taxYears) {
          verify(employmentService, times(1)).employments(Matchers.any(), Matchers.eq(taxYear))(Matchers.any())
          verify(taxAccountService, times(1)).taxCodeIncomesV2(Matchers.any(), Matchers.eq(taxYear))(Matchers.any())
        }
      }

      "pension data is returned" in {

        when(taxAccountService.taxCodeIncomesV2(any(), any())(any())) thenReturn Future.successful(
          Right(Seq(taxCodeIncome)))
        when(employmentService.employments(any(), any())(any())) thenReturn Future.successful(
          Seq(pensionEmployment3, pensionEmployment4))

        when(personService.personDetails(any())(any())) thenReturn Future.successful(fakePerson(nino))

        val controller = new TestController
        val result = controller.onPageLoad()(request)
        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.title() must include(Messages("tai.incomeTax.history.pageTitle"))

        for (taxYear <- taxYears) {
          verify(employmentService, times(1)).employments(Matchers.any(), Matchers.eq(taxYear))(Matchers.any())
          verify(taxAccountService, times(1)).taxCodeIncomesV2(Matchers.any(), Matchers.eq(taxYear))(Matchers.any())
        }
      }
    }
  }
}
