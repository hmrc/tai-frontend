package uk.gov.hmrc.tai.service

import controllers.auth.TaiUser
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.tai.model.TaxYear
import uk.gov.hmrc.tai.util.constants.journeyCache.UpdateNextYearsIncomeConstants

import scala.concurrent.Future

final case class UpdateNextYearsIncomeCacheModel(employmentName: String, employmentId: Int, currentValue: BigDecimal, newValue: Option[Int]) {
  def toCacheMap: Map[String, String] = {
    if (newValue.isDefined) {
      Map(
        UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME -> employmentName,
        UpdateNextYearsIncomeConstants.EMPLOYMENT_ID -> employmentId.toString,
        UpdateNextYearsIncomeConstants.CURRENT_AMOUNT -> currentValue.toString,
        UpdateNextYearsIncomeConstants.NEW_AMOUNT -> newValue.toString
      )
    } else {
      Map(
        UpdateNextYearsIncomeConstants.EMPLOYMENT_NAME -> employmentName,
        UpdateNextYearsIncomeConstants.EMPLOYMENT_ID -> employmentId.toString,
        UpdateNextYearsIncomeConstants.CURRENT_AMOUNT -> currentValue.toString
      )
    }
  }
}

class UpdateNextYearsIncomeService(implicit hc: HeaderCarrier) {

  lazy val journeyCacheService: JourneyCacheService = JourneyCacheService(UpdateNextYearsIncomeConstants.JOURNEY_KEY)
  lazy val employmentService: EmploymentService = EmploymentService
  lazy val taxAccountService: TaxAccountService = TaxAccountService

  def setup(employmentId: Int)(implicit user: TaiUser): Future[UpdateNextYearsIncomeCacheModel] = {

    val taxCodeIncomeFuture = taxAccountService.taxCodeIncomeForEmployment(Nino(user.getNino), TaxYear(), employmentId)
    val employmentFuture = employmentService.employment(Nino(user.getNino), employmentId)


    for {
      taxCodeIncomeOption <- taxCodeIncomeFuture
      employmentOption <- employmentFuture
    } yield (taxCodeIncomeOption, employmentOption) match {
      case (Some(taxCodeIncome), Some(employment)) => {
        val model = UpdateNextYearsIncomeCacheModel(employment.name, employmentId, taxCodeIncome.amount, None)

        journeyCacheService.cache(model.toCacheMap)

        model
      }
    }
  }

  def get: UpdateNextYearsIncomeCacheModel = {

  }

}
