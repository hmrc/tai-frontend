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

package controllers.viewModels

import controllers.IncomeViewModelFactory
import controllers.auth.TaiUser
import uk.gov.hmrc.tai.viewModels.YourIncomeCalculationViewModel
import uk.gov.hmrc.tai.model.{TaxCodeIncomeSummary, TaxSummaryDetails}
import play.api.http.Status
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.tai.model.rti.RtiStatus
import uk.gov.hmrc.tai.util.YourIncomeCalculationHelper
import uk.gov.hmrc.tai.util.TaiConstants._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.model.{EditableDetails, TaxCodeIncomeSummary, TaxSummaryDetails}

object YourIncomeCalculationPageVM extends IncomeViewModelFactory {
  override type ViewModelType = YourIncomeCalculationViewModel

  override def createObject(nino:Nino, details: TaxSummaryDetails, incomeId : Int)(
    implicit user: TaiUser, hc: HeaderCarrier): YourIncomeCalculationViewModel = {
    val incomeExplanations = details.incomeData.map(x => x.incomeExplanations)
    val ceased = details.increasesTax.flatMap(_.incomes.flatMap(_.taxCodeIncomes.ceasedEmployments))
    val incs = ceased.map(_.taxCodeIncomes).getOrElse(List[TaxCodeIncomeSummary]())

    val incomeExplanationDetails = incomeExplanations.flatMap{ incomeExpl =>
      incomeExpl.filter(_.incomeId == incomeId).headOption.map{ expl =>
        val (incomeMsg, incomeEstimateMsg) = YourIncomeCalculationHelper.getIncomeExplanationMessage(expl)
        val payrollMsg = YourIncomeCalculationHelper.displayPayrollNumber(expl.hasDuplicateEmploymentNames, expl.worksNumber, expl.isPension)
        val isEditable = incs.filter(_.employmentId == Some(incomeId)).headOption.map(_.isEditable).getOrElse(true)

        (expl.employerName, expl.isPension, incomeMsg, incomeEstimateMsg, payrollMsg, expl.editableDetails.copy(isEditable = isEditable),
        expl.employmentStatus, expl.endDate)
      }
    }

    val (employerName, isPension, incomeMsg, incomeEstimateMsg, payrollMsg, editableDetails, employmentStatus, endDate) = incomeExplanationDetails match {
      case Some(incomeExplanationDetails) => (incomeExplanationDetails._1, incomeExplanationDetails._2,
        incomeExplanationDetails._3, incomeExplanationDetails._4, incomeExplanationDetails._5, incomeExplanationDetails._6,
        incomeExplanationDetails._7, incomeExplanationDetails._8)
      case _ => ("", false, None, None, None, EditableDetails(), None, None)
    }

    val (employmentPayments, hasPrevious, totalNotEqualMessage) = YourIncomeCalculationHelper.getCurrentYearPayments(details, incomeId)

    val rtiStatus = details.accounts.headOption.flatMap{
      accounts => accounts.rtiStatus
    }.getOrElse(RtiStatus(Status.OK, "Success"))

    val isRtiDown = rtiStatus.status match{
      case Status.INTERNAL_SERVER_ERROR => true
      case _ => false
    }

    val isCeased =  employmentStatus.fold(EmploymentLive)(status => status) == EmploymentCeased
    val isLive = employmentStatus.fold(EmploymentLive)(status => status) == EmploymentLive

    YourIncomeCalculationViewModel(
      employerName= employerName,
      isPension = isPension,
      incomeCalculationMsg = incomeMsg.getOrElse(""),
      incomeCalculationEstimateMsg = if(!isCeased) incomeEstimateMsg else None,
      payrollMsg = payrollMsg,
      employmentPayments = employmentPayments,
      empId = incomeId,
      hasPrevious = hasPrevious,
      totalNotEqualMessage = if(isLive) totalNotEqualMessage else None,
      editableDetails = editableDetails,
      rtiDown = isRtiDown,
      employmentStatus = employmentStatus,
      endDate = endDate
    )
  }
}
