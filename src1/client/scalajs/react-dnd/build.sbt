Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-dnd"

version := Common.Vsn.REACT_DND

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test,
  "com.github.japgolly.scalajs-react" %%% "test" % Common.reactSjsVsn % Test,
)

npmDependencies in Compile ++= Seq(
  "react-dnd"                     -> version.value,
  "react-dnd-html5-backend"       -> version.value,
  "react-dnd-touch-backend"       -> version.value,
)

npmDependencies in Test ++= Seq(
  "react-dnd-test-backend"        -> version.value,
)

useYarn := true

requireJsDomEnv in Test := true

