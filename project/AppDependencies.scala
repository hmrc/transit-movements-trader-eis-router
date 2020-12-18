import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"  % "3.2.0"
  )

  val test = Seq(
    "org.scalatest"           %% "scalatest"                % "3.2.0"                 % Test,
    "com.typesafe.play"       %% "play-test"                % current                 % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.35.10"               % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"                 % "test, it",
    "com.github.tomakehurst"  % "wiremock-standalone"       % "2.27.1"                % "test, it",
    "org.scalacheck"          %% "scalacheck"               % "1.14.3"                % "test, it",
    "org.mockito"             % "mockito-core"              % "3.3.3"                 % "test, it",
    "org.scalatestplus"       %% "mockito-3-2"              % "3.1.2.0"               % "test, it",
    "org.scalatestplus"       %% "scalacheck-1-14"          % "3.2.2.0"               % "test, it"
  )
}