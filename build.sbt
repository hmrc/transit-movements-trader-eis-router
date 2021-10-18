import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "transit-movements-trader-eis-router"

lazy val microservice = Project(appName, file("."))
  .configs(IntegrationTest)
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(
    JUnitXmlReportPlugin
  ) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(DefaultBuildSettings.integrationTestSettings())
  .settings(SbtDistributablesPlugin.publishingSettings)
  .settings(inConfig(IntegrationTest)(ScalafmtPlugin.scalafmtConfigSettings))
  .settings(scalacSettings)
  .settings(scoverageSettings)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.14",
    resolvers += Resolver.jcenterRepo,
    PlayKeys.playDefaultPort := 9499,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )

lazy val scalacSettings = Def.settings(
  // Disable warnings arising from generated routing code
  scalacOptions += "-Wconf:src=routes/.*:silent",
  // Disable fatal warnings and warnings from discarding values
  scalacOptions ~= { opts =>
    opts.filterNot(Set("-Xfatal-warnings", "-Ywarn-value-discard"))
  },
  // Disable dead code warning as it is triggered by Mockito any()
  Test / scalacOptions ~= { opts =>
    opts.filterNot(Set("-Ywarn-dead-code"))
  }
)

lazy val scoverageSettings = Def.settings(
  Test / parallelExecution := false,
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true,
  ScoverageKeys.coverageExcludedPackages := Seq(
    "<empty>",
    "Reverse.*",
    ".*(config|views.*)",
    ".*(BuildInfo|Routes).*"
  ).mkString(";"),
  ScoverageKeys.coverageExcludedFiles := Seq(
    "<empty>",
    "Reverse.*",
    ".*BuildInfo.*",
    ".*javascript.*",
    ".*Routes.*",
    ".*GuiceInjector"
  ).mkString(";")
)
