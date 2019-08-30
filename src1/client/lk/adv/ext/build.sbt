Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "lk-adv-ext-sjs"

version := "0.0.0"

libraryDependencies ++= Seq(
  "be.doeraene"   %%% "scalajs-jquery"  % Common.sjsJqueryVsn,
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

// TODO требуется DOM в тестах.

useYarn := true
