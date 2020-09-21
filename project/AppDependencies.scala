import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._
  
  private val playVersion = "play-26"

  val compile = Seq(
    filters,
    jodaForms,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % s"1.2.0-$playVersion",
    "uk.gov.hmrc"       %% s"bootstrap-$playVersion"       % "1.16.0",
    "uk.gov.hmrc"       %% "domain"                        % s"5.10.0-$playVersion",
    "uk.gov.hmrc"       %% "url-builder"                   % s"3.4.0-$playVersion",
    "uk.gov.hmrc"       %% "play-partials"                 % s"6.11.0-$playVersion",
    "uk.gov.hmrc"       %% "play-language"                 % "3.4.0",
    "uk.gov.hmrc"       %% "local-template-renderer"       % s"2.9.0-$playVersion",
    "uk.gov.hmrc"       %% "auth-client"                   % s"3.1.0-$playVersion",
    "uk.gov.hmrc"       %% "play-ui"                       % s"8.12.0-$playVersion",
    "uk.gov.hmrc"       %% "play-config"                   % "7.5.0",
    "org.typelevel"     %% "cats-core"                     % "2.0.0",
    "com.typesafe.play" %% "play-json-joda"                % "2.6.10"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
        "uk.gov.hmrc"            %% "hmrctest"           % s"3.8.0-$playVersion" % scope,
        "org.pegdown"            %  "pegdown"            % "1.6.0" % scope,
        "org.jsoup"              %  "jsoup"              % "1.8.3" % scope,
        "org.mockito"            %  "mockito-all"        % "1.9.5" % scope,
        "org.mockito"            %  "mockito-core"       % "1.9.0" % scope,
        "com.github.tomakehurst" %  "wiremock"           % "2.15.0" % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope,
        "org.scalacheck"         %% "scalacheck"         % "1.13.4" % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc"            %% "hmrctest"           % s"3.8.0-$playVersion" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
        "org.pegdown"            %  "pegdown"            % "1.6.0" % scope,
        "org.jsoup"              %  "jsoup"              % "1.8.3" % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
