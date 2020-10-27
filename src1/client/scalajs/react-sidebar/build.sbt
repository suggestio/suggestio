Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-sidebar"

version := Common.Vsn.REACT_SIDEBAR

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

npmDependencies in Compile ++= Seq(
  "react-sidebar" -> version.value
)

useYarn := true

requireJsDomEnv in Test := true
