Common.settingsOrg

name := "ble-beaconer-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-cordova"     % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-cordova-ble" % "0.0.0-SNAPSHOT",
  "org.monifu"    %%% "minitest"            % "0.12"            % "test"
)

persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

scalaJSUseRhino in Global := false

