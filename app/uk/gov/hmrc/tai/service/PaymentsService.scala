/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import uk.gov.hmrc.tai.model.domain.{Employment, Payment}
import uk.gov.hmrc.tai.viewModels.PaymentDetailsViewModel

class PaymentsService @Inject()() {
  def filterDuplicates(employment: Employment): Seq[PaymentDetailsViewModel] = {
    val payments = employment.latestAnnualAccount.map(_.payments).getOrElse(Seq.empty[Payment])
    val paymentsWithoutDuplicates = payments.filterNot(_.duplicate.getOrElse(false))

    paymentsWithoutDuplicates.map(PaymentDetailsViewModel(_))
  }
}
