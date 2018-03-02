/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import controllers.auth.TaiUser
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.{SessionData, TaxCalculation, TaxSummaryDetails}

trait ViewModelFactory {
  type ViewModelType

  def createObject(nino: Nino, details: TaxSummaryDetails)(implicit user: TaiUser, hc: HeaderCarrier): ViewModelType
}


object ViewModelFactory {

  def create(viewModelFactory: ViewModelFactory, nino: Nino, details: TaxSummaryDetails)
            (implicit user: TaiUser, hc: HeaderCarrier): viewModelFactory.ViewModelType = {
    viewModelFactory.createObject(nino, details)
  }
}

trait IncomeViewModelFactory {
  type ViewModelType

  def createObject(nino: Nino, details: TaxSummaryDetails, incomeId: Int)(implicit user: TaiUser, hc: HeaderCarrier): ViewModelType
}

object IncomeViewModelFactory {

  def create(viewModelFactory: IncomeViewModelFactory, nino: Nino, details: TaxSummaryDetails, incomeId: Int)
            (implicit user: TaiUser, hc: HeaderCarrier): viewModelFactory.ViewModelType = {
    viewModelFactory.createObject(nino, details, incomeId)
  }
}


trait SessionViewModelFactory {
  type ViewModelType

  def createObject(nino: Nino, session: SessionData, taxCalc: Option[TaxCalculation] = None)(implicit user: TaiUser, hc: HeaderCarrier): ViewModelType
}

object SessionViewModelFactory {

  def create(viewModelFactory: SessionViewModelFactory, nino: Nino, session: SessionData,
             taxCalc: Option[TaxCalculation] = None)(implicit user: TaiUser, hc: HeaderCarrier):
  viewModelFactory.ViewModelType = {
    viewModelFactory.createObject(nino, session, taxCalc)
  }
}
