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
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tai.connectors.IabdConnector
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.model.domain.IabdDetails

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class IabdServiceSpec extends AnyWordSpec with Matchers with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val nino = Nino("AA123456A")
  private val year = TaxYear(2024)

  private def right(resp: HttpResponse): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    EitherT.rightT(resp)

  private def left(status: Int, body: String = "err"): EitherT[Future, UpstreamErrorResponse, HttpResponse] =
    EitherT.leftT(UpstreamErrorResponse(body, status))

  private def httpOk(body: String): HttpResponse =
    HttpResponse(200, body)

  private def json(details: String*): String = {
    val arr =
      if (details.isEmpty) "[]"
      else details.mkString("[", ",", "]")

    s"""{
       |  "data": {
       |    "iabdDetails": $arr
       |  },
       |  "errors": []
       |}""".stripMargin
  }

  private def oneIabd(
    empId: Option[Int],
    tpe: Option[Int],
    amount: Option[BigDecimal]
  ): String = {
    val emp  = empId.map(i => s""""employmentSequenceNumber": $i""")
    val typ  = tpe.map(i => s""""type": $i""")
    val amt  = amount.map(a => s""""grossAmount": $a""")
    val bits = List(emp, typ, amt).flatten.mkString(",")
    s"{$bits}"
  }

  "getIabds" should {

    "parse a valid payload (no type filter)" in {
      val connector = mock[IabdConnector]
      val body      = json(
        oneIabd(Some(1), Some(27), Some(BigDecimal(12345))),
        oneIabd(Some(2), Some(99), Some(BigDecimal(10)))
      )

      when(connector.getIabds(meq(nino), meq(year), meq(None))(any()))
        .thenReturn(right(httpOk(body)))

      val service = new IabdService(connector)
      val result  = Await.result(service.getIabds(nino, year).value, 5.seconds)

      result.isRight shouldBe true
      val seq = result.toOption.get
      seq.size shouldBe 2
      seq.head shouldBe IabdDetails(Some(1), None, Some(27), None, None, Some(BigDecimal(12345)))
      seq(1)   shouldBe IabdDetails(Some(2), None, Some(99), None, None, Some(BigDecimal(10)))
    }

    "parse a valid payload when type filter is provided (e.g. 027)" in {
      val connector = mock[IabdConnector]
      val body      = json(
        oneIabd(Some(1), Some(27), Some(BigDecimal(11111))),
        oneIabd(Some(3), Some(27), Some(BigDecimal(33333)))
      )

      when(connector.getIabds(meq(nino), meq(year), meq(Some("New Estimated Pay (027)")))(any()))
        .thenReturn(right(httpOk(body)))

      val service = new IabdService(connector)
      val result  = Await.result(service.getIabds(nino, year, Some("New Estimated Pay (027)")).value, 5.seconds)

      result.isRight      shouldBe true
      result.toOption.get shouldBe Seq(
        IabdDetails(Some(1), None, Some(27), None, None, Some(BigDecimal(11111))),
        IabdDetails(Some(3), None, Some(27), None, None, Some(BigDecimal(33333)))
      )
    }

    "fail the future when JSON is invalid (service uses .as[...])" in {
      val connector = mock[IabdConnector]
      val invalid   =
        """
          |{
          |  "data": { "wrongKey": [] },
          |  "errors": []
          |}
          |""".stripMargin

      when(connector.getIabds(meq(nino), meq(year), meq(None))(any()))
        .thenReturn(right(httpOk(invalid)))

      val service = new IabdService(connector)

      assertThrows[Exception] {
        Await.result(service.getIabds(nino, year).value, 5.seconds)
      }
    }

    "propagate Left when connector fails" in {
      val connector = mock[IabdConnector]

      when(connector.getIabds(meq(nino), meq(year), meq(None))(any()))
        .thenReturn(left(500, "boom"))

      val service = new IabdService(connector)
      val result  = Await.result(service.getIabds(nino, year).value, 5.seconds)

      result.isLeft                       shouldBe true
      result.left.toOption.get.statusCode shouldBe 500
    }
  }
}
