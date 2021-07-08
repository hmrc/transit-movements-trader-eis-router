import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

val appName = "transit-movements-trader-eis-router"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .configs(IntegrationTest)
  .settings(DefaultBuildSettings.integrationTestSettings())
  .settings(SbtDistributablesPlugin.publishingSettings)
  .settings(inThisBuild(buildSettings))
  .settings(scalacSettings)
  .settings(scoverageSettings)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.13",
    resolvers += Resolver.jcenterRepo,
    PlayKeys.playDefaultPort := 9499,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
  )

// Settings for the whole build
lazy val buildSettings = Def.settings(
  useSuperShell := false
)

// Scalac options
lazy val scalacSettings = Def.settings(
  // Disable dead code warning as it is triggered by Mockito any()
  Test / scalacOptions ~= {
    opts =>
      opts.filterNot(Set("-Ywarn-dead-code"))
  },
  // Disable warnings arising from generated routing code
  scalacOptions += "-Wconf:src=routes/.*:silent"
)

// Scoverage exclusions and minimums
lazy val scoverageSettings = Def.settings(
  parallelExecution in Test := false,
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageExcludedFiles := "<empty>;.*javascript.*;.*Routes.*;",
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true,
  ScoverageKeys.coverageExcludedPackages := Seq(
    """uk\.gov\.hmrc\.BuildInfo*""",
    """.*\.Routes""",
    """.*\.RoutesPrefix""",
    """.*\.Reverse[^.]*""",
    "testonly",
    "testOnly.*",
    "config.*"
  ).mkString(";")
)
