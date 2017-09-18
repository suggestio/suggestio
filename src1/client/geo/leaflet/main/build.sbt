Common.settingsOrgJS

// Turn this project into a Scala.js project by importing these settings
//enablePlugins(ScalaJSPlugin)

enablePlugins(ScalaJSBundlerPlugin)

name := "scalajs-leaflet"

version := "0.1s-SNAPSHOT"

// Show more comments when using dubious features
scalacOptions += "-feature"

libraryDependencies ++= Seq(
  Common.ORG      %%% "common-sjs"       % "0.0.0-SNAPSHOT",
  "org.scala-js"  %%% "scalajs-dom"      % Common.sjsDomVsn
)

npmDependencies in Compile ++= Seq(
  "leaflet"               -> Common.leafletJsVsn,
  "leaflet.locatecontrol" -> Common.leafletControlLocateJsVsn
)

