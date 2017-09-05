Common.settingsOrgJS

name := "sc-sjs"

version := "0.0.0-SNAPSHOT"

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"          % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-mapboxgl"    % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "ble-beaconer-sjs"    % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-cordova"     % "0.0.0-SNAPSHOT",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Без зависимостей. Всё минималистичненько.
skip in packageJSDependencies := true

// TODO Надо бы выставлять это на ci, но НЕ выставлять на продакшенах.
//scalacOptions in Compile ++= Seq("-Xelide-below", "WARNING")

// https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
enableReloadWorkflow := true

emitSourceMaps := false

scalaJSUseMainModuleInitializer := true

