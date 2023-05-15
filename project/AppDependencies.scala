import sbt._

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val playVersion = "play-28"
  private val bootstrapVersion = "7.15.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    jodaForms,
    "org.typelevel"     %% "cats-core"                        % "2.9.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"    % s"1.13.0-$playVersion",
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion" % bootstrapVersion,
    "uk.gov.hmrc"       %% "domain"                           % s"8.3.0-$playVersion",
    "uk.gov.hmrc"       %% "digital-engagement-platform-chat" % s"0.32.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-hmrc"               % s"7.7.0-$playVersion",
    "uk.gov.hmrc"       %% "play-frontend-pta"                % "0.5.0"
  )

  val test: Seq[ModuleID] = Seq(
          "org.scalatestplus.play" %% "scalatestplus-play"            % "5.1.0",
          "org.pegdown"            % "pegdown"                        % "1.6.0",
          "org.jsoup"              % "jsoup"                          % "1.16.1",
          "org.mockito"            %% "mockito-scala-scalatest"       % "1.17.14",
          "com.github.tomakehurst" % "wiremock-jre8"                  % "2.35.0",
          "com.typesafe.play"      %% "play-test"                     % PlayVersion.current,
          "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
          "org.scalatestplus"      %% "scalacheck-1-17"               % "3.2.15.0"
  )

  val jacksonVersion         = "2.13.2"
  val jacksonDatabindVersion = "2.13.2.2"

  val jacksonOverrides = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion)

  val jacksonDatabindOverrides = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion
  )

  val akkaSerializationJacksonOverrides = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module"     %% "jackson-module-scala",
  ).map(_ % jacksonVersion)


  val all: Seq[ModuleID] = compile ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides ++ test
}
