Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-resizable"

version := Common.Vsn.REACT_RESIZABLE

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test,
  "com.github.japgolly.scalajs-react" %%% "test" % Common.reactSjsVsn % Test,
)

npmDependencies in Compile ++= Seq(
  "react-resizable" -> version.value
)

useYarn := true

requireJsDomEnv in Test := true
