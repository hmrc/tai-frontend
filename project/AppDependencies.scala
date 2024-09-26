import play.sbt.PlayImport.*
import sbt.*

private object AppDependencies {
  private val playVersion = "play-30"
  private val hmrcMongoVersion = "1.9.0"
  private val bootstrapVersion = "8.6.0"
  private val webChatVersion = "1.6.0"
  private val scaWrapperVersion = "1.13.0"
  private val mongoFeatureToggleVersion = "1.6.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    "org.typelevel" %% "cats-core"                                   % "2.12.0",
    "uk.gov.hmrc"   %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
    "uk.gov.hmrc"   %% "digital-engagement-platform-chat-30"         % webChatVersion,
    "uk.gov.hmrc"   %% s"mongo-feature-toggles-client-$playVersion"  % mongoFeatureToggleVersion,
    "uk.gov.hmrc"   %% s"sca-wrapper-$playVersion"                   % scaWrapperVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"mongo-feature-toggles-client-test-$playVersion"  % mongoFeatureToggleVersion,
    "uk.gov.hmrc"   %% s"sca-wrapper-test-$playVersion"                   % scaWrapperVersion,
    "org.jsoup"     % "jsoup"                         % "1.18.1",
    "org.scalatestplus" %% "scalacheck-1-17"               % "3.2.18.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] =
    compile ++ test
}
