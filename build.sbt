import com.typesafe.sbt.web.SbtWeb
import play.sbt.routes.RoutesKeys.*
import sbt.Keys.*
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.defaultSettings

val appName = "tai-frontend"

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / scalafmtOnCompile := true

lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtWeb)
lazy val playSettings: Seq[Setting[?]] = Seq(
  routesImport ++= Seq(
    "uk.gov.hmrc.domain._",
    "uk.gov.hmrc.tai.model.admin._",
    "_root_.uk.gov.hmrc.tai.binders.TaxYearObjectBinder._",
    "_root_.uk.gov.hmrc.tai.binders.BenefitComponentTypeBinder._"
  )
)

lazy val scoverageSettings = {
  val scoverageExcludePatterns =
    List("<empty>", "Reverse.*", "dev.Routes.*", "tai.Routes.*", "prod.*", "testOnlyDoNotUseInAppConf.*", "config.*", "testOnly.*")

  Seq(
    ScoverageKeys.coverageExcludedPackages := scoverageExcludePatterns.mkString("", ";", ""),
    ScoverageKeys.coverageMinimumStmtTotal := 92.31,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
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

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= AppDependencies.all,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    PlayKeys.playDefaultPort := 9230
  )
  .settings(resolvers ++= Seq(Resolver.jcenterRepo))
  .settings(
    scalacOptions ++= Seq(
      "-feature",
      "-Werror",
      "-Wdead-code",
      "-Wunused:_",
      "-Wextra-implicit",
      "-Wconf:cat=unused-imports&site=.*views\\.html.*:s",
      "-Wconf:cat=unused-imports&site=<empty>:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:cat=unused&src=.*JavaScriptReverseRoutes\\.scala:s",
      "-Wconf:cat=deprecation&msg=\\.*value readRaw in object HttpReads is deprecated\\.*:s",
      "-Wconf:cat=deprecation&msg=\\.*method handleResponse in trait HttpErrorFunctions is deprecated\\.*:s",
      "-Wconf:cat=deprecation&msg=trait HttpClient in package http is deprecated \\(since 15.0.0\\).*:s",
      "-Wconf:cat=deprecation&msg=trait HttpGet in package http is deprecated \\(since 15.0.0\\).*:s",
      "-Wconf:msg=\\.*match may not be exhaustive.\\.*:s",
      "-Wconf:msg=a type was inferred to be `Object`; this may indicate a programming error\\.:s",
    )
  )
  .settings(scalacOptions ++= Seq("-Ypatmat-exhaust-depth", "off"))

Test / Keys.fork := true
Test / scalacOptions --= Seq("-Wdead-code", "-Wvalue-discard")

lazy val it = project
  .enablePlugins(play.sbt.PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    libraryDependencies ++= AppDependencies.test,
    DefaultBuildSettings.itSettings()
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

// Scalafix configuration - Only un comment if you want to correct the styling of the service, then comment again as this causes compile and test issues in the service
//ThisBuild / semanticdbEnabled := true
//ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
//ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"
//ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)
