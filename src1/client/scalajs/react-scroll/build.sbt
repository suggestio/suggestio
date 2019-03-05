Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-scroll"

version := Common.Vsn.REACT_SCROLL

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "scalajs-react-common"  % "0.0.0",
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

npmDependencies in Compile ++= Seq(
  "react-scroll" -> version.value
)

useYarn := true
