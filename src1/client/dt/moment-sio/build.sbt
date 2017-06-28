Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "moment-sio-sjs"

version := "0.0.0-SNAPSHOT"

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"      % "0.0.0-SNAPSHOT",
  Common.ORG      %%% "scalajs-moment"  % Common.Vsn.momentJs,
  "io.monix"      %%% "minitest"        % Common.minitestVsn  % Test
)

//requiresDOM in Test := true

