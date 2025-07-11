/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import javax.inject.Inject

class ApplicationConfig @Inject() (
  val runModeConfiguration: Configuration,
  servicesConfig: ServicesConfig
) extends FeatureTogglesConfig
    with AuthConfigProperties {

  def getOptional[A](key: String)(implicit loader: ConfigLoader[A]): Option[A] =
    runModeConfiguration.getOptional[A](key)

  private def decorateUrlForLocalDev(key: String): String =
    runModeConfiguration.getOptional[String](s"external-url.$key").getOrElse("")

  private lazy val dfsDigitalFormsFrontend: String = servicesConfig.baseUrl("dfs-digital-forms-frontend")

  lazy val incomeTaxFormPartialLinkUrl: String =
    s"$dfsDigitalFormsFrontend/digital-forms/forms/personal-tax/income-tax/catalogue"

  lazy val incomeFromEmploymentPensionLinkUrl: String =
    s"$dfsFrontendHost/submissions/new-form/tell-hmrc-about-your-pension-income/"
  lazy val companyBenefitsLinkUrl: String             =
    s"$dfsFrontendHost/submissions/new-form/tell-hmrc-about-your-company-benefits/"
  lazy val taxableStateBenefitLinkUrl: String         =
    s"$dfsFrontendHost/submissions/new-form/tell-hmrc-about-your-taxable-state-benefit/"
  lazy val otherIncomeLinkUrl: String                 =
    s"$dfsFrontendHost/digital-forms/form/tell-us-about-other-income/draft/guide"
  lazy val investmentIncomeLinkUrl: String            =
    s"$dfsFrontendHost/submissions/new-form/tell-hmrc-about-your-investment-income/"
  lazy val taxFreeAllowanceLinkUrl: String            =
    s"$dfsFrontendHost/submissions/new-form/tell-hmrc-about-your-tax-free-allowance"

  lazy val hicbcUpdateUrl: String =
    runModeConfiguration.get[String]("external-url.hicbcUrl")

  private lazy val accessibilityBaseUrl: String     = servicesConfig.getString(s"accessibility-statement.baseUrl")
  lazy private val accessibilityRedirectUrl: String = servicesConfig.getString(s"accessibility-statement.redirectUrl")

  def accessibilityStatementUrl: String = {
    val redirectUrl = RedirectUrl(taiHomePageUrl).getEither(
      OnlyRelative | AbsoluteWithHostnameFromAllowlist("localhost")
    ) match {
      case Right(safeRedirectUrl) => safeRedirectUrl.url
      case Left(error)            => throw new IllegalArgumentException(error)
    }
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=$redirectUrl"
  }

  lazy val checkUpdateProgressLinkUrl: String   = s"$trackFrontendHost/track"
  lazy val pertaxServiceUrl: String             = s"$pertaxFrontendHost/personal-account"
  lazy val pertaxServiceUpliftFailedUrl: String = s"$pertaxFrontendHost/personal-account/identity-check-failed"
  private lazy val feedbackSurveyUrl: String    = s"$feedbackHost/feedback/TES"
  lazy val cocarFrontendUrl: String             = s"$cocarFrontendHost/paye/company-car/details"
  lazy val marriageServiceHistoryUrl: String    = s"$tamcFrontendHost/marriage-allowance-application/history"
  lazy val medBenefitServiceUrl: String         = s"$benefitsFrontendHost/paye/benefits/medical-benefit"

  private lazy val ivUpliftPrefix: String = decorateUrlForLocalDev("identity-verification.prefix")
  lazy val sa16UpliftUrl: String          = s"$identityVerificationHost/$ivUpliftPrefix/uplift"

  lazy val taiHomePageUrl: String   = s"$taiRootUri/check-income-tax/what-do-you-want-to-do"
  lazy val taxYouPaidStatus: String = s"$taxCalcFrontendHost/tax-you-paid/status"
  lazy val hardshipHelpUrl: String  =
    s"$dfsFrontendHost/digital-forms/form/tell-us-how-you-want-to-pay-estimated-tax/draft/guide"

  lazy val scottishRateIncomeTaxUrl: String   = "https://www.gov.uk/scottish-rate-income-tax"
  lazy val welshRateIncomeTaxUrl: String      = "https://www.gov.uk/welsh-income-tax"
  lazy val welshRateIncomeTaxWelshUrl: String = "https://www.gov.uk/treth-incwm-cymru"
  lazy val contactHelplineUrl: String         =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
  lazy val contactHelplineWelshUrl: String    =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/welsh-language-helplines"

  lazy val basGatewayFrontendSignOutUrl: String =
    s"$basGatewayHost/bas-gateway/sign-out-without-state?continue=$feedbackSurveyUrl"

  lazy val basGatewayFrontendSignInUrl: String = s"$basGatewayHost/bas-gateway/sign-in"

  lazy val sessionTimeoutInSeconds: Long = getOptional[Int]("tai.session.timeout").getOrElse(900)

  // These hosts should be empty for Prod like environments, all frontend services run on the same host so e.g localhost:9030/tai in local should be /tai in prod
  private lazy val basGatewayHost: String           = decorateUrlForLocalDev("bas-gateway-frontend.host")
  private lazy val feedbackHost: String             = decorateUrlForLocalDev("feedback-survey-frontend.host")
  lazy val unauthorisedSignOutUrl: String           = decorateUrlForLocalDev("company-auth.unauthorised-url")
  private lazy val dfsFrontendHost: String          = decorateUrlForLocalDev(s"dfs-digital-forms-frontend.host")
  lazy val taiRootUri: String                       = decorateUrlForLocalDev("tai-frontend.host")
  private lazy val pertaxFrontendHost: String       = decorateUrlForLocalDev("pertax-frontend.host")
  private lazy val cocarFrontendHost: String        = decorateUrlForLocalDev("cocar-frontend.host")
  private lazy val tamcFrontendHost: String         = decorateUrlForLocalDev("tamc-frontend.host")
  private lazy val benefitsFrontendHost: String     = decorateUrlForLocalDev("benefits-frontend.host")
  private lazy val identityVerificationHost: String = decorateUrlForLocalDev("identity-verification.host")
  private lazy val taxCalcFrontendHost: String      = decorateUrlForLocalDev("taxcalc-frontend.host")
  private lazy val trackFrontendHost: String        = decorateUrlForLocalDev("tracking-frontend.host")
  lazy val pertaxUrl: String                        =
    servicesConfig.baseUrl("pertax")

  lazy val startEmploymentDateFilteredBefore: LocalDate =
    LocalDate.parse(servicesConfig.getString("feature.startEmploymentDateFilteredBefore"))

  val taiServiceUrl: String = servicesConfig.baseUrl("tai")

  val citizenDetailsUrl: String = servicesConfig.baseUrl("citizen-details")

  val newApiBulkOnboarding: Seq[Int] = runModeConfiguration.get[Seq[Int]]("tai.newApiOnboarding.bulk.allowed")

  lazy val fandfHost: String = servicesConfig.baseUrl("fandf")
}
