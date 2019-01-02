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

package uk.gov.hmrc.tai.viewModels.taxCodeChange

import controllers.FakeTaiPlayApplication
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.play.language.LanguageUtils.Dates
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.constants.TaiConstants.encodedMinusSign
import uk.gov.hmrc.tai.util.ViewModelHelper
import uk.gov.hmrc.time.TaxYearResolver

import scala.util.Random

/**
  * Created by digital032748 on 25/07/18.
  */
class YourTaxFreeAmountViewModelSpec extends PlaySpec with FakeTaiPlayApplication with ViewModelHelper with I18nSupport {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private def generateNino: Nino = new Generator(new Random).nextNino

  private val nino = generateNino
  private val defaultP2Date = new LocalDate()
  private val defaultCodingComponents = Seq.empty[CodingComponent]
  private val defaultEmploymentName : Map[Int, String] = Map(0 -> "")
  private val defaultCompanyCarBenefits : Seq[CompanyCarBenefit] = Seq.empty[CompanyCarBenefit]

  private def createViewModel(p2Date: LocalDate = defaultP2Date,
                              codingComponents: Seq[CodingComponent] = defaultCodingComponents,
                              employmentName: Map[Int, String] = defaultEmploymentName,
                              companyCarBenefits: Seq[CompanyCarBenefit] = defaultCompanyCarBenefits) : YourTaxFreeAmountViewModel = {
    YourTaxFreeAmountViewModel(p2Date, codingComponents, employmentName, companyCarBenefits)
  }

  "YourTaxFreeAmountViewModel" must {

    "return the P2 Issued Date in a date range" in {
      val viewModel = createViewModel()

      val expectedDateRange = messagesApi("tai.taxYear",htmlNonBroken(Dates.formatDate(defaultP2Date)),
      htmlNonBroken(Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)))

      viewModel.taxCodeDateRange mustBe expectedDateRange

    }

    "has formatted positive tax free amount" when {
      "calculated TaxFreeAmount is positive" in {
        val taxComponents = Seq(
          CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
          CodingComponent(EmployerProvidedServices, Some(12), 1000, "Benefit"),
          CodingComponent(ForeignDividendIncome, Some(12), 300, "Income"),
          CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))

        val viewModel = createViewModel(codingComponents = taxComponents)

        viewModel.annualTaxFreeAmount mustBe "£10,000"
      }
      "calculated TaxFreeAmount is positive and two Personal allowances are present" in {
        val taxComponents = Seq(
          CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
          CodingComponent(PersonalAllowanceAgedPAA, Some(234), 1000, "Personal Allowance"),
          CodingComponent(EmployerProvidedServices, Some(12), 1000, "Benefit"),
          CodingComponent(ForeignDividendIncome, Some(12), 300, "Income"),
          CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))

        val viewModel = createViewModel(codingComponents = taxComponents)

        viewModel.annualTaxFreeAmount mustBe "£11,000"
      }
      "calculated TaxFreeAmount is positive and all Personal allowances are present" in {
        val taxComponents = Seq(
          CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
          CodingComponent(PersonalAllowanceAgedPAA, Some(234), 1000, "Personal Allowance"),
          CodingComponent(PersonalAllowanceElderlyPAE, Some(234), 2000, "Personal Allowance"),
          CodingComponent(EmployerProvidedServices, Some(12), 1000, "Benefit"),
          CodingComponent(ForeignDividendIncome, Some(12), 300, "Income"),
          CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))

        val viewModel = createViewModel(codingComponents = taxComponents)

        viewModel.annualTaxFreeAmount mustBe "£13,000"
      }
    }

    "has formatted negative tax free amount" when {
      "calculated TaxFreeAmount is negative" in {
        val taxComponents = Seq(
          CodingComponent(PersonalAllowancePA, Some(234), 100, "Personal Allowance"),
          CodingComponent(EmployerProvidedServices, Some(12), 100, "Benefit"),
          CodingComponent(ForeignDividendIncome, Some(12), 1000, "Income"),
          CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))

        val viewModel = createViewModel(codingComponents = taxComponents)

        viewModel.annualTaxFreeAmount mustBe s"${encodedMinusSign}£1,200"
      }
    }

  }

}
