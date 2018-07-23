import sbt._

object FrontendBuild extends Build with MicroService {

  import com.typesafe.sbt.web.SbtWeb
  import com.typesafe.sbt.web.SbtWeb.autoImport._
  import play.sbt.routes.RoutesKeys._
  import sbt.Keys._

  val appName = "tai-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtWeb)
  override lazy val playSettings : Seq[Setting[_]] = Seq(
    routesImport ++= Seq(
      "uk.gov.hmrc.domain._",
      "_root_.uk.gov.hmrc.tai.binders.TaxYearObjectBinder._",
      "_root_.uk.gov.hmrc.tai.binders.BenefitComponentTypeBinder._"),
    unmanagedResourceDirectories in Assets += baseDirectory.value / "app" / "assets",
    excludeFilter in Assets := "js*" || "sass*"
  ) ++ JavaScriptBuild.javaScriptUiSettings
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile = Seq(
    filters,
    "uk.gov.hmrc"  %% "play-conditional-form-mapping" %  "0.2.0",
    "uk.gov.hmrc"  %%  "frontend-bootstrap"           %  "8.24.0",
    "uk.gov.hmrc"  %%  "url-builder"                  %  "2.1.0",
    "uk.gov.hmrc"  %%  "play-partials"                %  "6.1.0",
    "uk.gov.hmrc"  %%  "csp-client"                   %  "2.1.0",
    "uk.gov.hmrc"  %%  "play-language"                %  "3.4.0",
    "uk.gov.hmrc"  %%  "local-template-renderer"      %  "2.0.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest"     %%    "scalatest"  %  "2.2.6"  % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "uk.gov.hmrc"       %%    "hmrctest"   %  "2.3.0"  % scope,
        "org.pegdown"        %    "pegdown"    %  "1.6.0"  % scope,
        "org.jsoup"          %    "jsoup"      %  "1.8.3"  % scope,
        "org.mockito"      % "mockito-all" % "1.9.5" % scope,
        "org.mockito" % "mockito-core" % "1.9.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
        "org.scalatest" %% "scalatest" % "2.2.6" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup"          %    "jsoup"      %  "1.8.3"  % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()

}