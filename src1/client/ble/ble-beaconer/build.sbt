Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "ble-beaconer-sjs"

version := "0.0.0"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

useYarn := true
