import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val playVersion = "play-28"
  private val bootstrapVersion = "7.15.0"

  val compile = Seq(
    filters,
    jodaForms,
    "org.typelevel"     %% "cats-core"                        % "2.9.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"    % s"1.12.0-$playVersion",
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain"                           % s"8.1.0-$playVersion",
    "uk.gov.hmrc"       %% "digital-engagement-platform-chat" % s"0.32.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"               % s"7.7.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-pta"                % "0.5.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test,it"
    lazy val test: Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply() =
      new TestDependencies {
        override lazy val test = Seq(
          "org.scalatest"          %% "scalatest"          % "3.0.9"             % scope,
          "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0"             % scope,
          "org.pegdown"            % "pegdown"             % "1.6.0"             % scope,
          "org.jsoup"              % "jsoup"               % "1.15.4"            % scope,
          "org.mockito"            %% "mockito-scala-scalatest"       % "1.17.12",
          "com.github.tomakehurst" % "wiremock-jre8"       % "2.27.2"            % scope,
          "com.typesafe.play"      %% "play-test"          % PlayVersion.current % scope,
          "org.scalacheck"         %% "scalacheck"         % "1.14.3"            % scope,
          "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"             % bootstrapVersion,
          "org.scalatestplus" %% "scalacheck-1-15" % "3.2.3.0"
        )
      }.test
  }

  def apply() = compile ++ Test()
}
