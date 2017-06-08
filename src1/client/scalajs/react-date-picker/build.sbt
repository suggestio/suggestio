Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-date-picker"

version := "0.39.0-SNAPSHOT"

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-react-common"  % "0.0.0-SNAPSHOT",
  "io.monix"      %%% "minitest" % Common.minitestVsn % "test"
  // moment
)

npmDependencies in Compile ++= Seq(
  //"react-onclickoutside" -> "5.7.1",
  "react" -> Common.reactJsVsn,
  //"moment" -> Common.Vsn.momentJs,
  "react-datepicker" -> "0.39.0"
)

//requiresDOM in Test := true

