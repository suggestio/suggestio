Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "lk-adv-ext-sjs"

version := "0.0.0"

libraryDependencies ++= Seq(
  "be.doeraene"   %%% "scalajs-jquery"  % Common.sjsJqueryVsn,
  Common.ORG      %%% "common-sjs"      % "0.0.0",
  //"com.lihaoyi"   %%% "upickle"       % "0.2.+",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

// TODO требуется DOM в тестах.

useYarn := true
