import com.typesafe.sbt.web.SbtWeb
import play.sbt.routes.RoutesKeys._
import sbt.GlobFilter
import sbt.Keys._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, integrationTestSettings}
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import com.typesafe.sbt.uglify.Import._

val appName = "tai-frontend"
lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtWeb)
lazy val playSettings: Seq[Setting[_]] = Seq(
  routesImport ++= Seq(
    "uk.gov.hmrc.domain._",
    "_root_.uk.gov.hmrc.tai.binders.TaxYearObjectBinder._",
    "_root_.uk.gov.hmrc.tai.binders.BenefitComponentTypeBinder._"))

lazy val scoverageSettings = {

  val scoverageExcludePatterns =
    List("<empty>", "Reverse.*", "dev.Routes.*", "tai.Routes.*", "prod.*", "testOnlyDoNotUseInAppConf.*", "config.*")

  Seq(
    ScoverageKeys.coverageExcludedPackages := scoverageExcludePatterns.mkString("", ";", ""),
    ScoverageKeys.coverageMinimum := 92.31,
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

val silencerVersion = "1.7.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .settings(playSettings ++ scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(scalaVersion := "2.12.17")
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
  .settings(Test / Keys.fork := true)
  .settings(
    // concatenate js
    Concat.groups := Seq(
      "javascripts/tai-app.js" -> group(Seq(
        "javascripts/card.js",
        "javascripts/char-count.js",
        "javascripts/tai-backlink.js",
        "javascripts/tai-new.js",
        "javascripts/tax-code-change.js",
        "javascripts/urbanner.js"
      ))
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    // below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat, uglify),
    // only compress files generated by concat
    uglify / includeFilter := GlobFilter("tai-*.js")
  )
  .settings(
    scalacOptions += "-P:silencer:pathFilters=views;routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  )

lazy val TemplateTest = config("tt") extend Test

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

scalacOptions += "-Ypartial-unification"
// Scalafix configuration - Only un comment if you want to correct the styling of the service, then comment again as this causes compile and test issues in the service
//ThisBuild / semanticdbEnabled := true
//ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
//ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4"
//ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)
