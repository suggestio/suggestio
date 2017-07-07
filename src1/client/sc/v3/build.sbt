Common.settingsOrgJS

name := "sc3-sjs"

version := "0.0.0-SNAPSHOT"

enablePlugins(ScalaJSBundlerPlugin)

libraryDependencies ++= Seq(
  // С самого начала был нужен js-роутер.
  "com.github.japgolly.scalajs-react" %%% "extra" % Common.reactSjsVsn,

  // И сразу же завязываем генерацию css на scalacss, чтобы style="" костыли не городить.
  "com.github.japgolly.scalacss" %%% "ext-react"  % Common.Vsn.SCALACSS,

  // testing
  "io.monix"      %%% "minitest"                  % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

requiresDOM in Test := true

// TODO Надо бы выставлять это на ci, но НЕ выставлять на продакшенах.
//scalacOptions in Compile ++= Seq("-Xelide-below", "WARNING")

// https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
enableReloadWorkflow := true

emitSourceMaps := false

scalaJSUseMainModuleInitializer := true

