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

import javax.inject.Inject
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tai.model.TaxYear
import views.html.helper

class ApplicationConfig @Inject()(
  val runModeConfiguration: Configuration,
  servicesConfig: ServicesConfig
) extends FeatureTogglesConfig with AuthConfigProperties {

  def getOptional[A](key: String)(implicit loader: ConfigLoader[A]): Option[A] =
    runModeConfiguration.getOptional[A](key)

  def statusRange = s"${TaxYear().prev.year}-${TaxYear().year}"

  lazy val baseURL: String = servicesConfig.baseUrl("tai")

  lazy val citizenAuthHost: String = fetchUrl("citizen-auth")
  lazy val companyAuthUrl: String = fetchUrl("company-auth")
  lazy val urlEncode = helper.urlEncode(_: String)
  lazy val incomeTaxFormPartialLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/forms/personal-tax/income-tax/catalogue"
  lazy val incomeFromEmploymentPensionPartialLinkUrl =
    s"${fetchUrl("dfs-digital-forms-frontend")}/digital-forms/form/tell-us-about-income-from-employment-or-pension/draft/guide"

  lazy val incomeFromEmploymentPensionLinkUrl =
    s"$taiRootUri/digital-forms/form/tell-us-about-income-from-employment-or-pension/draft/guide"
  lazy val companyBenefitsLinkUrl =
    s"$taiRootUri/digital-forms/form/tell-us-about-company-benefits/draft/guide"
  lazy val taxableStateBenefitLinkUrl =
    s"$taiRootUri/digital-forms/form/tell-us-about-your-taxable-state-benefit/draft/guide"
  lazy val otherIncomeLinkUrl =
    s"$taiRootUri/digital-forms/form/tell-us-about-other-income/draft/guide"
  lazy val investmentIncomeLinkUrl =
    s"$taiRootUri/digital-forms/form/tell-us-about-investment-income/draft/guide"
  lazy val taxFreeAllowanceLinkUrl =
    s"$taiRootUri/digital-forms/form/check-income-tax-tell-us-your-tax-free-allowance/draft/guide"

  lazy val reportAProblemPartialUrl = s"${fetchUrl("contact-frontend")}/contact/problem_reports?secure=true&service=TAI"
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  lazy val urBannerEnabled: Boolean = getOptional[String]("feature.ur-banner.enabled").getOrElse("true").toBoolean
  lazy val urBannerLink: String = getOptional[String]("ur-banner.url").getOrElse("")
  lazy val checkUpdateProgressLinkUrl = s"${fetchUrl("track")}/track"

  lazy val analyticsToken: Option[String] = getOptional[String]("govuk-tax.google-analytics.token")
  lazy val gaValueOfPayments: String =
    getOptional[String]("govuk-tax.google-analytics.gaValueOfPayments").getOrElse("")
  lazy val gaRecStatus: String = getOptional[String]("govuk-tax.google-analytics.gaRecStatus").getOrElse("")
  lazy val analyticsHost: String = getOptional[String]("govuk-tax.google-analytics.host").getOrElse("auto")
  lazy val pertaxServiceUrl = s"${fetchUrl("pertax-frontend")}/personal-account"
  lazy val pertaxServiceUpliftFailedUrl: String =
    getOptional[String]("govuk-tax.external-url.pertax-frontend.host")
      .getOrElse("") +
      "/personal-account/identity-check-failed"
  lazy val pertaxExitSurveyUrl = s"$pertaxServiceUrl/signout?origin=TES"
  lazy val feedbackSurveyUrl = s"$feedbackHost/feedback/TES"
  lazy val feedbackHost: String =
    getOptional[String]("govuk-tax.external-url.feedback-survey-frontend.host").getOrElse("")
  lazy val cocarFrontendUrl = s"${fetchUrl("cocar-frontend")}/paye/company-car/details"
  lazy val updateCompanyCarDetailsUrl = s"$personServiceUrl/redirect-company-car"
  lazy val personServiceUrl = s"${fetchUrl("tai-frontend")}/check-income-tax"
  lazy val marriageServiceUrl = s"${fetchUrl("tamc-frontend")}/marriage-allowance-application/history"
  lazy val marriageServiceHistoryUrl = s"${fetchUrl("tamc-frontend")}/marriage-allowance-application/history"
  lazy val medBenefitServiceUrl = s"${fetchUrl("benefits-frontend")}/paye/benefits/medical-benefit"
  lazy val mainContentHeaderPartialUrl =
    s"${fetchUrl("header-service")}/personal-account/integration/main-content-header"
  lazy val sa16UpliftUrl = s"${fetchUrl("identity-verification-frontend")}/mdtp/uplift"
  lazy val taiHomePageUrl: String = getOptional[String]("govuk-tax.external-url.tai-frontend.host")
    .getOrElse("") + "/check-income-tax/what-do-you-want-to-do"
  lazy val taxYouPaidStatus = s"${fetchUrl("taxcalc-frontend")}/tax-you-paid/status"
  lazy val gg_web_context: String = getOptional[String]("external-url.gg.web-context").getOrElse("gg")
  lazy val ida_web_context: String = getOptional[String]("external-url.ida.web-context").getOrElse("ida")
  lazy val hardshipHelpBase: String = getOptional[String]("govuk-tax.external-url.hardship-help.host").getOrElse("")
  lazy val hardshipHelpUrl =
    s"$hardshipHelpBase/digital-forms/form/tell-us-how-you-want-to-pay-estimated-tax/draft/guide"
  private val contactHost = getOptional[String](s"govuk-tax.services.contact-frontend.host").getOrElse("")
  lazy val companyAuthFrontendSignOutUrl = s"$companyAuthUrl/gg/sign-out?continue=$feedbackSurveyUrl"
  lazy val unauthorisedSignOutUrl: String =
    getOptional[String]("govuk-tax.external-url.company-auth.unauthorised-url").getOrElse("")
  lazy val citizenAuthFrontendSignOutUrl: String = citizenAuthHost + "/ida/signout"
  lazy val assetsPath =
    s"${getOptional[String](s"assets.url").getOrElse("")}${getOptional[String](s"assets.version").getOrElse("")}/"
  lazy val scottishRateIncomeTaxUrl: String = "https://www.gov.uk/scottish-rate-income-tax"
  lazy val welshRateIncomeTaxUrl: String = "https://www.gov.uk/welsh-income-tax"
  lazy val welshRateIncomeTaxWelshUrl: String = "https://www.gov.uk/treth-incwm-cymru"
  lazy val contactHelplineUrl: String =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
  lazy val contactHelplineWelshUrl: String =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/welsh-language-helplines"

  lazy val frontendTemplatePath: String =
    getOptional[String]("govuk-tax.services.frontend-template-provider.path").getOrElse("/template/mustache")

  lazy val taiRootUri: String = getOptional[String]("govuk-tax.taxPlatformTaiRootUri").getOrElse("")

  def fetchUrl(service: String): String =
    try {
      servicesConfig.baseUrl(service)
    } catch {
      case ex: RuntimeException => taiRootUri
      case _: Throwable         => s"Unknown Exception: $service-url"
    }

  lazy val isTaiCy3Enabled: Boolean = getOptional[Boolean]("tai.cy3.enabled").getOrElse(false)
  lazy val numberOfPreviousYearsToShow: Int = getOptional[Int]("tai.numberOfPreviousYearsToShow").getOrElse(5)
}

trait FeatureTogglesConfig { self: ApplicationConfig =>
  val cyPlusOneEnabled: Boolean = getOptional[Boolean]("tai.cyPlusOne.enabled").getOrElse(false)
  val welshLanguageEnabled: Boolean = getOptional[Boolean]("tai.feature.welshLanguage.enabled").getOrElse(false)
  val companyCarForceRedirectEnabled: Boolean =
    getOptional[Boolean]("tai.feature.companyCarForceRedirect.enabled").getOrElse(false)
  val cyPlus1EstimatedPayEnabled: Boolean = getOptional[Boolean]("tai.cyPlusOne.enabled").getOrElse(false)
  val webChatEnabled: Boolean = getOptional[Boolean]("tai.webChat.enabled").getOrElse(false)
}

trait AuthConfigProperties { self: ApplicationConfig =>

  val postSignInRedirectUrl: Option[String] = getOptional[String]("govuk-tax.login-callback.url")

  val activatePaperless: Boolean = getOptional[Boolean]("govuk-tax.activatePaperless")
    .getOrElse(throw new IllegalStateException("Could not find configuration for govuk-tax.activatePaperless"))

  val activatePaperlessEvenIfGatekeeperFails: Boolean =
    getOptional[Boolean](s"govuk-tax.activatePaperlessEvenIfGatekeeperFails")
      .getOrElse(throw new IllegalStateException("Could not find configuration for govuk-tax.activatePaperless"))

  val taxPlatformTaiRootUri: String =
    getOptional[String]("govuk-tax.taxPlatformTaiRootUri").getOrElse("http://noConfigTaiRootUri")
}
