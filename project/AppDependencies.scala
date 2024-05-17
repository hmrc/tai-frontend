import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

private object AppDependencies {
  private val playVersion = "play-29"
  private val hmrcMongoVersion = "1.8.0"
  private val bootstrapVersion = "8.5.0"
  private val webChatVersion = "1.4.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
//    jodaForms,
    "org.typelevel" %% "cats-core"                     % "2.10.0",
    "uk.gov.hmrc" %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
//    "uk.gov.hmrc"   %% "digital-engagement-platform-chat-29"           % s"1.4.0-$playVersion",
    //    "uk.gov.hmrc"       %% "digital-engagement-platform-chat" % s"0.32.0-$playVersion",
//    "uk.gov.hmrc" %% "digital-engagement-platform-chat-29"        % webChatVersion,
    "uk.gov.hmrc" %% "digital-engagement-platform-chat-29" % "1.1.0",
    "uk.gov.hmrc" %% s"mongo-feature-toggles-client-$playVersion" % "1.3.0",
    "uk.gov.hmrc" %% s"sca-wrapper-$playVersion"                  % "1.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"              % "jsoup"                         % "1.17.2",
    "org.mockito"           %% "mockito-scala-scalatest"       % "1.17.31",
//    "com.github.tomakehurst" % "wiremock-jre8"                 % "2.35.1",
    "com.typesafe.play"     %% "play-test"                     % PlayVersion.current,
    "uk.gov.hmrc"           %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.scalatestplus"     %% "scalacheck-1-17"               % "3.2.18.0",
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
  //  "com.vladsch.flexmark"   % "flexmark-all"                  % "0.64.8"
  ).map(_ % "test")

  /*
  "org.scalatest" %% "scalatest" % "3.2.18",
"org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1",
"org.pegdown" % "pegdown" % "1.6.0",
"org.playframework" %% "play-test" % PlayVersion.current,
"com.github.tomakehurst" % "wiremock-standalone" % "3.0.1",
"com.vladsch.flexmark" % "flexmark-all" % "0.64.8",
   */
  
  val jacksonVersion = "2.13.2"
  val jacksonDatabindVersion = "2.13.2.2"

  val jacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion)

  val jacksonDatabindOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion
  )

  val akkaSerializationJacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module"    %% "jackson-module-scala"
  ).map(_ % jacksonVersion)

  val all: Seq[ModuleID] =
    compile ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides ++ test
}
/*
  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"http-caching-client-$playVersion"          % "11.2.0",
    "uk.gov.hmrc"       %% "tax-year"                                   % "4.0.0",
    "io.lemonlabs"      %% "scala-uri"                                  % "4.0.3",
    "org.jsoup"          % "jsoup"                                      % "1.17.2",
    "org.typelevel"     %% "cats-core"                                  % "2.10.0",
    "org.apache.commons" % "commons-text"                               % "1.11.0",
    "uk.gov.hmrc"       %% s"sca-wrapper-$playVersion"                  % "1.6.0",
    "uk.gov.hmrc"       %% s"mongo-feature-toggles-client-$playVersion" % "1.3.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "org.mockito"         %% "mockito-scala-scalatest"       % "1.17.31",
    "org.scalatestplus"   %% "scalacheck-1-17"               % "3.2.18.0",
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
  ).map(_ % "test")
 */

/*
import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

private object AppDependencies {

  private val playVersion = "play-28"
  private val bootstrapVersion = "8.1.0"

  val compile: Seq[ModuleID] = Seq(
    filters,
    jodaForms,
    "org.typelevel"     %% "cats-core"                        % "2.10.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"    % s"1.13.0-$playVersion",
    "uk.gov.hmrc"       %% "digital-engagement-platform-chat" % s"0.32.0-$playVersion",
    "uk.gov.hmrc"       %% "mongo-feature-toggles-client"     % "0.3.0",
    "uk.gov.hmrc"       %% s"sca-wrapper-$playVersion"        % "1.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.jsoup"              % "jsoup"                          % "1.16.1",
    "org.mockito"            %% "mockito-scala-scalatest"       % "1.17.27",
    "com.github.tomakehurst" % "wiremock-jre8"                  % "2.35.1",
    "com.typesafe.play"      %% "play-test"                     % PlayVersion.current,
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,

    "org.scalatestplus"      %% "scalacheck-1-17"               % "3.2.17.0",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % "1.3.0",

    "com.vladsch.flexmark"   %  "flexmark-all"                  % "0.64.8"
  ).map(_ % "test,it")

  val jacksonVersion         = "2.13.2"
  val jacksonDatabindVersion = "2.13.2.2"

  val jacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core"     % "jackson-core",
    "com.fasterxml.jackson.core"     % "jackson-annotations",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"
  ).map(_ % jacksonVersion)

  val jacksonDatabindOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion
  )

  val akkaSerializationJacksonOverrides: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor",
    "com.fasterxml.jackson.module"     % "jackson-module-parameter-names",
    "com.fasterxml.jackson.module"     %% "jackson-module-scala",
  ).map(_ % jacksonVersion)


  val all: Seq[ModuleID] = compile ++ jacksonDatabindOverrides ++ jacksonOverrides ++ akkaSerializationJacksonOverrides ++ test
}
 */