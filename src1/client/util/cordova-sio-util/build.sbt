Common.settingsOrgJS

name := "cordova-sio-util-sjs"

enablePlugins(ScalaJSBundlerPlugin)

version := "0.0.0"

libraryDependencies ++= Seq(
  "io.monix"            %%% "minitest"            % Common.minitestVsn  % Test,
)

testFrameworks += new TestFramework("minitest.runner.Framework")

useYarn := true
