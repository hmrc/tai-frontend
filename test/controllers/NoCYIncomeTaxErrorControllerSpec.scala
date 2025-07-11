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

package controllers

import builders.RequestBuilder
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import play.api.i18n.{I18nSupport, Messages}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadRequestException, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.service.EmploymentService
import utils.BaseSpec
import views.html.NoCYIncomeTaxErrorView

import java.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class NoCYIncomeTaxErrorControllerSpec extends BaseSpec with I18nSupport {

  def createSUT(employmentDataFailure: Option[Throwable] = None) =
    new SUT(employmentDataFailure)

  val employmentService: EmploymentService = mock[EmploymentService]

  class SUT(employmentDataFailure: Option[Throwable])
      extends NoCYIncomeTaxErrorController(
        employmentService,
        mock[AuditConnector],
        mockAuthJourney,
        mcc,
        inject[NoCYIncomeTaxErrorView]
      ) {

    val sampleEmployment: Seq[Employment] = Seq(
      Employment(
        "empName",
        Live,
        None,
        Some(LocalDate.of(2017, 6, 9)),
        None,
        Nil,
        "taxNumber",
        "payeNumber",
        1,
        None,
        hasPayrolledBenefit = false,
        receivingOccupationalPension = false,
        EmploymentIncome
      )
    )

    employmentDataFailure match {
      case None            =>
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.successful(sampleEmployment))
      case Some(throwable) =>
        when(employmentService.employments(any(), any())(any())).thenReturn(Future.failed(throwable))
    }

  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(employmentService)
  }

  "Calling the Current Year Page method" should {

    "call noCYIncomeTaxErrorPage() successfully with an authorised session " in {
      val sut    = createSUT()
      val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))
      val doc    = Jsoup.parse(contentAsString(result))
      status(result) mustBe OK
      doc.title() must include(Messages("tai.noCYIncomeError.title"))
    }

    "call employment service to fetch sequence of employments" in {
      val sut = createSUT()
      Await.result(sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET")), 5 seconds)
      verify(employmentService).employments(any(), any())(any())
    }

    "display the page" when {
      "employment service throws NotFound exception" in {
        val sut    = createSUT(employmentDataFailure = Some(new NotFoundException("no data found")))
        val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        Await.result(result, 5 seconds)
        verify(employmentService).employments(any(), any())(any())
        status(result) mustBe OK
      }

      "employment service throws BadRequest exception" in {
        val sut    = createSUT(employmentDataFailure = Some(new BadRequestException("bad request")))
        val result = sut.noCYIncomeTaxErrorPage()(RequestBuilder.buildFakeRequestWithAuth("GET"))

        Await.result(result, 5 seconds)
        verify(employmentService).employments(any(), any())(any())
        status(result) mustBe OK
      }
    }
  }

}
