import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val catsVersion = "2.6.1"

  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-27" % "5.12.0",
    "org.typelevel" %% "cats-core"                 % catsVersion
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"           % "3.2.9",
    "com.typesafe.play"      %% "play-test"           % current,
    "com.vladsch.flexmark"    % "flexmark-all"        % "0.36.8",
    "org.scalatestplus.play" %% "scalatestplus-play"  % "4.0.3",
    "com.github.tomakehurst"  % "wiremock-standalone" % "2.27.2",
    "org.scalacheck"         %% "scalacheck"          % "1.15.4",
    "org.mockito"             % "mockito-core"        % "3.3.3",
    "org.scalatestplus"      %% "mockito-3-2"         % "3.1.2.0",
    "org.scalatestplus"      %% "scalacheck-1-14"     % "3.2.2.0",
    "org.typelevel"          %% "cats-core"           % catsVersion
  ).map(_ % s"$Test, $IntegrationTest")
}
