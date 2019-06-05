/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.{DateTime, LocalDate}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.EmploymentsConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.{AddEmployment, Employment, EndEmployment, IncorrectIncome}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class EmploymentServiceSpec extends PlaySpec with MockitoSugar {

  "Employment Service" must {
    "return employments" in {
      val sut = createSUT
      when(employmentsConnector.employments(any(), any())(any())).thenReturn(Future.successful(employments))

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
        when(employmentsConnector.employments(any(), any())(any())).thenReturn(Future.successful(employmentDetails))

        val employmentNames = Await.result(sut.employmentNames(nino, year), 5.seconds)

        employmentNames mustBe Map(2 -> "company name")
      }

      "connector returns multiple employment" in {
        val sut = createSUT
        val employment1 = Employment("company name 1", Some("123"), new LocalDate("2016-05-26"),
          Some(new LocalDate("2016-05-26")), Nil, "", "", 1, None, false, false)
        val employment2 = Employment("company name 2", Some("123"), new LocalDate("2016-05-26"),
          Some(new LocalDate("2016-05-26")), Nil, "", "", 2, None, false, false)

        when(employmentsConnector.employments(any(), any())(any())).thenReturn(Future.successful(List(employment1, employment2)))

        val employmentNames = Await.result(sut.employmentNames(nino, year), 5.seconds)

        employmentNames mustBe Map(1 -> "company name 1", 2 -> "company name 2")
      }

      "connector does not return any employment" in {
        val sut = createSUT
        when(employmentsConnector.employments(any(), any())(any())).thenReturn(Future.successful(Seq.empty))

        val data = Await.result(sut.employmentNames(nino, year), 5.seconds)

        data mustBe Map()
      }
    }
  }

  "employment" must {
    "return an employment" when {
      "the connector returns one" in {
        val sut = createSUT

        when(employmentsConnector.employment(any(), any())(any())).thenReturn(Future.successful(Some(employment)))

        val data = Await.result(sut.employment(nino, 8), 5 seconds)

        data mustBe Some(employment)
      }
    }
    "return none" when {
      "the connector does not return an employment" in {
        val sut = createSUT

        when(employmentsConnector.employment(any(), any())(any())).thenReturn(Future.successful(None))

        val data = Await.result(sut.employment(nino, 8), 5 seconds)

        data mustBe None
      }
    }
  }

  "end employment" must {
    "return envelope id" in {
      val sut = createSUT
      when(employmentsConnector.endEmployment(any(), any(), any())(any())).thenReturn(Future.successful("123-456-789"))

      val endEmploymentData = EndEmployment(new LocalDate(2017, 10, 15), "YES", Some("EXT-TEST"))

      val data = Await.result(sut.endEmployment(nino, 1, endEmploymentData), 5.seconds)

      data mustBe "123-456-789"
    }
  }

  "add employment" must {
    "return an envelope id" in {
      val sut = createSUT
      val model = AddEmployment(employerName = "testEmployment", payrollNumber = "12345", startDate = new LocalDate(2017, 6, 6), telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      when(employmentsConnector.addEmployment(Matchers.eq(nino), Matchers.eq(model))(any())).thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.addEmployment(nino, model), 5.seconds)

      envId mustBe "123-456-789"
    }
    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut = createSUT
        val model = AddEmployment(employerName = "testEmployment", payrollNumber = "12345", startDate = new LocalDate(2017, 6, 6), telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
        when(employmentsConnector.addEmployment(Matchers.eq(nino), Matchers.eq(model))(any())).thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy (Await.result(sut.addEmployment(nino, model), 5.seconds))
        rte.getMessage mustBe s"No envelope id was generated when adding the new employment for ${nino.nino}"
      }
    }
  }

  "incorrect employment" must {
    "return an envelope id" in {
      val sut = createSUT
      val model = IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
      when(employmentsConnector.incorrectEmployment(Matchers.eq(nino), Matchers.eq(1), Matchers.eq(model))(any())).thenReturn(Future.successful(Some("123-456-789")))

      val envId = Await.result(sut.incorrectEmployment(nino, 1, model), 5.seconds)

      envId mustBe "123-456-789"
    }

    "generate a runtime exception" when {
      "no envelope id was returned from the connector layer" in {
        val sut = createSUT
        val model = IncorrectIncome(whatYouToldUs = "TEST", telephoneContactAllowed = "Yes", telephoneNumber = Some("123456789"))
        when(employmentsConnector.incorrectEmployment(Matchers.eq(nino), Matchers.eq(1), Matchers.eq(model))(any())).thenReturn(Future.successful(None))

        val rte = the[RuntimeException] thrownBy Await.result(sut.incorrectEmployment(nino, 1, model), 5.seconds)
        rte.getMessage mustBe s"No envelope id was generated when sending incorrect employment details for ${nino.nino}"
      }
    }
  }

  private val year: TaxYear = TaxYear(DateTime.now().getYear)
  private val nino: Nino = new Generator().nextNino
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val employment = Employment("company name", Some("123"), new LocalDate("2016-05-26"),
    Some(new LocalDate("2016-05-26")), Nil, "", "", 2, None, false, false)
  private val employmentDetails = List(employment)
  private val employments = employmentDetails.head :: employmentDetails.head :: Nil

  private def createSUT = new EmploymentServiceTest

  val employmentsConnector = mock[EmploymentsConnector]

  private class EmploymentServiceTest extends EmploymentService(
    employmentsConnector
  )

}
