Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-resumablejs"

version := "0.0.0"

testFrameworks += new TestFramework("minitest.runner.Framework")

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % Common.sjsDomVsn,
  "io.monix"     %%% "minitest"    % Common.minitestVsn  % Test
)

npmDependencies in Compile ++= Seq(
  "resumablejs"   -> Common.Vsn.RESUMABLE_JS
)

useYarn := true