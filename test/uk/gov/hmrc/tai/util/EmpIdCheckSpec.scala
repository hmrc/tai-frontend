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

package uk.gov.hmrc.tai.util

import cats.data.EitherT
import controllers.ErrorPagesHandler
import org.mockito.Mockito.reset
import controllers.auth.{AuthedUser, DataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.tai.model.UserAnswers
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{Employment, EmploymentIncome}
import uk.gov.hmrc.tai.service.EmploymentService
import utils.BaseSpec
import views.html.IdNotFound
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDate
import scala.concurrent.Future

class EmpIdCheckSpec extends BaseSpec {

  val mockEmploymentService: EmploymentService = mock[EmploymentService]
  val idNotFoundView: IdNotFound               = inject[IdNotFound]

  val empIdCheck = EmpIdCheck(mockEmploymentService, idNotFoundView, mcc, inject[ErrorPagesHandler])

  val employment = Employment(
    "employer1",
    Live,
    None,
    Some(LocalDate.of(2016, 6, 9)),
    None,
    "taxNumber",
    "payeNumber",
    1,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  protected implicit val dataRequest: DataRequest[AnyContent] = DataRequest(
    fakeRequest,
    taiUser = AuthedUser(
      Nino(nino.toString()),
      Some("saUtr"),
      None
    ),
    fullName = "",
    userAnswers = UserAnswers("", "")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmploymentService)
    when(mockEmploymentService.employmentsOnly(any(), any())(any()))
      .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(employment)))
  }

  "checkValidId" must {
    "be a NotFound with idNotFoundView" when {
      "the empId does not match one for the list of employments" in {
        val result = empIdCheck.checkValidId(2).futureValue(Timeout(Span(5, Seconds)))

        status(Future.successful(result.get)) mustBe NOT_FOUND
        contentAsString(Future.successful(result.get)) mustBe idNotFoundView.apply().toString
      }
    }
    "proceed with initial request" when {
      "the empId matches one for the list of employments" in {
        val result = empIdCheck.checkValidId(employment.sequenceNumber).futureValue
        result mustBe None
      }
    }
    "be an error page" when {
      "the call to employments fails" in {
        when(mockEmploymentService.employmentsOnly(any(), any())(any()))
          .thenReturn(
            EitherT.leftT[Future, Seq[Employment]](UpstreamErrorResponse.apply("Call failed", INTERNAL_SERVER_ERROR))
          )

        val result = empIdCheck.checkValidId(employment.sequenceNumber).futureValue(Timeout(Span(5, Seconds)))

        status(Future.successful(result.get)) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
