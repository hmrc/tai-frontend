import sbt._

object FrontendBuild extends Build with MicroService {

  import com.typesafe.sbt.web.SbtWeb
  import com.typesafe.sbt.web.SbtWeb.autoImport._
  import play.sbt.routes.RoutesKeys._
  import sbt.Keys._

  val appName = "tai-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
  lazy val plugins: Seq[Plugins] = Seq(play.sbt.PlayScala, SbtWeb)
  override lazy val playSettings: Seq[Setting[_]] = Seq(
    dependencyOverrides += "uk.gov.hmrc" %% "play-config" % "7.5.0",
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
    "uk.gov.hmrc"  %% "play-conditional-form-mapping" % "0.2.0",
    "uk.gov.hmrc"  %% "bootstrap-play-25"             % "4.12.0",
    "uk.gov.hmrc"  %% "domain"                        % "5.6.0-play-25",
    "uk.gov.hmrc"  %% "url-builder"                   % "2.1.0",
    "uk.gov.hmrc"  %% "play-partials"                 % "6.9.0-play-25",
    "uk.gov.hmrc"  %% "csp-client"                    % "3.4.0",
    "uk.gov.hmrc"  %% "play-language"                 % "3.4.0",
    "uk.gov.hmrc"  %% "local-template-renderer"       % "2.4.0",
    "uk.gov.hmrc"  %% "auth-client"                   % "2.20.0-play-25",
    "uk.gov.hmrc"  %% "govuk-template"                % "5.35.0-play-25",
    "uk.gov.hmrc"  %% "play-ui"                       % "7.40.0-play-25"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
        "uk.gov.hmrc"       %%    "hmrctest"   %  "3.8.0-play-25"  % scope,
        "org.pegdown"        %    "pegdown"    %  "1.6.0"  % scope,
        "org.jsoup"          %    "jsoup"      %  "1.8.3"  % scope,
        "org.mockito"      % "mockito-all" % "1.9.5" % scope,
        "org.mockito" % "mockito-core" % "1.9.0" % scope,
        "com.github.tomakehurst" % "wiremock" % "2.15.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalacheck" %% "scalacheck" % "1.13.4" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.8.0-play-25" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()

}