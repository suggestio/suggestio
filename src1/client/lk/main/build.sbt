import org.scalajs.jsenv.nodejs.NodeJSEnv

Common.settingsOrgJS

name := "lk-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)


//testFrameworks += new TestFramework("minitest.runner.Framework")

// Есть плавающая проблема с LibraryOnly и Leaflet.js (global.L):
// https://github.com/scalacenter/scalajs-bundler/issues/178
webpackBundlingMode := BundlingMode.LibraryOnly()

//webpackBundlingMode in fullOptJS := BundlingMode.Application

// Use a different Webpack configuration file for production
//[info] ERROR in lk-sjs-opt-library.js from UglifyJs
//[info] Invalid assignment [./node_modules/source-map-loader!./node_modules/quill-delta/lib/delta.js:119,0][lk-sjs-opt-library.js:80073,36]
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js")

scalaJSLinkerConfig in ThisBuild ~= { _.withESFeatures(_
  .withESVersion( Common.Vsn.ECMA_SCRIPT )
)}

scalaJSLinkerConfig in fullOptJS ~= { _.withSourceMap(false) }

scalaJSUseMainModuleInitializer := true

useYarn := true

// Попытка гашения node.js OOM на стадии "Bundling NPM deps..."
jsEnv := new NodeJSEnv(
  NodeJSEnv.Config()
    .withArgs( List("--max-old-space-size=8192") )
)

