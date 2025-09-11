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

import controllers.auth.{AuthedUser, DataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.tai.model.{TaxYear, UserAnswers}
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.{AnnualAccount, Employment, EmploymentIncome, TemporarilyUnavailable}
import uk.gov.hmrc.tai.service.EmploymentService
import utils.BaseSpec
import views.html.IdNotFound
import play.api.mvc.Results.Ok
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import scala.concurrent.Future

class EmpIdCheckSpec extends BaseSpec {

  val mockEmploymentService = mock[EmploymentService]
  val idNotFoundView        = inject[IdNotFound]

  val empIdCheck = EmpIdCheck(mockEmploymentService, idNotFoundView, mcc)

  val employment = Employment(
    "employer1",
    Live,
    None,
    Some(LocalDate.of(2016, 6, 9)),
    None,
    Seq(AnnualAccount(7, TaxYear().prev, TemporarilyUnavailable, Nil, Nil)),
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

  def block: Future[Result] = Future.successful(Ok("expected result"))

  "checkValidId" must {
    "be a NotFound with idNotFoundView" when {
      "the empId does not match one for the list of employments" in {
        when(mockEmploymentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))

        val result = empIdCheck.checkValidId(block, 3)
        status(result) mustBe NOT_FOUND
      }
    }
    "proceed with initial request" when {
      "the empId matches one for the list of employments" in {
        when(mockEmploymentService.employments(any(), any())(any())).thenReturn(Future.successful(Seq(employment)))

        val result = empIdCheck.checkValidId(block, employment.sequenceNumber)
        status(result) mustBe OK
        contentAsString(result) mustBe "expected result"
      }
    }
  }

}
