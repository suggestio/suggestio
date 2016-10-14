Common.settingsOrg

name := "sc-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-mapboxgl"    % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "ble-beaconer-sjs"    % "0.0.0-SNAPSHOT",
  "org.monifu"    %%% "minitest"            % "0.12"            % "test"
)

persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

// Без зависимостей. Всё минималистичненько.
skip in packageJSDependencies := true

