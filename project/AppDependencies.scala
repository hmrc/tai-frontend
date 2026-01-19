import play.sbt.PlayImport.*
import sbt.*

private object AppDependencies {
  private val playVersion               = "play-30"
  private val scaWrapperVersion         = "4.5.0"
  private val mongoFeatureToggleVersion = "2.4.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    "org.typelevel" %% "cats-core"                                   % "2.13.0",
    "uk.gov.hmrc"   %% s"play-conditional-form-mapping-$playVersion" % "3.4.0",
    "uk.gov.hmrc"   %% s"mongo-feature-toggles-client-$playVersion"  % mongoFeatureToggleVersion,
    "uk.gov.hmrc"   %% s"sca-wrapper-$playVersion"                   % scaWrapperVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"mongo-feature-toggles-client-test-$playVersion" % mongoFeatureToggleVersion,
    "uk.gov.hmrc"       %% s"sca-wrapper-test-$playVersion"                  % scaWrapperVersion,
    "org.jsoup"          % "jsoup"                                           % "1.22.1",
    "org.scalatestplus" %% "scalacheck-1-18"                                 % "3.2.19.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] =
    compile ++ test
}
