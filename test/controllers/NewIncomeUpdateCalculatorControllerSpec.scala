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
import data.TaiData
import mocks.{MockPartialRetriever, MockTemplateRenderer}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.tai.service.{ActivityLoggerService, EmploymentService, JourneyCacheService, TaiService}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.partials.PartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Employment}

import scala.concurrent.Future
import scala.util.Random

class NewIncomeUpdateCalculatorControllerSpec extends PlaySpec with FakeTaiPlayApplication with MockitoSugar {

  implicit val messages: Messages = play.api.i18n.Messages.Implicits.applicationMessages

  "getChooseHowToUpdatePage" must {

    val employmentAmount = (isLive: Boolean, isOccupationalPension: Boolean) => EmploymentAmount(name = "name", description = "description", employmentId = SampleId,
      newAmount = 200, oldAmount = 200, isLive = isLive, isOccupationalPension = isOccupationalPension)

    "return a Result" when {
      val testTaxSummary = TaiData.getEditableCeasedAndIncomeTaxSummary

      "income for edit is None" in {
        val sut = createSut
        when(sut.taiService.incomeForEdit(any(), any())(any())).thenReturn(None)

        val result = sut.getChooseHowToUpdatePage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url
      }

      "income for edit has occupational pension true" in {
        val sut = createSut
        when(sut.taiService.incomeForEdit(any(), any())(any())).thenReturn(Some(employmentAmount(true, true)))

        val result = sut.getChooseHowToUpdatePage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeControllerNew.pensionIncome().url
      }

      "income for edit has isLive as false and occupational pension as false" in {
        val sut = createSut
        when(sut.taiService.incomeForEdit(any(), any())(any())).thenReturn(Some(employmentAmount(false, false)))

        val result = sut.getChooseHowToUpdatePage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.TaxAccountSummaryController.onPageLoad().url
      }

      "income for edit has isLive as true, occupational pension as false and has multiple incomes" in {
        val sut = createSut
        when(sut.taiService.incomeForEdit(any(), any())(any())).thenReturn(Some(employmentAmount(true, false)))

        val result = sut.getChooseHowToUpdatePage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe messages("tai.howToUpdate.title")
      }

      "income for edit has isLive as true, occupational pension as false and has single income" in {
        val sut = createSut
        val testTaxSummary = TaiData.getSinglePensionIncome
        when(sut.taiService.incomeForEdit(any(), any())(any())).thenReturn(Some(employmentAmount(true, false)))

        val result = sut.getChooseHowToUpdatePage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.title() mustBe messages("tai.howToUpdate.title")
      }

      "income for edit has isLive as true, occupational pension as false and has single income with no id" in {
        val sut = createSut
        val nino = fakeNino
        val taxCodeIncomeTotal: TaxCodeIncomeTotal = TaxCodeIncomeTotal(List(TaxCodeIncomeSummary(name = "name", taxCode = "1060L", tax = Tax())), 15000, 880, 4400)

        val taxCodeIncomes = TaxCodeIncomes(employments = Some(taxCodeIncomeTotal), hasDuplicateEmploymentNames = false,
          totalIncome = 15000, totalTaxableIncome = 800, totalTax = 4000)
        val tax = IncreasesTax(Some(Incomes(taxCodeIncomes, NoneTaxCodeIncomes(totalIncome = 0), 15000)), None, 15000)

        val testTaxSummary: TaxSummaryDetails = TaxSummaryDetails(nino.nino, SampleId, Some(tax))
        when(sut.taiService.incomeForEdit(any(), any())(any())).thenReturn(Some(employmentAmount(true, false)))

        val result = sut.getChooseHowToUpdatePage(nino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url
      }
    }
  }

  "chooseHowToUpdatePage" must {
    "display the result" in {
      val sut = createSut
      val result = sut.chooseHowToUpdatePage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.YourIncomeCalculationController.yourIncomeCalculationPage(None).url
    }
  }

  "getWorkingHoursPage" must {
    "return the working page as result" in {
      val sut = createSut
      val testTaxSummary = TaiData.getEditableCeasedAndIncomeTaxSummary

      val result = sut.getWorkingHoursPage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))
      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() mustBe messages("tai.workingHours.title")
    }
  }

  "workingHoursPage" must {
    "display the page" in {
      val sut = createSut
      val result = sut.workingHoursPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() mustBe messages("tai.workingHours.title")
    }
  }

  "processChooseHowToUpdate" must {
    "redirect the user" when {
      "user choose incomeCalculator" in {
        val sut = createSut
        val testTaxSummary = TaiData.getBasicRateTaxSummary
        val result = sut.processChooseHowToUpdate(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeUpdateCalculatorController.workingHoursPage().url
      }

      "user choose enterAnnual" in {
        val sut = createSut
        val testTaxSummary = TaiData.getBasicRateTaxSummary
        val result = sut.processChooseHowToUpdate(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "enterAnnual"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeController.viewIncomeForEdit().url
      }

      "user submits no data to the page" in {
        val sut = createSut
        val testTaxSummary = TaiData.getBasicRateTaxSummary
        val result = sut.processChooseHowToUpdate(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "handleChooseHowToUpdate" must {
    "display the page" in {
      val sut = createSut
      val result = sut.handleChooseHowToUpdate()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("howToUpdate" -> "incomeCalculator"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.IncomeUpdateCalculatorController.workingHoursPage().url
    }
  }

  "getWorkingHoursPage" must {
    "return result with working hours page" in {
      val sut = createSut
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val result = sut.getWorkingHoursPage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() mustBe messages("tai.workingHours.title")
    }
  }

  "workingHoursPage" must {
    "return result with working hours page" in {
      val sut = createSut
      val result = sut.workingHoursPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() mustBe messages("tai.workingHours.title")
    }
  }

  "processWorkingHours" must {
    "redirect the user" when {
      "user choose same" in {
        val sut = createSut
        val testTaxSummary = TaiData.getBasicRateTaxSummary
        val result = sut.processWorkingHours(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "same"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeUpdateCalculatorController.payPeriodPage().url
      }

      "user choose veryDifferent" in {
        val sut = createSut
        val testTaxSummary = TaiData.getBasicRateTaxSummary
        val result = sut.processWorkingHours(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "veryDifferent"), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe routes.IncomeUpdateCalculatorController.calcUnavailablePage().url
      }

      "user submits no data to the page" in {
        val sut = createSut
        val testTaxSummary = TaiData.getBasicRateTaxSummary
        val result = sut.processWorkingHours(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody(), UserBuilder.apply(), sessionData(testTaxSummary))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "handleWorkingHours" must {
    "display the page" in {
      val sut = createSut
      val result = sut.handleWorkingHours()(RequestBuilder.buildFakeRequestWithAuth("POST").withFormUrlEncodedBody("workingHours" -> "same"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.IncomeUpdateCalculatorController.payPeriodPage().url
    }
  }

  "getPayPeriodPage" must {
    "return result with pay period page" in {
      val sut = createSut
      val testTaxSummary = TaiData.getBasicRateTaxSummary
      val result = sut.getPayPeriodPage(fakeNino, SampleId, EmployerName)(RequestBuilder.buildFakeRequestWithAuth("GET"), UserBuilder.apply(), sessionData(testTaxSummary))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() mustBe messages("tai.payPeriod.title")
    }
  }

  "payPeriodPage" must {
    "return result with pay period page" in {
      val sut = createSut
      val result = sut.payPeriodPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe OK
      val doc = Jsoup.parse(contentAsString(result))
      doc.title() mustBe messages("tai.payPeriod.title")
    }
  }

  "how to update page" must {
    "save edit income details" in {
      val sut = createSut
      val employmentAmount = EmploymentAmount(name = "name", description = "description", employmentId = SampleId,
        newAmount = 200, oldAmount = 200, isOccupationalPension = true)
      when(sut.employmentService.employment(any(), any())(any())).
        thenReturn(Future.successful(Some(Employment("Test", None, LocalDate.now(), None, Seq.empty[AnnualAccount], "", "", 1))))
      when(sut.taiService.incomeForEdit(any(), any())(any())).thenReturn(Some(employmentAmount))
      val session = TaiData.getSessionDataWithCYPYRtiData
      when(sut.taiService.updateTaiSession(any())(any())).thenReturn(Future.successful(session))

      val result = sut.howToUpdatePage(employmentAmount.employmentId)(RequestBuilder.buildFakeRequestWithAuth("GET"))

      status(result) mustBe SEE_OTHER
      redirectLocation(result).get mustBe routes.IncomeControllerNew.pensionIncome().url
    }
  }

  val SampleId = 1
  val EmployerName = "sample employer"
  def fakeNino = new Generator(new Random).nextNino

  val fakeTaiRoot = TaiRoot(fakeNino.nino, 0, "Mr", "Kkk", None, "Sss", "Kkk Sss", false, Some(false))

  def sessionData(taxSummary: TaxSummaryDetails) = SessionData(fakeNino.nino, Some(fakeTaiRoot), taxSummary, None, None)

  def createSut = new SUT()

  class SUT extends IncomeUpdateCalculatorController {
    override val taiService: TaiService = mock[TaiService]
    override val activityLoggerService: ActivityLoggerService = mock[ActivityLoggerService]
    override val auditConnector: AuditConnector = mock[AuditConnector]
    override protected val authConnector: AuthConnector = mock[AuthConnector]
    override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override implicit val partialRetriever: PartialRetriever = MockPartialRetriever
    override protected val delegationConnector: DelegationConnector = mock[DelegationConnector]
    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
    override val employmentService: EmploymentService = mock[EmploymentService]

    val ad = AuthBuilder.createFakeAuthData
    when(authConnector.currentAuthority(any(), any())).thenReturn(ad)

    when(taiService.taiSession(any(), any(), any())(any())).thenReturn(Future.successful(AuthBuilder.createFakeSessionDataWithPY))
    when(journeyCacheService.cache(any())(any())).thenReturn(Future.successful(Map.empty[String, String]))
  }

}
