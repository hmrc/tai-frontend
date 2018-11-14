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

package uk.gov.hmrc.tai.viewModels

import controllers.FakeTaiPlayApplication
import org.scalatestplus.play.PlaySpec
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.tai.config.ApplicationConfig
import uk.gov.hmrc.tai.model.domain._
import uk.gov.hmrc.tai.model.domain.benefits.CompanyCarBenefit
import uk.gov.hmrc.tai.model.domain.calculation.CodingComponent
import uk.gov.hmrc.tai.util.HtmlFormatter
import uk.gov.hmrc.tai.util.constants.TaiConstants
import uk.gov.hmrc.tai.util.constants.TaiConstants.encodedMinusSign
import uk.gov.hmrc.time.TaxYearResolver

class TaxFreeAmountViewModelSpec extends PlaySpec with FakeTaiPlayApplication with I18nSupport {
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "apply method" must {

    "return a view model" which {

      "has header relating to current tax year" in {
        val sut = TaxFreeAmountViewModel(codingComponents, employmentNames, companyCarBenefits)
        sut.header mustBe expectedHeader
      }

      "has formatted positive tax free amount" when {
        "calculated TaxFreeAmount is positive" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
            CodingComponent(EmployerProvidedServices, Some(12), 1000, "Benefit"),
            CodingComponent(ForeignDividendIncome, Some(12), 300, "Income"),
            CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))
          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)

          sut.annualTaxFreeAmount mustBe "£10,000"
        }
        "calculated TaxFreeAmount is positive and two Personal allowances are present" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
            CodingComponent(PersonalAllowanceAgedPAA, Some(234), 1000, "Personal Allowance"),
            CodingComponent(EmployerProvidedServices, Some(12), 1000, "Benefit"),
            CodingComponent(ForeignDividendIncome, Some(12), 300, "Income"),
            CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))
          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)

          sut.annualTaxFreeAmount mustBe "£11,000"
        }
        "calculated TaxFreeAmount is positive and all Personal allowances are present" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
            CodingComponent(PersonalAllowanceAgedPAA, Some(234), 1000, "Personal Allowance"),
            CodingComponent(PersonalAllowanceElderlyPAE, Some(234), 2000, "Personal Allowance"),
            CodingComponent(EmployerProvidedServices, Some(12), 1000, "Benefit"),
            CodingComponent(ForeignDividendIncome, Some(12), 300, "Income"),
            CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))
          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)

          sut.annualTaxFreeAmount mustBe "£13,000"
        }
      }

      "has formatted negative tax free amount" when {
        "calculated TaxFreeAmount is negative" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 100, "Personal Allowance"),
            CodingComponent(EmployerProvidedServices, Some(12), 100, "Benefit"),
            CodingComponent(ForeignDividendIncome, Some(12), 1000, "Income"),
            CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))
          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)

          sut.annualTaxFreeAmount mustBe s"${encodedMinusSign}£1,200"
        }
      }

      "has all the rows with empty values" when {
        "there is no tax coding" in {
          val taxComponents = emptyCodingComponents

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          sut mustBe TaxFreeAmountViewModel(
            expectedHeader,
            expectedTitle,
            "£0",
            TaxFreeAmountSummaryViewModel(Seq(
              emptyPersonalAllowanceItem,
              emptyAdditionsItem,
              emptyDeductionsItem,
              emptyTotalsItem)))
        }
      }

      "has empty additions values" when {
        "there is no tax component and only Personal Allowances" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
            CodingComponent(PersonalAllowanceAgedPAA, Some(234), 1000, "Personal Allowance"),
            CodingComponent(PersonalAllowanceElderlyPAE, Some(234), 2000, "Personal Allowance"))

          val result = Seq(
            TaxFreeAmountSummaryRowViewModel(
              Messages("tai.taxFreeAmount.table.additions.noAddition"),
              "£0",
              ChangeLinkViewModel(isDisplayed = false)
            ))

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          val additionRows = sut.taxFreeAmountSummary.summaryItems(1).rows
          additionRows mustBe result
        }
      }

      "has personal allowance row with correct values" when {
        "there is only personal allowance" in {
          val taxComponents = Seq(CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"))

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          sut mustBe TaxFreeAmountViewModel(
            expectedHeader,
            expectedTitle,
            "£11,500",
            TaxFreeAmountSummaryViewModel(Seq(
              TaxFreeAmountSummaryCategoryViewModel(
                Messages("tai.taxFreeAmount.table.columnOneHeader"),
                Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                hideHeaders = false,
                hideCaption = true,
                Messages("tai.taxFreeAmount.table.allowances.caption"),
                Seq(
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""),
                    "£11,500",
                    ChangeLinkViewModel(isDisplayed = false)
                  ))),
              emptyAdditionsItem,
              emptyDeductionsItem,
              totalsItem("£11,500")
            )))
        }
      }

      "has additions rows with correct values" when {
        "there are only additions" in {

          val taxFreeAllowanceLinkUrl = ApplicationConfig.taxFreeAllowanceLinkUrl

          val taxComponents = Seq(
            CodingComponent(GiftAidPayments, Some(234), 100, "GiftAidPayments"),
            CodingComponent(GiftsSharesCharity, Some(234), 200, "GiftsSharesCharity"),
            CodingComponent(PersonalPensionPayments, Some(234), 1000, "PersonalPensionPayments"))

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          sut mustBe TaxFreeAmountViewModel(
            expectedHeader,
            expectedTitle,
            "£1,300",
            TaxFreeAmountSummaryViewModel(Seq(
              emptyPersonalAllowanceItem,
              TaxFreeAmountSummaryCategoryViewModel(
                Messages("tai.taxFreeAmount.table.columnOneHeader"),
                Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                hideHeaders = true,
                hideCaption = false,
                Messages("tai.taxFreeAmount.table.additions.caption"),
                Seq(
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.taxComponent.GiftAidPayments"),
                    "£100",
                    ChangeLinkViewModel(
                      isDisplayed = true,
                      value =  Messages("tai.taxFreeAmount.table.taxComponent.GiftAidPayments"),
                      href = taxFreeAllowanceLinkUrl)
                  ),
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.taxComponent.GiftsSharesCharity"),
                    "£200",
                    ChangeLinkViewModel(
                      isDisplayed = true,
                      value =  Messages("tai.taxFreeAmount.table.taxComponent.GiftsSharesCharity"),
                      href = taxFreeAllowanceLinkUrl)
                  ),
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.taxComponent.PersonalPensionPayments"),
                    "£1,000",
                    ChangeLinkViewModel(
                      isDisplayed = true,
                      value =  Messages("tai.taxFreeAmount.table.taxComponent.PersonalPensionPayments"),
                      href = taxFreeAllowanceLinkUrl)
                  ),
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.additions.total"),
                    "£1,300",
                    ChangeLinkViewModel(isDisplayed = false)
                  ))),
              emptyDeductionsItem,
              totalsItem("£1,300")
            )
          ))
        }
      }

      "has deductions rows with correct values" when {
        "there are only deductions" in {
          val taxComponents = Seq(
            CodingComponent(MedicalInsurance, Some(234), 200, "MedicalInsurance"),
            CodingComponent(ChildBenefit, None, 1000, "ChildBenefit"),
            CodingComponent(OtherEarnings, Some(234), 5000, "OtherEarnings"))

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          sut mustBe TaxFreeAmountViewModel(
            expectedHeader,
            expectedTitle,
            s"${encodedMinusSign}£6,200",
            TaxFreeAmountSummaryViewModel(Seq(
              emptyPersonalAllowanceItem,
              emptyAdditionsItem,
              TaxFreeAmountSummaryCategoryViewModel(
                Messages("tai.taxFreeAmount.table.columnOneHeader"),
                Messages("tai.taxFreeAmount.table.columnTwoHeader"),
                hideHeaders = true,
                hideCaption = false,
                Messages("tai.taxFreeAmount.table.deductions.caption"),
                Seq(
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"),
                    "£200",
                    ChangeLinkViewModel(
                      isDisplayed = true,
                      value = Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"),
                      href = controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform).url)
                  ),
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.taxComponent.ChildBenefit"),
                    "£1,000",
                    ChangeLinkViewModel(isDisplayed = false)
                  ),
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.taxComponent.OtherEarnings"),
                    "£5,000",
                    ChangeLinkViewModel(isDisplayed = false)
                  ),
                  TaxFreeAmountSummaryRowViewModel(
                    Messages("tai.taxFreeAmount.table.deductions.total"),
                    "£6,200",
                    ChangeLinkViewModel(isDisplayed = false)
                  ))),
              totalsItem(s"${encodedMinusSign}£6,200")
            )
          ))
        }
      }

      "has total row with positive value" when {
        "there are list of all the components with a positive total" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
            CodingComponent(GiftAidPayments, Some(234), 512, "GiftAidPayments"),
            CodingComponent(GiftsSharesCharity, Some(234), 1000, "TotalGiftAidPayments"),
            CodingComponent(MedicalInsurance, Some(234), 2000, "MedicalInsurance"),
            CodingComponent(ChildBenefit, None, 3000, "ChildBenefit"),
            CodingComponent(OtherEarnings, Some(234), 4000, "OtherEarnings")
          )

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          sut.taxFreeAmountSummary.summaryItems.last mustBe
            TaxFreeAmountSummaryCategoryViewModel(
              Messages("tai.taxFreeAmount.table.columnOneHeader"),
              Messages("tai.taxFreeAmount.table.columnTwoHeader"),
              hideHeaders = true,
              hideCaption = true,
              Messages("tai.taxFreeAmount.table.totals.caption"),
              Seq(
                TaxFreeAmountSummaryRowViewModel(
                  Messages("tai.taxFreeAmount.table.totals.label"),
                  "£4,012",
                  ChangeLinkViewModel(isDisplayed = false)
                )))
        }
      }

      "has total row with negative value" when {
        "there are list of all the components with a negative total" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 5500, "Personal Allowance"),
            CodingComponent(GiftAidPayments, Some(234), 512, "GiftAidPayments"),
            CodingComponent(GiftsSharesCharity, Some(234), 1000, "TotalGiftAidPayments"),
            CodingComponent(MedicalInsurance, Some(234), 2000, "MedicalInsurance"),
            CodingComponent(ChildBenefit, None, 3000, "ChildBenefit"),
            CodingComponent(OtherEarnings, Some(234), 4000, "OtherEarnings")
          )

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          sut.taxFreeAmountSummary.summaryItems.last mustBe
            TaxFreeAmountSummaryCategoryViewModel(
              Messages("tai.taxFreeAmount.table.columnOneHeader"),
              Messages("tai.taxFreeAmount.table.columnTwoHeader"),
              hideHeaders = true,
              hideCaption = true,
              Messages("tai.taxFreeAmount.table.totals.caption"),
              Seq(
                TaxFreeAmountSummaryRowViewModel(
                  Messages("tai.taxFreeAmount.table.totals.label"),
                  s"${encodedMinusSign}£1,988",
                  ChangeLinkViewModel(isDisplayed = false)
                )))
        }
      }

      "has total row with zero value" when {
        "there are list of all the components with a zero total" in {
          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 5500, "Personal Allowance"),
            CodingComponent(GiftAidPayments, Some(234), 512, "GiftAidPayments"),
            CodingComponent(GiftsSharesCharity, Some(234), 1000, "TotalGiftAidPayments"),
            CodingComponent(MedicalInsurance, Some(234), 1000, "MedicalInsurance"),
            CodingComponent(ChildBenefit, None, 5500, "ChildBenefit"),
            CodingComponent(OtherEarnings, Some(234), 512, "OtherEarnings")
          )

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)
          sut.taxFreeAmountSummary.summaryItems.last mustBe
            TaxFreeAmountSummaryCategoryViewModel(
              Messages("tai.taxFreeAmount.table.columnOneHeader"),
              Messages("tai.taxFreeAmount.table.columnTwoHeader"),
              hideHeaders = true,
              hideCaption = true,
              Messages("tai.taxFreeAmount.table.totals.caption"),
              Seq(
                TaxFreeAmountSummaryRowViewModel(
                  Messages("tai.taxFreeAmount.table.totals.label"),
                  "£0",
                  ChangeLinkViewModel(isDisplayed = false)
                )))
        }
      }

      "correct rows" when {
        "there is a list of all components provided" in {

          val taxFreeAllowanceLinkUrl = ApplicationConfig.taxFreeAllowanceLinkUrl

          val taxComponents = Seq(
            CodingComponent(PersonalAllowancePA, Some(234), 7500, "Personal Allowance"),
            CodingComponent(GiftAidPayments, Some(234), 512, "GiftAidPayments"),
            CodingComponent(GiftsSharesCharity, Some(234), 1000, "GiftsSharesCharity"),
            CodingComponent(MedicalInsurance, Some(234), 1000, "MedicalInsurance"),
            CodingComponent(ChildBenefit, None, 5500, "ChildBenefit"),
            CodingComponent(OtherEarnings, Some(234), 512, "OtherEarnings")
          )

          val sut = TaxFreeAmountViewModel(taxComponents, employmentNames, companyCarBenefits)

          val expectedPersonalAllowanceItem = TaxFreeAmountSummaryCategoryViewModel(
            Messages("tai.taxFreeAmount.table.columnOneHeader"),
            Messages("tai.taxFreeAmount.table.columnTwoHeader"),
            hideHeaders = false,
            hideCaption = true,
            Messages("tai.taxFreeAmount.table.allowances.caption"),
            Seq(
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""),
                "£7,500",
                ChangeLinkViewModel(isDisplayed = false)
              )))

          val expectedAdditionsItem = TaxFreeAmountSummaryCategoryViewModel(
            Messages("tai.taxFreeAmount.table.columnOneHeader"),
            Messages("tai.taxFreeAmount.table.columnTwoHeader"),
            hideHeaders = true,
            hideCaption = false,
            Messages("tai.taxFreeAmount.table.additions.caption"),
            Seq(
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.taxComponent.GiftAidPayments"),
                "£512",
                ChangeLinkViewModel(
                  isDisplayed = true,
                  value =  Messages("tai.taxFreeAmount.table.taxComponent.GiftAidPayments"),
                  href = taxFreeAllowanceLinkUrl)
              ),
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.taxComponent.GiftsSharesCharity"),
                "£1,000",
                ChangeLinkViewModel(
                  isDisplayed = true,
                  value =  Messages("tai.taxFreeAmount.table.taxComponent.GiftsSharesCharity"),
                  href = taxFreeAllowanceLinkUrl)
              ),
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.additions.total"),
                "£1,512",
                ChangeLinkViewModel(isDisplayed = false)
              )))

          val expectedDeductionsItem = TaxFreeAmountSummaryCategoryViewModel(
            Messages("tai.taxFreeAmount.table.columnOneHeader"),
            Messages("tai.taxFreeAmount.table.columnTwoHeader"),
            hideHeaders = true,
            hideCaption = false,
            Messages("tai.taxFreeAmount.table.deductions.caption"),
            Seq(
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"),
                "£1,000",
                ChangeLinkViewModel(
                  isDisplayed = true,
                  value = Messages("tai.taxFreeAmount.table.taxComponent.MedicalInsurance"),
                  href = controllers.routes.ExternalServiceRedirectController.auditInvalidateCacheAndRedirectService(TaiConstants.MedicalBenefitsIform).url)
              ),
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.taxComponent.ChildBenefit"),
                "£5,500",
                ChangeLinkViewModel(isDisplayed = false)
              ),
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.taxComponent.OtherEarnings"),
                "£512",
                ChangeLinkViewModel(isDisplayed = false)
              ),
              TaxFreeAmountSummaryRowViewModel(
                Messages("tai.taxFreeAmount.table.deductions.total"),
                "£7,012",
                ChangeLinkViewModel(isDisplayed = false)
              )))


          val expectedTotalsItem = totalsItem("£2,000")

          sut.taxFreeAmountSummary.summaryItems(0) mustBe expectedPersonalAllowanceItem
          sut.taxFreeAmountSummary.summaryItems(1) mustBe expectedAdditionsItem
          sut.taxFreeAmountSummary.summaryItems(2) mustBe expectedDeductionsItem
          sut.taxFreeAmountSummary.summaryItems(3) mustBe expectedTotalsItem
        }
      }

    }
  }

  val codingComponents = Seq(
    CodingComponent(PersonalAllowancePA, Some(234), 11500, "Personal Allowance"),
    CodingComponent(EmployerProvidedServices, Some(12), 100, "Benefit"),
    CodingComponent(ForeignDividendIncome, Some(12), 1000, "Income"),
    CodingComponent(MarriageAllowanceTransferred, Some(31), 200, "Deduction"))

  val emptyPersonalAllowanceItem = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = false,
    hideCaption = true,
    Messages("tai.taxFreeAmount.table.allowances.caption"),
    Seq(
      TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.taxComponent.PersonalAllowancePA").replace(" (PA)", ""),
        "£0",
        ChangeLinkViewModel(isDisplayed = false)
      )))

  val emptyAdditionsItem = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = false,
    Messages("tai.taxFreeAmount.table.additions.caption"),
    Seq(
      TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.additions.noAddition"),
        "£0",
        ChangeLinkViewModel(isDisplayed = false)
      )))

  val emptyDeductionsItem = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = false,
    Messages("tai.taxFreeAmount.table.deductions.caption"),
    Seq(
      TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.deductions.noDeduction"),
        "£0",
        ChangeLinkViewModel(isDisplayed = false)
      )))

  val emptyTotalsItem = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = true,
    Messages("tai.taxFreeAmount.table.totals.caption"),
    Seq(
      TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.totals.label"),
        "£0",
        ChangeLinkViewModel(isDisplayed = false)
      )))

  def totalsItem(formattedTotal: String) = TaxFreeAmountSummaryCategoryViewModel(
    Messages("tai.taxFreeAmount.table.columnOneHeader"),
    Messages("tai.taxFreeAmount.table.columnTwoHeader"),
    hideHeaders = true,
    hideCaption = true,
    Messages("tai.taxFreeAmount.table.totals.caption"),
    Seq(
      TaxFreeAmountSummaryRowViewModel(
        Messages("tai.taxFreeAmount.table.totals.label"),
        formattedTotal,
        ChangeLinkViewModel(isDisplayed = false)
      )))

  val emptyCodingComponents = Seq.empty[CodingComponent]

  val expectedHeader, expectedTitle = Messages("tai.taxFreeAmount.heading.pt1") + " " +
    Messages("tai.taxYear",
      HtmlFormatter.htmlNonBroken(TaxYearResolver.startOfCurrentTaxYear.toString("d MMMM yyyy")),
      HtmlFormatter.htmlNonBroken(TaxYearResolver.endOfCurrentTaxYear.toString("d MMMM yyyy")))

  val employmentNames = Map.empty[Int, String]
  val companyCarBenefits = Seq.empty[CompanyCarBenefit]
}

