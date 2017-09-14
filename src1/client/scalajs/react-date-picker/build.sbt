Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-date-picker"

version := "0.39.0"

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-react-common"  % "0.0.0-SNAPSHOT",
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test
  // moment
)

npmDependencies in Compile ++= Seq(
  "react-datepicker" -> version.value
)

