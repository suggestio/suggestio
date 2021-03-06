Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-mui-treasury"

version := Common.Vsn.MUI_TREASURY

testFrameworks += new TestFramework("minitest.runner.Framework")

scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

npmDependencies in Compile += (
  "@mui-treasury/styles" -> version.value
)

useYarn := true
