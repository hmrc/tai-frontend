import play.sbt.PlayImport.*
import sbt.*

private object AppDependencies {
  private val playVersion = "play-30"
  private val hmrcMongoVersion = "1.9.0"
  private val bootstrapVersion = "8.6.0"
  private val webChatVersion = "1.6.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    "org.typelevel" %% "cats-core"                                   % "2.12.0",
    "uk.gov.hmrc"   %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
    "uk.gov.hmrc"   %% "digital-engagement-platform-chat-30"         % webChatVersion,
    "uk.gov.hmrc"   %% s"mongo-feature-toggles-client-$playVersion"  % "1.5.0",
    "uk.gov.hmrc"   %% s"sca-wrapper-$playVersion"                   % "1.9.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.mockito"       %% "mockito-scala-scalatest"       % "1.17.37",
    "org.scalatestplus" %% "scalacheck-1-17"               % "3.2.18.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.jsoup"          % "jsoup"                         % "1.17.2"
  ).map(_ % "test")

  val all: Seq[ModuleID] =
    compile ++ test
}
