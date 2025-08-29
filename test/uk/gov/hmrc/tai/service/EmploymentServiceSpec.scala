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

package uk.gov.hmrc.tai.service

import cats.data.EitherT
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.when
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.connectors.EmploymentsConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.*
import uk.gov.hmrc.tai.model.domain.income.Live
import utils.BaseSpec

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EmploymentServiceSpec extends BaseSpec {

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  private val employment = Employment(
    "company name",
    Live,
    Some("123"),
    Some(LocalDate.parse("2016-05-26")),
    Some(LocalDate.parse("2016-05-26")),
    Nil,
    "",
    "",
    2,
    None,
    hasPayrolledBenefit = false,
    receivingOccupationalPension = false,
    EmploymentIncome
  )

  private val employmentDetails = List(employment)
  private val employments       = employmentDetails.head :: employmentDetails.head :: Nil

  private def createSUT = new EmploymentServiceTest

  private val employmentsConnector: EmploymentsConnector = mock[EmploymentsConnector]

  private class EmploymentServiceTest
      extends EmploymentService(
        employmentsConnector
      )

  "Employment Service employments" must {
    "return employments" in {
      val sut = createSUT

      when(employmentsConnector.employments(any(), any())(any()))
        .thenReturn(Future.successful(employments))

      val data = Await.result(sut.employments(nino, year), 5.seconds)

      data mustBe employments
    }
  }

  "Employment Service ceasedEmployments" must {
    "return employments" in {
      val sut = createSUT

      when(employmentsConnector.ceasedEmployments(any(), any())(any()))
        .thenReturn(Future.successful(employments))

      val data = Await.result(sut.ceasedEmployments(nino, year), 5.seconds)

      data mustBe employments
    }
  }

  "Employment Service employmentNames" must {
    "return a map of employment id and employment name (single record)" in {
      val sut = createSUT

      when(employmentsConnector.employments(any(), any())(any()))
        .thenReturn(Future.successful(employmentDetails))

      val employmentNames = Await.result(sut.employmentNames(nino, year), 5.seconds)

      employmentNames mustBe Map(2 -> "company name")
    }

    "return a map of employment id and employment name (multiple records)" in {
      val sut = createSUT

      val employment1 = Employment(
        "company name 1",
        Live,
        Some("123"),
        Some(LocalDate.parse("2016-05-26")),
        Some(LocalDate.parse("2016-05-26")),
        Nil,
        "",
        "",
        1,
        None,
        false,
        false,
        EmploymentIncome
      )
      val employment2 = Employment(
        "company name 2",
        Live,
        Some("123"),
        Some(LocalDate.parse("2016-05-26")),
        Some(LocalDate.parse("2016-05-26")),
        Nil,
        "",
        "",
        2,
        None,
        false,
        false,
        EmploymentIncome
      )

      when(employmentsConnector.employments(any(), any())(any()))
        .thenReturn(Future.successful(List(employment1, employment2)))

      val employmentNames = Await.result(sut.employmentNames(nino, year), 5.seconds)

      employmentNames mustBe Map(1 -> "company name 1", 2 -> "company name 2")
    }

    "return an empty map when there are no employments" in {
      val sut = createSUT

      when(employmentsConnector.employments(any(), any())(any()))
        .thenReturn(Future.successful(Seq.empty))

      val data = Await.result(sut.employmentNames(nino, year), 5.seconds)

      data mustBe Map.empty
    }
  }

  "Employment Service employment" must {
    "return an employment when the connector finds one" in {
      val sut = createSUT

      when(employmentsConnector.employment(any(), any())(any()))
        .thenReturn(Future.successful(Some(employment)))

      val data = Await.result(sut.employment(nino, 8), 5.seconds)

      data mustBe Some(employment)
    }

    "return None when the connector finds nothing" in {
      val sut = createSUT

      when(employmentsConnector.employment(any(), any())(any()))
        .thenReturn(Future.successful(None))

      val data = Await.result(sut.employment(nino, 8), 5.seconds)

      data mustBe None
    }
  }

  "Employment Service employmentOnly" must {
    "return an employment when the connector finds one" in {
      val sut = createSUT

      when(employmentsConnector.employmentOnly(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(employment)))

      val data = Await.result(sut.employmentOnly(nino, 2, year), 5.seconds)

      data mustBe Some(employment)
    }

    "return None when the connector finds nothing" in {
      val sut = createSUT

      when(employmentsConnector.employmentOnly(any(), any(), any())(any()))
        .thenReturn(Future.successful(None))

      val data = Await.result(sut.employmentOnly(nino, 2, year), 5.seconds)

      data mustBe None
    }
  }

  "Employment Service employmentsOnly" must {
    "return Right(employments) on success" in {
      val sut = createSUT

      when(employmentsConnector.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](employments))

      val either = Await.result(sut.employmentsOnly(nino, year).value, 5.seconds)

      either mustBe Right(employments)
    }

    "return Left(error) on failure" in {
      val sut   = createSUT
      val error = UpstreamErrorResponse("boom", 500)

      when(employmentsConnector.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, Seq[Employment]](error))

      val either = Await.result(sut.employmentsOnly(nino, year).value, 5.seconds)

      either mustBe Left(error)
    }
  }

  "Employment Service endEmployment" must {
    "return envelope id" in {
      val sut = createSUT

      when(employmentsConnector.endEmployment(any(), any(), any())(any()))
        .thenReturn(Future.successful("123-456-789"))

      val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))

      val data = Await.result(sut.endEmployment(nino, 1, endEmploymentData), 5.seconds)

      data mustBe "123-456-789"
    }
  }

  "Employment Service addEmployment" must {
    "return an envelope id" in {
      val sut = createSUT

      val model = AddEmployment(
        employerName = "testEmployment",
        payrollNumber = "12345",
        startDate = LocalDate.of(2017, 6, 6),
        payeRef = "123/AB456",
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )

      when(employmentsConnector.addEmployment(meq(nino), meq(model))(any()))
        .thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.addEmployment(nino, model), 5.seconds)

      envId mustBe "123-456-789"
    }

    "throw when connector returns no envelope id" in {
      val sut = createSUT

      val model = AddEmployment(
        employerName = "testEmployment",
        payrollNumber = "12345",
        startDate = LocalDate.of(2017, 6, 6),
        payeRef = "123/AB456",
        telephoneContactAllowed = "Yes",
        telephoneNumber = Some("123456789")
      )

      when(employmentsConnector.addEmployment(meq(nino), meq(model))(any()))
        .thenReturn(Future.successful(None))

      val rte = the[RuntimeException] thrownBy Await.result(sut.addEmployment(nino, model), 5.seconds)
      rte.getMessage mustBe s"No envelope id was generated when adding the new employment for ${nino.nino}"
    }
  }

  "Employment Service incorrectEmployment" must {
    "return an envelope id" in {
      val sut = createSUT

      val model = IncorrectIncome("TEST", "Yes", Some("123456789"))

      when(employmentsConnector.incorrectEmployment(meq(nino), meq(1), meq(model))(any()))
        .thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.incorrectEmployment(nino, 1, model), 5.seconds)

      envId mustBe "123-456-789"
    }

    "throw when connector returns no envelope id" in {
      val sut = createSUT

      val model = IncorrectIncome("TEST", "Yes", Some("123456789"))

      when(employmentsConnector.incorrectEmployment(meq(nino), meq(1), meq(model))(any()))
        .thenReturn(Future.successful(None))

      val rte = the[RuntimeException] thrownBy Await.result(sut.incorrectEmployment(nino, 1, model), 5.seconds)
      rte.getMessage mustBe s"No envelope id was generated when sending incorrect employment details for ${nino.nino}"
    }
  }
}
