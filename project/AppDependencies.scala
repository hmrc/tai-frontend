import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val scalaCompatibleVersion = "2.12"
  private val playVersion = "play-28"

  val compile = Seq(
    filters,
    jodaForms,
    "org.typelevel"     %% "cats-core"                                       % "2.9.0",
    "com.typesafe.play" %% "play-json-joda"                                  % "2.9.3",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"                   % s"1.12.0-$playVersion",
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion"                % "7.9.0",
    "uk.gov.hmrc"       %% "domain"                                          % s"8.1.0-$playVersion",
    "uk.gov.hmrc"       %% "url-builder"                                     % s"3.7.0-$playVersion",
    "uk.gov.hmrc"       % s"local-template-renderer_$scalaCompatibleVersion" % s"2.17.0-$playVersion",
    "uk.gov.hmrc"       %% "digital-engagement-platform-chat"                % s"0.32.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"                              % s"3.34.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-pta"                               % "0.4.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test,it"
    lazy val test: Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply() =
      new TestDependencies {
        override lazy val test = Seq(
          "org.scalatest"          %% "scalatest"              % "3.0.9"             % scope,
          "org.scalatestplus.play" %% "scalatestplus-play"     % "4.0.3"             % scope,
          "org.pegdown"            % "pegdown"                 % "1.6.0"             % scope,
          "org.jsoup"              % "jsoup"                   % "1.8.3"             % scope,
          "org.mockito"            % "mockito-all"             % "1.10.19"           % scope,
          "com.github.tomakehurst" % "wiremock-jre8"           % "2.26.1"            % scope,
          "com.typesafe.play"      %% "play-test"              % PlayVersion.current % scope,
          "org.scalacheck"         %% "scalacheck"             % "1.14.3"            % scope,
          "uk.gov.hmrc"            %% "bootstrap-test-play-28" % "7.13.0"             % scope
        )
      }.test
  }

  def apply() = compile ++ Test()
}
