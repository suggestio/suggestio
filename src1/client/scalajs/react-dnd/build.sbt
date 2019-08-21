Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-react-dnd"

version := Common.Vsn.REACT_DND

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"      %%% "minitest" % Common.minitestVsn % Test
)

npmDependencies in Compile ++= Seq(
  "react-dnd-cjs"                 -> version.value,
  "react-dnd-html5-backend-cjs"   -> version.value,
  "react-dnd-touch-backend-cjs"   -> version.value,
)

npmDependencies in Test ++= Seq(
  "react-dnd-test-backend-cjs"    -> version.value,
)

useYarn := true
