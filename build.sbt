Common.settingsOrgJS

name := "sc-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-mapboxgl"    % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "ble-beaconer-sjs"    % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-cordova"     % "0.0.0-SNAPSHOT",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % "test"
)

persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

jsDependencies += RuntimeDOM % "test"

// Без зависимостей. Всё минималистичненько.
skip in packageJSDependencies := true

// TODO Надо бы выставлять это на ci, но НЕ выставлять на продакшенах.
//scalacOptions in Compile ++= Seq("-Xelide-below", "WARNING")
