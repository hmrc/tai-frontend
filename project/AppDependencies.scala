import play.sbt.PlayImport.*
import sbt.*

private object AppDependencies {
  private val playVersion = "play-29"
  private val hmrcMongoVersion = "1.8.0"
  private val bootstrapVersion = "8.5.0"
  private val webChatVersion = "1.4.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    "org.typelevel" %% "cats-core"                                   % "2.10.0",
    "uk.gov.hmrc"   %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
    "uk.gov.hmrc"   %% "digital-engagement-platform-chat-29"         % webChatVersion,
    "uk.gov.hmrc"   %% s"mongo-feature-toggles-client-$playVersion"  % "1.3.0",
    "uk.gov.hmrc"   %% s"sca-wrapper-$playVersion"                   % "1.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.mockito"       %% "mockito-scala-scalatest"       % "1.17.31",
    "org.scalatestplus" %% "scalacheck-1-17"               % "3.2.18.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion,
    "org.jsoup"          % "jsoup"                         % "1.17.2"
  ).map(_ % "test")

  val all: Seq[ModuleID] =
    compile ++ test
}
