package uk.gov.hmrc.tai.service

import com.github.tomakehurst.wiremock.client.WireMock.{get, ok, urlEqualTo}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.model.TaxYear
import utils.WireMockHelper

import scala.util.Random

class UpdateNextYearsIncomeServiceSpec extends PlaySpec
  with WireMockHelper
  with MockitoSugar {

  "setup" must {
    "initialize the journey cache" in {

      val nino = generateNino
      val id = 1

      val taxAccountServiceUrl = s"/tai/$nino/tax-account/${TaxYear()}/income/tax-code-incomes"
      val employmentUrl = s"/tai/$nino/employments/$id"

      server.stubFor(
        get(urlEqualTo(taxAccountServiceUrl)).willReturn(ok(taxCodeIncomeJson.toString()))
      )

      server.stubFor(
        get(urlEqualTo(employmentUrl)).willReturn(ok(employmentJson.toString()))
      )
    }
  }

  private val employmentJson: JsValue = {
    Json.obj(
      "data" -> Json.obj(
        "name" -> "EmploymentName",
        "startDate" -> "GET THE YEAR",
        "annualAccounts" -> Json.arr(),
        "taxDistrictNumber" -> "123",
        "payeNumber" -> "Paye Number",
        "sequenceNumber" -> 1,
        "hasPayrolledBenefit" -> false,
        "receivingOccupationalPension" -> false
      )
    )
  }

  private val taxCodeIncomeJson: JsValue = {
    Json.obj(
      "data" -> Json.obj(
        "componentType" -> "EmploymentIncome",
        "employmentId" -> 1,
        "amount" -> "10000",
        "description" -> "description",
        "taxCode" -> "1185L",
        "name" -> "EmploymentName",
        "basisOperation" -> "Cumulative",
        "status" -> "Live"
      )
    )
  }

  private def generateNino: Nino = new Generator(new Random).nextNino

  val updateNextYearsIncomeService = new UpdateNextYearsIncomeService
//  private class updateNextYearsIncomeService extends UpdateNextYearsIncomeService {
//    override val journeyCacheService: JourneyCacheService = mock[JourneyCacheService]
//    override val employmentService: EmploymentService = mock[EmploymentService]
//    override val taxAccountService: TaxAccountService = mock[TaxAccountService]
//  }
}
