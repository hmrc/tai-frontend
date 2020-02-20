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

package uk.gov.hmrc.tai.config

import play.api.Play._
import uk.gov.hmrc.tai.model.TaxYear
import views.html.helper

class ApplicationConfig extends DefaultServicesConfig {

  def statusRange = s"${TaxYear().prev.year}-${TaxYear().year}"

  lazy val citizenAuthHost = fetchUrl("citizen-auth")
  lazy val companyAuthUrl = fetchUrl("company-auth")
  lazy val urlEncode = helper.urlEncode(_: String)
  lazy val incomeTaxFormPartialLinkUrl =
    s"${fetchUrl("iform-partial")}/digital-forms/forms/personal-tax/income-tax/catalogue"
  lazy val incomeFromEmploymentPensionPartialLinkUrl =
    s"${fetchUrl("iform-partial")}/digital-forms/form/tell-us-about-income-from-employment-or-pension/draft/guide"
  lazy val incomeFromEmploymentPensionLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/form/tell-us-about-income-from-employment-or-pension/draft/guide"
  lazy val companyBenefitsLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/form/tell-us-about-company-benefits/draft/guide"
  lazy val taxableStateBenefitLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/form/tell-us-about-your-taxable-state-benefit/draft/guide"
  lazy val otherIncomeLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/form/tell-us-about-other-income/draft/guide"
  lazy val investmentIncomeLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/form/tell-us-about-investment-income/draft/guide"
  lazy val taxFreeAllowanceLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/form/check-income-tax-tell-us-your-tax-free-allowance/draft/guide"
  lazy val reportAProblemPartialUrl = s"${fetchUrl("contact-frontend")}/contact/problem_reports?secure=true&service=TAI"
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  lazy val urBannerEnabled = configuration.getString("feature.ur-banner.enabled").getOrElse("true").toBoolean
  lazy val urBannerLink = configuration.getString("ur-banner.url").getOrElse("")
  lazy val checkUpdateProgressLinkUrl = s"${fetchUrl("track")}/track"

  lazy val analyticsToken: Option[String] = configuration.getString(s"govuk-tax.$env.google-analytics.token")
  lazy val gaValueOfPayments: String =
    configuration.getString(s"govuk-tax.$env.google-analytics.gaValueOfPayments").getOrElse("")
  lazy val gaRecStatus: String = configuration.getString(s"govuk-tax.$env.google-analytics.gaRecStatus").getOrElse("")
  lazy val analyticsHost: String = configuration.getString(s"govuk-tax.$env.google-analytics.host").getOrElse("auto")
  lazy val pertaxServiceUrl = s"${fetchUrl("pertax-frontend")}/personal-account"
  lazy val pertaxServiceUpliftFailedUrl = s"${fetchUrl("pertax-frontend")}/personal-account/identity-check-failed"
  lazy val pertaxExitSurveyUrl = s"$pertaxServiceUrl/signout?origin=TES"
  lazy val feedbackSurveyUrl = s"$feedbackHost/feedback/TES"
  lazy val feedbackHost =
    configuration.getString(s"govuk-tax.$env.external-url.feedback-survey-frontend.host").getOrElse("")
  lazy val cocarFrontendUrl = s"${fetchUrl("cocar-frontend")}/paye/company-car/details"
  lazy val updateCompanyCarDetailsUrl = s"$personServiceUrl/redirect-company-car"
  lazy val personServiceUrl = s"${fetchUrl("tai-frontend")}/check-income-tax"
  lazy val marriageServiceUrl = s"${fetchUrl("tamc-frontend")}/marriage-allowance-application/history"
  lazy val marriageServiceHistoryUrl = s"${fetchUrl("tamc-frontend")}/marriage-allowance-application/history"
  lazy val medBenefitServiceUrl = s"${fetchUrl("benefits-frontend")}/paye/benefits/medical-benefit"
  lazy val mainContentHeaderPartialUrl =
    s"${fetchUrl("header-service")}/personal-account/integration/main-content-header"
  lazy val sa16UpliftUrl = s"${fetchUrl("identity-verification-frontend")}/mdtp/uplift"
  lazy val taiFrontendServiceUrl = s"$personServiceUrl/income-tax"
  lazy val taxYouPaidStatus = s"${fetchUrl("taxcalc-frontend")}/tax-you-paid/status"
  lazy val gg_web_context = configuration.getString(s"$env.external-url.gg.web-context").getOrElse("gg")
  lazy val ida_web_context = configuration.getString(s"$env.external-url.ida.web-context").getOrElse("ida")
  lazy val hardshipHelpBase = configuration.getString(s"govuk-tax.$env.external-url.hardship-help.host").getOrElse("")
  lazy val hardshipHelpUrl =
    s"$hardshipHelpBase/digital-forms/form/tell-us-how-you-want-to-pay-estimated-tax/draft/guide"
  private val contactHost = configuration.getString(s"govuk-tax.$env.services.contact-frontend.host").getOrElse("")
  lazy val companyAuthFrontendSignOutUrl = s"$companyAuthUrl/gg/sign-out?continue=$feedbackSurveyUrl"
  lazy val citizenAuthFrontendSignOutUrl = citizenAuthHost + "/ida/signout"
  lazy val assetsPath =
    s"${configuration.getString(s"$env.assets.url").getOrElse("")}${configuration.getString(s"$env.assets.version").getOrElse("")}/"
  lazy val scottishRateIncomeTaxUrl: String = "https://www.gov.uk/scottish-rate-income-tax"
  lazy val welshRateIncomeTaxUrl: String = "https://www.gov.uk/welsh-income-tax"
  lazy val welshRateIncomeTaxWelshUrl: String = "https://www.gov.uk/treth-incwm-cymru"
  lazy val contactHelplineUrl: String =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
  lazy val contactHelplineWelshUrl: String =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/welsh-language-helplines"

  lazy val frontendTemplatePath: String =
    configuration.getString(s"govuk-tax.$env.services.frontend-template-provider.path").getOrElse("/template/mustache")

  def fetchUrl(service: String) =
    try {
      baseUrl(service)
    } catch {
      case ex: RuntimeException => ""
      case _: Throwable         => s"Unknown Exception: $service-url"
    }

  lazy val isTaiCy3Enabled = configuration.getBoolean("tai.cy3.enabled").getOrElse(false)
}

object ApplicationConfig extends ApplicationConfig

trait FeatureTogglesConfig extends DefaultServicesConfig {
  val cyPlusOneEnabled = configuration.getBoolean("tai.cyPlusOne.enabled").getOrElse(false)
  val welshLanguageEnabled = configuration.getBoolean("tai.feature.welshLanguage.enabled").getOrElse(false)
  val companyCarForceRedirectEnabled =
    configuration.getBoolean("tai.feature.companyCarForceRedirect.enabled").getOrElse(false)
  val cyPlus1EstimatedPayEnabled = configuration.getBoolean("tai.cyPlusOne.enabled").getOrElse(false)
  val webChatEnabled = configuration.getBoolean("tai.webChat.enabled").getOrElse(false)
}

object FeatureTogglesConfig extends FeatureTogglesConfig

trait TaiConfig extends DefaultServicesConfig {
  lazy val baseURL: String = baseUrl("tai")
}
