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

package uk.gov.hmrc.tai.metrics

import com.codahale.metrics.{Counter, Timer}
import com.codahale.metrics.Timer.Context
import com.codahale.metrics.MetricRegistry
import uk.gov.hmrc.play.graphite.MicroserviceMetrics



sealed trait Metric
case object GetTaxCalculationMetric extends Metric

trait Metrics {
  def startTimer(m: Metric): Timer.Context
  def incrementSuccessCounter(m: Metric): Unit
  def incrementFailedCounter(m: Metric): Unit
}

object Metrics extends Metrics with MicroserviceMetrics{

  val registry: MetricRegistry = metrics.defaultRegistry
  val timers: Map[Metric, Timer] = Map(
    GetTaxCalculationMetric -> registry.timer("get-taxcalc-summary-timer")
  )

  val successCounters: Map[Metric, Counter] = Map(
    GetTaxCalculationMetric -> registry.counter("get-taxcalc-summary-success-counter")
  )

  val failedCounters: Map[Metric, Counter] = Map(
    GetTaxCalculationMetric -> registry.counter("get-taxcalc-summary-failed-counter")
  )

  override def startTimer(m: Metric): Context = timers(m).time()
  override def incrementSuccessCounter(m: Metric): Unit = successCounters(m).inc()
  override def incrementFailedCounter(m: Metric): Unit = failedCounters(m).inc()
}
