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

import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain.Employment
import uk.gov.hmrc.tai.model.domain.income.TaxCodeIncome
import uk.gov.hmrc.tai.util.constants.TaiConstants._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditService @Inject() (
  @Named("appName") appName: String,
  val auditConnector: AuditConnector,
  appConfig: ApplicationConfig,
  implicit val executionContext: ExecutionContext
) {

  val userEnterEvent = "userEntersService"
  val employmentPensionEvent = "startedEmploymentPensionJourney"
  val companyBenefitsEvent = "startedCompanyBenefitJourney"
  val companyCarEvent = "startedCompanyCarJourney"
  val finishedCompanyCarEvent = "finishedCompanyCarJourney"
  val medicalBenefitsEvent = "startedMedBenJourney"
  val otherIncomeEvent = "startedOtherIncomeJourney"
  val investIncomeEvent = "startedInvestmentIncomeJourney"
  val stateBenefitEvent = "startedStateBenefitJourney"
  val marriageAllowanceEvent = "startedMarriageAllowanceJourney"

  def createAndSendAuditEvent(eventName: String, details: Map[String, String])(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[AuditResult] =
    createAndSendAuditEvent(eventName, fetchPath(request), details)

  def createAndSendAuditEvent(eventName: String, path: String, details: Map[String, String])(implicit
    hc: HeaderCarrier
  ): Future[AuditResult] =
    auditConnector.sendEvent(
      DataEvent(
        auditSource = appName,
        auditType = eventName,
        detail = details,
        tags = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails() ++
          AuditExtensions.auditHeaderCarrier(hc).toAuditTags(eventName, path)
      )
    )

  def sendUserEntryAuditEvent(
    nino: Nino,
    path: String,
    numberOfEmployments: Seq[Employment],
    numberOfTaxCodeIncomes: Seq[TaxCodeIncome],
    isJrsTileShown: Boolean
  )(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val details = Map(
      "nino"                       -> nino.nino,
      "noOfCurrentYearEmployments" -> numberOfEmployments.size.toString,
      "noOfTaxCodes"               -> numberOfTaxCodeIncomes.size.toString,
      "isJrsTileShown"             -> isJrsTileShown.toString
    )
    createAndSendAuditEvent(userEnterEvent, path, details)
  }

  def sendEndCompanyCarAuditEvent(
    nino: String,
    employmentId: String,
    carSeqNo: String,
    endDate: String,
    fuelBenefitDate: Option[String],
    isSuccessful: Boolean,
    path: String
  )(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val details = Map(
      "nino"          -> nino,
      "employmentId"  -> employmentId,
      "carSequenceNo" -> carSeqNo,
      "carEndDate"    -> endDate,
      "fuelEndDate"   -> fuelBenefitDate.getOrElse("NA"),
      "isSuccessful"  -> isSuccessful.toString
    )

    createAndSendAuditEvent(finishedCompanyCarEvent, path, details)
  }

  def sendAuditEventAndGetRedirectUri(nino: Nino, iformName: String)(implicit
    hc: HeaderCarrier,
    request: Request[AnyContent]
  ): Future[String] = {
    def sendIformRedirectUriAuditEvent(nino: Nino, path: String, auditEvent: String) = {
      val details = Map(
        "nino" -> nino.nino
      )
      createAndSendAuditEvent(auditEvent, path, details)
    }

    val (redirectUri, auditEventName) = iformName match {
      case EmployeePensionIForm     => (appConfig.incomeFromEmploymentPensionLinkUrl, employmentPensionEvent)
      case CompanyBenefitsIform     => (appConfig.companyBenefitsLinkUrl, companyBenefitsEvent)
      case CompanyCarsIform         => (appConfig.cocarFrontendUrl, companyCarEvent)
      case MedicalBenefitsIform     => (appConfig.medBenefitServiceUrl, medicalBenefitsEvent)
      case OtherIncomeIform         => (appConfig.otherIncomeLinkUrl, otherIncomeEvent)
      case InvestIncomeIform        => (appConfig.investmentIncomeLinkUrl, investIncomeEvent)
      case StateBenefitsIform       => (appConfig.taxableStateBenefitLinkUrl, stateBenefitEvent)
      case MarriageAllowanceService => (appConfig.marriageServiceHistoryUrl, marriageAllowanceEvent)
    }
    sendIformRedirectUriAuditEvent(nino, fetchPath(request), auditEventName).map { _ =>
      redirectUri
    }
  }

  private def fetchPath(request: Request[AnyContent]) =
    request.headers.get("Referer").getOrElse("NA")

}
