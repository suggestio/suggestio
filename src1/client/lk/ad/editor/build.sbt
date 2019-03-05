Common.settingsOrgJS

enablePlugins(ScalaJSBundlerPlugin)

name := "lk-ad-editor-sjs"

version := "0.0.0-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG                     %%% "lk-common-sjs"       % "0.0.0",
  "com.softwaremill.macwire"     %%  "macros"              % Common.Vsn.MACWIRE % "provided",
  "org.scala-js"                 %%% "scalajs-dom"         % Common.sjsDomVsn
)

useYarn := true
