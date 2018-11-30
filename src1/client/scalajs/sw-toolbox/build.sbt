Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-sw-toolbox"

version := Common.Vsn.SW_TOOLBOX

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scala-js"  %%% "scalajs-dom" % Common.sjsDomVsn,
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

testFrameworks += new TestFramework("minitest.runner.Framework")

npmDependencies in Compile ++= Seq(
  "sw-toolbox" -> version.value
)

