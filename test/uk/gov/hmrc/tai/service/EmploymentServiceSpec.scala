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
import cats.instances.future.*
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.when
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.connectors.{EmploymentsConnector, RtiConnector}
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.income.Live
import uk.gov.hmrc.tai.model.domain.*
import utils.BaseSpec

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class EmploymentServiceSpec extends BaseSpec {

  private val year: TaxYear = TaxYear(LocalDateTime.now().getYear)

  private val employment        = Employment(
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
    false,
    false,
    EmploymentIncome
  )
  private val employmentDetails = List(employment)
  private val employments       = employmentDetails.head :: employmentDetails.head :: Nil

  private def createSUT = new EmploymentServiceTest

  val employmentsConnector: EmploymentsConnector = mock[EmploymentsConnector]
  val rtiConnector: RtiConnector                 = mock[RtiConnector]

  private class EmploymentServiceTest
      extends EmploymentService(
        employmentsConnector,
        rtiConnector
      )

  "Employment Service" must {
    "return employments with payments when sequence number matches" in {
      val sut = createSUT

      val employment2 = employment.copy(sequenceNumber = 1)
      val employment3 = employment.copy(sequenceNumber = 3)

      val annualAccount  = AnnualAccount(1, TaxYear(2016), Available, Nil, Nil)
      val annualAccount2 = annualAccount.copy(sequenceNumber = 2)
      val annualAccount3 = annualAccount.copy(taxYear = TaxYear(2017))

      when(employmentsConnector.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(employment, employment2, employment3)))
      when(rtiConnector.getPaymentsForYear(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(annualAccount, annualAccount2, annualAccount3)))

      val data = Await.result(sut.employments(nino, year), 5.seconds)

      data mustBe Seq(
        employment.copy(annualAccounts = Seq(annualAccount2)),
        employment2.copy(annualAccounts = Seq(annualAccount, annualAccount3)),
        employment3
      )
    }

    "return employments with no payments when no matching sequence number" in {
      val sut = createSUT

      val annualAccount = AnnualAccount(1, TaxYear(2016), Available, Nil, Nil)

      when(employmentsConnector.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](employments))
      when(rtiConnector.getPaymentsForYear(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(annualAccount)))

      val data = Await.result(sut.employments(nino, year), 5.seconds)

      data mustBe employments
    }

    "return employments with no payments rti call callus" in {
      val sut = createSUT

      when(employmentsConnector.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](employments))
      when(rtiConnector.getPaymentsForYear(any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, Seq[AnnualAccount]](UpstreamErrorResponse.Upstream5xxResponse))

      val data = Await.result(sut.employments(nino, year), 5.seconds)

      data mustBe employments
    }
  }

  "CeasedEmployments Service" must {
    "return employments" in {
      val sut = createSUT

      when(employmentsConnector.ceasedEmployments(any(), any())(any())).thenReturn(Future.successful(employments))

      val data = Await.result(sut.ceasedEmployments(nino, year), 5.seconds)

      data mustBe employments
    }
  }

  "Employment Names" must {
    "return a map of employment id and employment name" when {
      "connector returns one employment" in {
        val sut = createSUT

        when(employmentsConnector.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](employmentDetails))

        val employmentNames = Await.result(sut.employmentNames(nino, year), 5.seconds)

        employmentNames mustBe Map(2 -> "company name")
      }

      "connector returns multiple employment" in {
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

        when(employmentsConnector.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](List(employment1, employment2)))

        val employmentNames = Await.result(sut.employmentNames(nino, year), 5.seconds)

        employmentNames mustBe Map(1 -> "company name 1", 2 -> "company name 2")
      }

      "connector does not return any employment" in {
        val sut = createSUT

        when(employmentsConnector.employmentsOnly(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty))

        val data = Await.result(sut.employmentNames(nino, year), 5.seconds)

        data mustBe Map()
      }
    }
  }

  "employment" must {
    "return an employment" when {
      "the connector returns one with payments from RTI matching the given employment number" in {
        val sut = createSUT

        val annualAccount  = AnnualAccount(2, TaxYear(2016), Available, Nil, Nil)
        val annualAccount2 = annualAccount.copy(taxYear = TaxYear(2017))
        val annualAccount3 = annualAccount.copy(sequenceNumber = 7)

        when(employmentsConnector.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(rtiConnector.getPaymentsForYear(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq(annualAccount, annualAccount2, annualAccount3)))

        val data = Await.result(sut.employment(nino, 8), 5.seconds)

        data mustBe Some(employment.copy(annualAccounts = Seq(annualAccount, annualAccount2)))
      }
      "the connector returns one with no payments from RTI when nothing returned" in {
        val sut = createSUT

        when(employmentsConnector.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(rtiConnector.getPaymentsForYear(any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, UpstreamErrorResponse](Seq.empty[AnnualAccount]))

        val data = Await.result(sut.employment(nino, 8), 5.seconds)

        data mustBe Some(employment)
      }
      "the connector returns one with no annualAccounts when RTI call fails" in {
        val sut = createSUT

        when(employmentsConnector.employmentOnly(any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(employment)))
        when(rtiConnector.getPaymentsForYear(any(), any())(any()))
          .thenReturn(EitherT.leftT[Future, Seq[AnnualAccount]](UpstreamErrorResponse.Upstream5xxResponse))

        val data = Await.result(sut.employment(nino, 8), 5.seconds)

        data mustBe Some(employment)
      }
    }
    "return none" when {
      "the connector does not return an employment" in {
        val sut = createSUT

        when(employmentsConnector.employmentOnly(any(), any(), any())(any())).thenReturn(Future.successful(None))

        val data = Await.result(sut.employment(nino, 8), 5.seconds)

        data mustBe None
      }
    }
  }

  "employmentOnly" must {
    "return an employment for the given tax year" in {
      val sut = createSUT

      when(employmentsConnector.employmentOnly(any(), any(), any())(any()))
        .thenReturn(Future.successful(Some(employment)))

      val data = Await.result(sut.employmentOnly(nino, 2, year), 5.seconds)

      data mustBe Some(employment)
    }
  }

  "employmentsOnly" must {
    "return employments for the given tax year" in {
      val sut = createSUT

      when(employmentsConnector.employmentsOnly(any(), any())(any()))
        .thenReturn(EitherT.rightT(List(employment)))

      val data = Await.result(sut.employmentsOnly(nino, year).value, 5.seconds)

      data mustBe Right(List(employment))
    }
  }

  "end employment" must {
    "return envelope id" in {
      val sut = createSUT

      when(employmentsConnector.endEmployment(any(), any(), any())(any())).thenReturn(Future.successful("123-456-789"))

      val endEmploymentData = EndEmployment(LocalDate.of(2017, 10, 15), "YES", Some("EXT-TEST"))

      val data = Await.result(sut.endEmployment(nino, 1, endEmploymentData), 5.seconds)

      data mustBe "123-456-789"
    }
  }

  "add employment" must {
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
    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
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
  }

  "incorrect employment" must {
    "return an envelope id" in {
      val sut = createSUT

      val model =
        IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      when(employmentsConnector.incorrectEmployment(meq(nino), meq(1), meq(model))(any()))
        .thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.incorrectEmployment(nino, 1, model), 5.seconds)

      envId mustBe "123-456-789"
    }

    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut = createSUT

        val model =
          IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
        when(employmentsConnector.incorrectEmployment(meq(nino), meq(1), meq(model))(any()))
          .thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy Await.result(sut.incorrectEmployment(nino, 1, model), 5.seconds)
        rte.getMessage mustBe s"No envelope id was generated when sending incorrect employment details for ${nino.nino}"
      }
    }
  }
}
