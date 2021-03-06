import org.scalajs.jsenv.nodejs.NodeJSEnv

Common.settingsOrgJS

name := "sc3-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "extra" % Common.reactSjsVsn,
  "com.softwaremill.macwire"     %% "macros"     % Common.Vsn.MACWIRE % "provided",
  "io.monix"      %%% "minitest"                  % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

// TODO Надо бы выставлять это на ci, но НЕ выставлять на продакшенах.
//scalacOptions in Compile ++= Seq("-Xelide-below", "WARNING")

// https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
webpackBundlingMode := BundlingMode.LibraryOnly()

// https://github.com/scalacenter/scalajs-bundler/issues/178
// TODO Проблемы с L() и window.L. Нужно как-то организовать воспроизводимый bug-report и отправить в scala.js.
//webpackBundlingMode in fullOptJS := BundlingMode.Application


// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js")

// TODO scalajs-1.0: leaflet-markercluster и locatecontrol мешают переезду на ES2015 (true).
// ECMA2015: Надо разобраться с window.L и плагинами, зависящими от global.L
scalaJSLinkerConfig in ThisBuild ~= { _.withESFeatures(_
  .withESVersion( Common.Vsn.ECMA_SCRIPT )
  // avoid ES class'es = true, иначе тормозит iOS Safari (cordova app) и Firefox (по каким-то benchmark'ам) из-за ES2015 classes
  .withAvoidClasses(true)
  // avoid let & const = true, иначе тормозит iOS Safari (cordova app) https://github.com/scala-js/scala-js/pull/4277 https://bugs.webkit.org/show_bug.cgi?id=199866
  .withAvoidLetsAndConsts(true)
)}

// Выключение оптимизации для дебага нетривиальных ошибок, видимых только на продакшене
//scalaJSLinkerConfig ~= { _.withOptimizer(false) }

scalaJSLinkerConfig in fullOptJS ~= { _.withSourceMap(false) }


scalaJSUseMainModuleInitializer := true

useYarn := true


// Ускорить node.js на продакшене.
jsEnv := new NodeJSEnv(
  NodeJSEnv.Config()
    .withArgs( "--max-old-space-size=4096" :: Nil )
)


