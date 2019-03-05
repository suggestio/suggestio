Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-quill-delta"

version := "0.0.0"

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "io.monix"     %%% "minitest"    % Common.minitestVsn  % Test
)

npmDependencies in Compile ++= Seq(
  "quill-delta" -> Common.Vsn.QUILL_DELTA
)

//requiresDOM in Test := true

useYarn := true
