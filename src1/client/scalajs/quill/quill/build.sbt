Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-quill"

version := "0.0.0"

//testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % Common.sjsDomVsn
)

npmDependencies in Compile ++= Seq(
  "quill"   -> Common.Vsn.QUILL
)

