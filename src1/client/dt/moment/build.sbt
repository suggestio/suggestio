Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-moment"

version := Common.Vsn.momentJs

persistLauncher in Compile := false

persistLauncher in Test := false

// Show more comments when using dubious features
scalacOptions += "-feature"

npmDependencies in Compile ++= Seq(
  "moment" -> Common.Vsn.momentJs
)

libraryDependencies ++= Seq(
  "io.monix" %%% "minitest" % Common.minitestVsn  % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

