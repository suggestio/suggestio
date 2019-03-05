Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "lk-ads-sjs"

version := "0.0.0"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  "com.softwaremill.macwire"     %%  "macros"              % Common.Vsn.MACWIRE % "provided",
  "org.scala-js"                 %%% "scalajs-dom"         % Common.sjsDomVsn
)

useYarn := true
