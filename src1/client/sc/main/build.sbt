Common.settingsOrgJS

name := "sc-sjs"

version := "0.0.0-SNAPSHOT"

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-mapboxgl"    % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "ble-beaconer-sjs"    % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-cordova"     % "0.0.0-SNAPSHOT",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % "test"
)

persistLauncher in Compile := false

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

requiresDOM in Test := true

// Без зависимостей. Всё минималистичненько.
skip in packageJSDependencies := true

// TODO Надо бы выставлять это на ci, но НЕ выставлять на продакшенах.
//scalacOptions in Compile ++= Seq("-Xelide-below", "WARNING")

enableReloadWorkflow := true

emitSourceMaps := false

