Common.settingsOrgJS

name := "lk-adv-ext-sjs"

version := "0.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "be.doeraene"   %%% "scalajs-jquery"  % Common.sjsJqueryVsn,
  Common.ORG      %%% "common-sjs"      % "0.0.0-SNAPSHOT",
  //"com.lihaoyi"   %%% "upickle"       % "0.2.+",
  "io.monix"      %%% "minitest"            % Common.minitestVsn  % "test"
)

//persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("minitest.runner.Framework")

// требуется DOM в тестах. http://www.scala-js.org/doc/tutorial.html
//jsDependencies += RuntimeDOM
requiresDOM in Test := true

