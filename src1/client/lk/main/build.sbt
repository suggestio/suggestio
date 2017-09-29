//import org.scalajs.core.tools.javascript.OutputMode

Common.settingsOrgJS

name := "lk-sjs"

version := "0.0.0-SNAPSHOT"

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "be.doeraene"         %%% "scalajs-jquery"      % Common.sjsJqueryVsn,
  Common.ORG            %%% "lk-adv-ext-sjs"      % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adv-direct-sjs"   % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adv-geo-tags-sjs" % "0.0.0-SNAPSHOT",
  Common.ORG            %%% "lk-adn-map-sjs"      % "0.0.0-SNAPSHOT"
  //"io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Пытаемся разрулить проблему с Leaflet, потому что MarkerCluster не может без глобальной L,
// а без L.noConflict() конфликт с выхлопом webpack, который тоже что-то записывает в L.
//webpackConfigFile := Some(baseDirectory.value / "lk.webpack.config.js")

// https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
// https://github.com/scalacenter/scalajs-bundler/issues/178
webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly()

webpackBundlingMode in fullOptJS := BundlingMode.LibraryOnly()  // BundlingMode.Application


emitSourceMaps := true

(emitSourceMaps in fullOptJS) := true

npmDependencies in Compile ++= Seq(
  "react"     -> Common.reactJsVsn,
  "react-dom" -> Common.reactJsVsn
)

scalaJSUseMainModuleInitializer := true

