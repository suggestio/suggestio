Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "lk-adv-ext-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "be.doeraene"   %%% "scalajs-jquery"  % Common.sjsJqueryVsn,
  Common.ORG      %%% "common-sjs"      % "0.0.0-SNAPSHOT",
  //"com.lihaoyi"   %%% "upickle"       % "0.2.+",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % "test"
)

testFrameworks += new TestFramework("minitest.runner.Framework")

// TODO требуется DOM в тестах.

