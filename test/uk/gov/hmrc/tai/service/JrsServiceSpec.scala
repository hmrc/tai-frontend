/*
 * Copyright 2023 HM Revenue & Customs
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
import cats.implicits.catsStdInstancesForFuture
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.connectors.JrsConnector
import uk.gov.hmrc.tai.model.{Employers, JrsClaims, YearAndMonth}
import utils.BaseSpec

class JrsServiceSpec extends BaseSpec with ScalaFutures with IntegrationPatience {

  val jrsConnector: JrsConnector = mock[JrsConnector]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]

  val jrsService = new JrsService(jrsConnector, mockAppConfig)

  when(mockAppConfig.jrsClaimsEnabled).thenReturn(true)

  val jrsClaimsAPIResponse: JrsClaims = JrsClaims(
    List(
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12"), YearAndMonth("2020-11"))),
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-02"), YearAndMonth("2021-01")))
    )
  )

  val jrsClaimsServiceResponse: JrsClaims = JrsClaims(
    List(
      Employers("ASDA", "ABC-DEFGHIJ", List(YearAndMonth("2021-01"), YearAndMonth("2021-02"))),
      Employers("TESCO", "ABC-DEFGHIJ", List(YearAndMonth("2020-12")))
    )
  )

  "getJrsClaims" should {

    "sort the employers alphabetically and yearAndMonth in ascending order" when {

      "connector returns some jrs data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(EitherT.rightT(jrsClaimsAPIResponse))

        when(mockAppConfig.jrsClaimsFromDate).thenReturn("2020-12")

        val result = jrsService.getJrsClaims(nino)

        result.value.futureValue mustBe Some(jrsClaimsServiceResponse)

        jrsClaimsServiceResponse.toString mustNot contain("November 2020")

      }
    }

    "return none" when {

      "connector returns empty jrs data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(EitherT.rightT(JrsClaims(List.empty)))

        val result = jrsService.getJrsClaims(nino).value.futureValue

        result mustBe None

      }

      "connector returns None" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(
          EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR))
        )

        val result = jrsService.getJrsClaims(nino).value.futureValue

        result mustBe None

      }
    }
  }

  "checkIfJrsClaimsDataExist" should {

    "return true" when {

      "connector returns jrs claim data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(EitherT.rightT(jrsClaimsAPIResponse))

        val result = jrsService.checkIfJrsClaimsDataExist(nino).value.futureValue

        result mustBe Right(true)
      }

    }

    "return false" when {

      "connector returns empty jrs claim data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(EitherT.rightT(JrsClaims(List.empty)))

        val result = jrsService.checkIfJrsClaimsDataExist(nino).value.futureValue

        result mustBe Right(false)
      }

      "connector returns no jrs claim data" in {

        when(jrsConnector.getJrsClaimsForIndividual(nino)(hc)).thenReturn(
          EitherT.leftT(UpstreamErrorResponse("error", INTERNAL_SERVER_ERROR))
        )

        val result = jrsService.checkIfJrsClaimsDataExist(nino).value.futureValue

        result mustBe a[Left[_, _]]
      }

      "jrs claim feature toggle is disabled" in {

        when(mockAppConfig.jrsClaimsEnabled).thenReturn(false)

        val result = jrsService.checkIfJrsClaimsDataExist(nino).value.futureValue

        result mustBe Right(false)
      }
    }
  }

}
