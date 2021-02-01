/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.metrics

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.tai.model.enums.APITypes

class MetricsSpec extends PlaySpec with MockitoSugar {

  "startTimer" must {
    "return a timer context" when {
      "given an api lable" in {
        val mockMetricRegistry = mock[MetricRegistry]
        when(mockMetricRegistry.timer(any()))
          .thenReturn(new Timer())

        val mockMetrics = mock[com.kenshoo.play.metrics.Metrics]
        when(mockMetrics.defaultRegistry)
          .thenReturn(mockMetricRegistry)
        val sut = new Metrics(mockMetrics)

        val timer: Timer.Context = sut.startTimer(APITypes.JrsClaimAPI)
        timer.stop()
        timer.close()

        verify(mockMetricRegistry, times(1))
          .timer(meq("jrs-claim-api-timer"))
      }
    }
  }
  "incrementSuccessCounter" must {
    "increment the given success metric" in {
      val mockCounter = mock[Counter]
      val mockMetricRegistry = mock[MetricRegistry]
      when(mockMetricRegistry.counter(meq("jrs-claim-api-success-counter")))
        .thenReturn(mockCounter)

      val mockMetrics = mock[com.kenshoo.play.metrics.Metrics]
      when(mockMetrics.defaultRegistry)
        .thenReturn(mockMetricRegistry)

      val sut = new Metrics(mockMetrics)

      sut.incrementSuccessCounter(APITypes.JrsClaimAPI)

      verify(mockCounter, times(1)).inc
    }
  }
}
