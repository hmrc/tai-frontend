/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ApplicationConfig @Inject()(
  val runModeConfiguration: Configuration,
  servicesConfig: ServicesConfig
) extends FeatureTogglesConfig with AuthConfigProperties {

  def getOptional[A](key: String)(implicit loader: ConfigLoader[A]): Option[A] =
    runModeConfiguration.getOptional[A](key)

  def decorateUrlForLocalDev(key: String): String =
    runModeConfiguration.getOptional[String](s"external-url.$key").getOrElse("")

  lazy val dfsDigitalFormsFrontend = servicesConfig.baseUrl("dfs-digital-forms-frontend")

  lazy val incomeTaxFormPartialLinkUrl =
    s"$dfsDigitalFormsFrontend/digital-forms/forms/personal-tax/income-tax/catalogue"

  lazy val jrsClaimsFromDate: String = servicesConfig.getString("tai.jrs.claims.from.date")

  lazy val incomeFromEmploymentPensionLinkUrl =
    s"$dfsFrontendHost/digital-forms/form/tell-us-about-income-from-employment-or-pension/draft/guide"
  lazy val companyBenefitsLinkUrl =
    s"$dfsFrontendHost/digital-forms/form/tell-us-about-company-benefits/draft/guide"
  lazy val taxableStateBenefitLinkUrl =
    s"$dfsFrontendHost/digital-forms/form/tell-us-about-your-taxable-state-benefit/draft/guide"
  lazy val otherIncomeLinkUrl =
    s"$dfsFrontendHost/digital-forms/form/tell-us-about-other-income/draft/guide"
  lazy val investmentIncomeLinkUrl =
    s"$dfsFrontendHost/digital-forms/form/tell-us-about-investment-income/draft/guide"
  lazy val taxFreeAllowanceLinkUrl =
    s"$dfsFrontendHost/digital-forms/form/check-income-tax-tell-us-your-tax-free-allowance/draft/guide"

  lazy val accessibilityBaseUrl: String = servicesConfig.getString(s"accessibility-statement.baseUrl")
  lazy private val accessibilityRedirectUrl: String = servicesConfig.getString(s"accessibility-statement.redirectUrl")
  def accessibilityStatementUrl(referrer: String) =
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=${SafeRedirectUrl(
      accessibilityBaseUrl + referrer).encodedUrl}"

  lazy val reportAProblemPartialUrl =
    s"${servicesConfig.baseUrl("contact-frontend")}/contact/problem_reports?secure=true&service=TAI"
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  lazy val urBannerEnabled: Boolean = getOptional[String]("feature.ur-banner.enabled").getOrElse("true").toBoolean
  lazy val urBannerLink: String = getOptional[String]("ur-banner.url").getOrElse("")
  lazy val checkUpdateProgressLinkUrl = s"$trackFrontendHost/track"
  lazy val analyticsToken: Option[String] = getOptional[String]("microservice.google-analytics.token")
  lazy val gaValueOfPayments: String =
    getOptional[String]("microservice.google-analytics.gaValueOfPayments").getOrElse("")
  lazy val gaRecStatus: String = getOptional[String]("microservice.google-analytics.gaRecStatus").getOrElse("")
  lazy val analyticsHost: String = getOptional[String]("microservice.google-analytics.host").getOrElse("auto")
  lazy val pertaxServiceUrl = s"$pertaxFrontendHost/personal-account"
  lazy val pertaxServiceUpliftFailedUrl: String = s"$pertaxFrontendHost/personal-account/identity-check-failed"
  lazy val feedbackSurveyUrl = s"$feedbackHost/feedback/TES"
  lazy val cocarFrontendUrl = s"$cocarFrontendHost/paye/company-car/details"
  lazy val marriageServiceHistoryUrl = s"$tamcFrontendHost/marriage-allowance-application/history"
  lazy val medBenefitServiceUrl = s"$benefitsFrontendHost/paye/benefits/medical-benefit"

  lazy val ivUpliftprefix = decorateUrlForLocalDev("identity-verification.prefix")
  lazy val sa16UpliftUrl = s"$identityVerificationHost/$ivUpliftprefix/uplift"

  lazy val taiHomePageUrl: String = s"$taiRootUri/check-income-tax/what-do-you-want-to-do"
  lazy val taxYouPaidStatus = s"$taxCalcFrontendHost/tax-you-paid/status"
  lazy val hardshipHelpUrl =
    s"$dfsFrontendHost/digital-forms/form/tell-us-how-you-want-to-pay-estimated-tax/draft/guide"
  private val contactHost = getOptional[String](s"microservice.services.contact-frontend.host").getOrElse("")

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
    getOptional[String]("microservice.services.frontend-template-provider.path").getOrElse("/template/mustache")

  lazy val basGatewayFrontendSignOutUrl =
    s"$basGatewayHost/bas-gateway/sign-out-without-state?continue=$feedbackSurveyUrl"

  lazy val taxReliefExpenseClaimLink =
    taxReliefExpenseClaimHost + "/claim-tax-relief-expenses/only-claiming-working-from-home-tax-relief"

  lazy val basGatewayFrontendSignInUrl = s"$basGatewayHost/bas-gateway/sign-in"

  lazy val citizenAuthFrontendSignOutUrl: String = citizenAuthHost + "/ida/signout"

  lazy val sessionTimeoutInSeconds: Int = getOptional[Int]("tai.session.timeout").getOrElse(900)
  lazy val sessionCountdownInSeconds: Int = getOptional[Int]("tai.session.countdown").getOrElse(120)

  //These hosts should be empty for Prod like environments, all frontend services run on the same host so e.g localhost:9030/tai in local should be /tai in prod
  lazy val citizenAuthHost: String = decorateUrlForLocalDev("citizen-auth.host")
  lazy val taxReliefExpenseClaimHost: String = decorateUrlForLocalDev("p87-frontend.host")
  lazy val basGatewayHost: String = decorateUrlForLocalDev("bas-gateway-frontend.host")
  lazy val feedbackHost: String = decorateUrlForLocalDev("feedback-survey-frontend.host")
  lazy val unauthorisedSignOutUrl: String = decorateUrlForLocalDev("company-auth.unauthorised-url")
  lazy val dfsFrontendHost = decorateUrlForLocalDev(s"dfs-digital-forms-frontend.host")
  lazy val taiRootUri: String = decorateUrlForLocalDev("tai-frontend.host")
  lazy val pertaxFrontendHost: String = decorateUrlForLocalDev("pertax-frontend.host")
  lazy val cocarFrontendHost: String = decorateUrlForLocalDev("cocar-frontend.host")
  lazy val tamcFrontendHost: String = decorateUrlForLocalDev("tamc-frontend.host")
  lazy val benefitsFrontendHost: String = decorateUrlForLocalDev("benefits-frontend.host")
  lazy val identityVerificationHost: String = decorateUrlForLocalDev("identity-verification.host")
  lazy val taxCalcFrontendHost: String = decorateUrlForLocalDev("taxcalc-frontend.host")
  lazy val trackFrontendHost: String = decorateUrlForLocalDev("tracking-frontend.host")
  lazy val jrsClaimsServiceUrl: String = servicesConfig.baseUrl("coronavirus-jrs-published-employees")
}

trait FeatureTogglesConfig { self: ApplicationConfig =>
  val cyPlusOneEnabled: Boolean = getOptional[Boolean]("tai.cyPlusOne.enabled").getOrElse(false)
  val jrsClaimsEnabled: Boolean = getOptional[Boolean]("tai.jrsClaims.enabled").getOrElse(false)
  val welshLanguageEnabled: Boolean = getOptional[Boolean]("tai.feature.welshLanguage.enabled").getOrElse(false)
  val companyCarForceRedirectEnabled: Boolean =
    getOptional[Boolean]("tai.feature.companyCarForceRedirect.enabled").getOrElse(false)
  val cyPlus1EstimatedPayEnabled: Boolean = getOptional[Boolean]("tai.cyPlusOne.enabled").getOrElse(false)
  lazy val accessibilityStatementToggle: Boolean =
    getOptional[Boolean]("accessibility-statement.toggle").getOrElse(false)
  lazy val isTaiCy3Enabled: Boolean = getOptional[Boolean]("tai.cy3.enabled").getOrElse(false)

  val numberOfPreviousYearsToShow: Int = 5 // Always 5 in all configs
}

trait AuthConfigProperties { self: ApplicationConfig =>
  val postSignInRedirectUrl: Option[String] = getOptional[String]("microservice.login-callback.url")

  val taxPlatformTaiRootUri: String =
    getOptional[String]("microservice.taxPlatformTaiRootUri").getOrElse("http://noConfigTaiRootUri")
}
