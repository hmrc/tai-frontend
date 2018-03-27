import play.routes.compiler.StaticRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning


trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import TestPhases._
  import uk.gov.hmrc.SbtAutoBuildPlugin
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

  val appName: String

  lazy val appDependencies: Seq[ModuleID] = Seq.empty
  lazy val playSettings: Seq[Setting[_]] = Seq.empty

  lazy val scoverageSettings = {

    val scoverageExcludePatterns = List(
      "<empty>",
      "Reverse.*",
      "app.Routes.*",
      "tai.Routes.*",
      "prod.*",
      "testOnlyDoNotUseInAppConf.*",
      "config.*",
      "controllers.AuthenticationConnectors")

    Seq(
      ScoverageKeys.coverageExcludedPackages := scoverageExcludePatterns.mkString("", ";", ""),
      ScoverageKeys.coverageMinimum := 84,
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

  def findSbtFiles(rootDir: File): Seq[String] = {
    if (rootDir.getName == "project") {
      rootDir.listFiles().map(_.getName).toSeq
    } else {
      Seq()
    }
  }

  def findPlayConfFiles(rootDir: File): Seq[String] = {
    Option {
      new File(rootDir, "conf").listFiles()
    }.fold(Seq[String]()) { confFiles =>
      confFiles
        .map(_.getName.replace(".routes", ".Routes"))
    }
  }


  val wartRemovedExcludedClasses = Seq(
    "uk.gov.hmrc.BuildInfo"
  )

  lazy val microservice = Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
    .settings(playSettings ++ scoverageSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(scalaVersion := "2.11.11")
    .settings(
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      routesGenerator := StaticRoutesGenerator)
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .configs(IntegrationTest)
    .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
    .settings(resolvers ++= Seq(Resolver.jcenterRepo))
}

private object TestPhases {

  val allPhases = "tt->test;test->test;test->compile;compile->compile"
  val allItPhases = "tit->it;it->it;it->compile;compile->compile"

  lazy val TemplateTest = config("tt") extend Test
  lazy val TemplateItTest = config("tit") extend IntegrationTest

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}