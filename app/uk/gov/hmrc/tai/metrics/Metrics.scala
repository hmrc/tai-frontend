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

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer.Context
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.tai.model.enums.APITypes
import uk.gov.hmrc.tai.model.enums.APITypes.APITypes

@Singleton
class Metrics @Inject()(metrics: com.kenshoo.play.metrics.Metrics) {

  private val registry: MetricRegistry = metrics.defaultRegistry

  val SuccessCounterSuffix = "-success-counter"
  val FailureCounterSuffix = "-failed-counter"
  val TimerSuffix = "-timer"

  val metricDescriptions = Map(
    APITypes.JrsClaimAPI -> "jrs-claim-api"
  )

  def startTimer(api: APITypes): Context = registry.timer(metricDescriptions(api) + TimerSuffix).time()
  def incrementSuccessCounter(api: APITypes): Unit =
    registry.counter(metricDescriptions(api) + SuccessCounterSuffix).inc()
  def incrementFailedCounter(api: APITypes): Unit =
    registry.counter(metricDescriptions(api) + FailureCounterSuffix).inc()

}
