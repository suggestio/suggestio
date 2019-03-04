Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "ble-beaconer-sjs"

version := "0.0.0"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"          % "0.0.0",
  Common.ORG      %%% "scalajs-cordova"     % "0.0.0",
  Common.ORG      %%% "scalajs-cordova-ble" % "0.0.0",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

