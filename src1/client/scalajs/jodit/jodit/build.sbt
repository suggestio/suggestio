Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-jodit"

version := Common.Vsn.JODIT

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scala-js"  %%% "scalajs-dom" % Common.sjsDomVsn,
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test,
)

npmDependencies in Compile ++= Seq(
  "jodit"  -> version.value,
)

useYarn := true
