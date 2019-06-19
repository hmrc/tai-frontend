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

package uk.gov.hmrc.tai.service.benefits

import javax.inject.{Inject, Named}
import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.CompanyCarConnector
import uk.gov.hmrc.tai.connectors.responses.{TaiNoCompanyCarFoundResponse, TaiResponse, TaiSuccessResponse, TaiSuccessResponseWithPayload}
import uk.gov.hmrc.tai.model.domain.benefits.{CompanyCarBenefit, WithdrawCarAndFuel}
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.model.domain.{CarBenefit, Employment}
import uk.gov.hmrc.tai.service.journeyCache.JourneyCacheService
import uk.gov.hmrc.tai.service.{AuditService, EmploymentService}
import uk.gov.hmrc.tai.util.constants.JourneyCacheConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CompanyCarService @Inject() (carConnector: CompanyCarConnector,
                                   employmentService: EmploymentService,
                                   auditService: AuditService,
                                   @Named("Company Car") journeyCacheService: JourneyCacheService) extends JourneyCacheConstants {

  def companyCarOnCodingComponents(nino: Nino, codingComponents: Seq[CodingComponent])(implicit hc: HeaderCarrier): Future[Seq[CompanyCarBenefit]] = {
    if (codingComponents.exists(_.componentType == CarBenefit))
      companyCars(nino)
    else
      Future.successful(Seq.empty[CompanyCarBenefit])
  }

  def companyCars(nino: Nino)(implicit hc: HeaderCarrier): Future[Seq[CompanyCarBenefit]] = {
    carConnector.companyCarsForCurrentYearEmployments(nino).map(_.filterNot(isCompanyCarDateWithdrawn))
  }

  def isCompanyCarDateWithdrawn(companyCarBenefit: CompanyCarBenefit): Boolean = {
    companyCarBenefit.companyCars.exists(_.dateWithdrawn.isDefined)
  }

  private def cacheFromCompanyCar(employmentOpt: Option[Employment], ccBen: CompanyCarBenefit, empId: Int): Option[Map[String, String]] = {
    ccBen.companyCars.headOption flatMap { car =>
      val employment = employmentOpt.getOrElse(
        throw new RuntimeException(s"No employment record was located the supplied employment id $empId")
      )
      val optionalStart = car.dateMadeAvailable map { dt => CompanyCar_DateStartedKey -> dt.toString }
      val optionalFuelStart = car.dateActiveFuelBenefitMadeAvailable map { dt => CompanyCar_DateFuelBenefitStartedKey -> dt.toString }
      Some((Seq(
        CompanyCar_Version -> ccBen.version.getOrElse(0).toString,
        CompanyCar_CarModelKey -> car.makeModel,
        CompanyCar_CarProviderKey -> employment.name,
        CompanyCar_CarSeqNoKey -> car.carSeqNo.toString,
        CompanyCar_HasActiveFuelBenefitdKey -> car.hasActiveFuelBenefit.toString
      ) ++ optionalStart ++ optionalFuelStart).toMap[String, String])
    }

  }

  def beginJourney(nino: Nino, empId: Int)(implicit hc: HeaderCarrier): Future[TaiResponse] = {

    val journeyCache: Future[Option[Map[String, String]]] = for {
      employmentOpt <- employmentService.employment(nino, empId)
      ccBenOption <- carConnector.companyCarBenefitForEmployment(nino, empId)
    } yield {
      ccBenOption match {
        case Some(ccBen) if !isCompanyCarDateWithdrawn(ccBen) => cacheFromCompanyCar(employmentOpt, ccBen, empId)
        case _ => None
      }
    }

    journeyCache.flatMap {
      case None => Future.successful(TaiNoCompanyCarFoundResponse("No company car found"))
      case Some(cache) => journeyCacheService.cache(cache).map(x => TaiSuccessResponseWithPayload(x))
    }
  }

  def companyCarEmploymentId(implicit hc: HeaderCarrier): Future[Int] = {
    journeyCacheService.mandatoryValueAsInt(CompanyCar_EmployerIdKey)
  }

  def withdrawCompanyCarAndFuel(nino: Nino, referer: String)(implicit hc: HeaderCarrier): Future[TaiResponse] = {

    journeyCacheService.currentCache.flatMap { cache =>

      val taiResponse: Option[Future[TaiResponse]] = for {
        employmentSeqNum <- cache.get(CompanyCar_EmployerIdKey)
        carSeqNum <- cache.get(CompanyCar_CarSeqNoKey)
        version <- cache.get(CompanyCar_Version)
        carEndDate <- cache.get(CompanyCar_DateGivenBackKey)
        fuelDate = cache.get(CompanyCar_DateFuelBenefitStoppedKey)
      } yield {
        carConnector.withdrawCompanyCarAndFuel(nino, employmentSeqNum.toInt, carSeqNum.toInt,
          WithdrawCarAndFuel(version.toInt, new LocalDate(carEndDate), fuelDate.map(x => new LocalDate(x)))).map { r =>
          val isSuccess = r match {
            case TaiSuccessResponse => true
            case _ => false
          }
          auditService.sendEndCompanyCarAuditEvent(nino.toString(), employmentSeqNum, carSeqNum, carEndDate,
            fuelDate, isSuccess, referer)
          r
        }
      }

      taiResponse.getOrElse(throw new RuntimeException("Empty value in Company Car cache"))
    }
  }
}
