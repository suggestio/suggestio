Common.settingsOrgJS

name := "sc3-sjs"

version := "0.0.0-SNAPSHOT"

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

webpackBundlingMode := BundlingMode.LibraryOnly()

// Use a different Webpack configuration file for production
webpackConfigFile in fullOptJS := Some(baseDirectory.value / "webpack.prod.config.js")

emitSourceMaps := false

scalaJSUseMainModuleInitializer := true

