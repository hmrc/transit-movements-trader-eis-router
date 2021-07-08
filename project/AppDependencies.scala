import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val catsVersion = "2.6.1"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-27" % "5.7.0",
    "org.typelevel" %% "cats-core"                 % catsVersion
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"           % "3.2.9"   % "test, it",
    "com.typesafe.play"      %% "play-test"           % current   % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"        % "0.36.8"  % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"  % "4.0.3"   % "test, it",
    "com.github.tomakehurst"  % "wiremock-standalone" % "2.27.2"  % "test, it",
    "org.scalacheck"         %% "scalacheck"          % "1.15.4"  % "test, it",
    "org.mockito"             % "mockito-core"        % "3.4.6"   % "test, it",
    "org.scalatestplus"      %% "mockito-3-4"         % "3.2.9.0" % "test, it",
    "org.scalatestplus"      %% "scalacheck-1-15"     % "3.2.9.0" % "test, it"
  )
}
