//import org.scalajs.core.tools.javascript.OutputMode

Common.settingsOrgJS

name := "lk-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)


//testFrameworks += new TestFramework("minitest.runner.Framework")

// Есть плавающая проблема с LibraryOnly и Leaflet.js (global.L):
// https://github.com/scalacenter/scalajs-bundler/issues/178
webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly()

//webpackBundlingMode in fullOptJS := BundlingMode.LibraryOnly()
webpackBundlingMode in fullOptJS := BundlingMode.Application

// Use a different Webpack configuration file for production
//[info] ERROR in lk-sjs-opt-library.js from UglifyJs
//[info] Invalid assignment [./node_modules/source-map-loader!./node_modules/quill-delta/lib/delta.js:119,0][lk-sjs-opt-library.js:80073,36]
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js")

emitSourceMaps := true

(emitSourceMaps in fullOptJS) := true

npmDependencies in Compile ++= Seq(
  "react"     -> Common.reactJsVsn,
  "react-dom" -> Common.reactJsVsn
)

scalaJSUseMainModuleInitializer := true

useYarn := true

// А иначе упадёт на стадии bundling NPM deps.
jsEnv := NodeJSEnv(args = Seq("--max_old_space_size=4096")).value

// ECMA2015: Надо разобраться с window.L и плагинами, зависящими от global.L
//scalaJSLinkerConfig ~= { _.withESFeatures(_.withUseECMAScript2015(true)) }
