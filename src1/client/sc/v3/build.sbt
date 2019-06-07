Common.settingsOrgJS

name := "sc3-sjs"

version := "0.0.0"

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  // С самого начала был нужен js-роутер.
  "com.github.japgolly.scalajs-react" %%% "extra" % Common.reactSjsVsn,

  // И сразу же завязываем генерацию css на scalacss, чтобы style="" костыли не городить.
  "com.github.japgolly.scalacss" %%% "ext-react"  % Common.Vsn.SCALACSS,

  // Compile-time DI без рантаймового кода (кроме нагенеренной примитивщины):
  "com.softwaremill.macwire"     %% "macros"     % Common.Vsn.MACWIRE % "provided",
  //"com.softwaremill.macwire"     %% "util"       % Common.Vsn.MACWIRE,

  // testing
  "io.monix"      %%% "minitest"                  % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

// TODO Надо бы выставлять это на ci, но НЕ выставлять на продакшенах.
//scalacOptions in Compile ++= Seq("-Xelide-below", "WARNING")

// https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
//webpackBundlingMode := BundlingMode.LibraryOnly()
// https://github.com/scalacenter/scalajs-bundler/issues/178
webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly()
//
// TODO Проблемы с L() и window.L. Нужно как-то организовать воспроизводимый bug-report и отправить в scala.js.
webpackBundlingMode in fullOptJS := BundlingMode.Application


// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js")

emitSourceMaps := false

scalaJSUseMainModuleInitializer := true

useYarn := true

// ECMA2015: Надо разобраться с window.L и плагинами, зависящими от global.L
//scalaJSLinkerConfig ~= { _.withESFeatures(_.withUseECMAScript2015(true)) }

