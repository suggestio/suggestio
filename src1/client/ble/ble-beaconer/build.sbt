Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "ble-beaconer-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-cordova"     % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-cordova-ble" % "0.0.0-SNAPSHOT",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % "test"
)

testFrameworks += new TestFramework("minitest.runner.Framework")

