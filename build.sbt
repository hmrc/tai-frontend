import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport._
import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, integrationTestSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "tai-frontend"
lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtWeb)
lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq(
    "uk.gov.hmrc.domain._",
    "_root_.uk.gov.hmrc.tai.binders.TaxYearObjectBinder._",
    "_root_.uk.gov.hmrc.tai.binders.BenefitComponentTypeBinder._"),
  unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
  excludeFilter in Assets := "js*" || "sass*"
) ++ JavaScriptBuild.javaScriptUiSettings

lazy val scoverageSettings = {

  val scoverageExcludePatterns =
    List("<empty>", "Reverse.*", "dev.Routes.*", "tai.Routes.*", "prod.*", "testOnlyDoNotUseInAppConf.*", "config.*")

  Seq(
    ScoverageKeys.coverageExcludedPackages := scoverageExcludePatterns.mkString("", ";", ""),
    ScoverageKeys.coverageMinimum := 92.52,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

def makeExcludedFiles(rootDir: File): Seq[String] = {
  val excluded = findPlayConfFiles(rootDir) ++ findSbtFiles(rootDir) ++ wartRemovedExcludedClasses
  println(s"[auto-code-review] excluding the following files: ${excluded.mkString(",")}")
  excluded
}

def findSbtFiles(rootDir: File): Seq[String] =
  if (rootDir.getName == "project") {
    rootDir.listFiles().map(_.getName).toSeq
  } else {
    Seq()
  }

def findPlayConfFiles(rootDir: File): Seq[String] =
  Option {
    new File(rootDir, "conf").listFiles()
  }.fold(Seq[String]()) { confFiles =>
    confFiles
      .map(_.getName.replace(".routes", ".Routes"))
  }

val wartRemovedExcludedClasses = Seq(
  "uk.gov.hmrc.BuildInfo"
)

val akkaVersion = "2.5.23"
val akkaPackage = "com.typesafe.akka"

dependencyOverrides ++= Set(
  akkaPackage %% "akka-stream"    % akkaVersion,
  akkaPackage %% "akka-protobuf"  % akkaVersion,
  akkaPackage %% "akka-slf4j"     % akkaVersion,
  akkaPackage %% "akka-actor"     % akkaVersion,
  akkaPackage %% "akka-http-core" % "10.0.15"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(scalaVersion := "2.11.12")
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    scalafmtOnCompile := true,
    PlayKeys.playDefaultPort := 9230
  )
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings())
  .settings(resolvers ++= Seq(Resolver.jcenterRepo))
  .settings(majorVersion := 0)
  .settings(Keys.fork in Test := true)

lazy val TemplateTest = config("tt") extend Test
