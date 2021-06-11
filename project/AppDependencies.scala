import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._
  
  private val playVersion = "play-27"

  val compile = Seq(
    filters,
    jodaForms,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"    % s"1.9.0-$playVersion",
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion" % "5.3.0",
    "uk.gov.hmrc"       %% "domain"                           % s"5.10.0-$playVersion",
    "uk.gov.hmrc"       %% "url-builder"                      % s"3.4.0-$playVersion",
    "uk.gov.hmrc"       %% "play-partials"                    % s"8.1.0-$playVersion",
    "uk.gov.hmrc"       %% "play-language"                    % s"4.10.0-$playVersion",
    "uk.gov.hmrc"       %% "local-template-renderer"          % s"2.15.0-$playVersion",
    "uk.gov.hmrc"       %% "play-ui"                          % s"9.2.0-$playVersion",
    "org.typelevel"     %% "cats-core"                        % "2.0.0",
    "com.typesafe.play" %% "play-json-joda"                   % "2.6.10",
    "uk.gov.hmrc"       %% "digital-engagement-platform-chat" % s"0.14.0-$playVersion"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "org.scalatest"            %%    "scalatest"                   % "3.0.9",
        "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % scope,
        "org.pegdown"            %  "pegdown"            % "1.6.0" % scope,
        "org.jsoup"              %  "jsoup"              % "1.8.3" % scope,
        "org.mockito"            %  "mockito-all"        % "1.10.19" % scope,
        "com.github.tomakehurst" %  "wiremock-jre8"      % "2.26.1" % scope,
        "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope,
        "org.scalacheck"         %% "scalacheck"         % "1.14.3" % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}
